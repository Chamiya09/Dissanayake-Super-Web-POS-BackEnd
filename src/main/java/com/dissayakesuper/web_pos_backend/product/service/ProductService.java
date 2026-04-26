package com.dissayakesuper.web_pos_backend.product.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.dissayakesuper.web_pos_backend.inventory.repository.InventoryRepository;
import com.dissayakesuper.web_pos_backend.product.dto.ProductBulkImportResponse;
import com.dissayakesuper.web_pos_backend.product.dto.ProductImportError;
import com.dissayakesuper.web_pos_backend.product.dto.ProductImportSuccess;
import com.dissayakesuper.web_pos_backend.product.dto.ProductPageResponse;
import com.dissayakesuper.web_pos_backend.product.dto.ProductRequest;
import com.dissayakesuper.web_pos_backend.product.entity.Product;
import com.dissayakesuper.web_pos_backend.product.repository.ProductRepository;

@Service
@Transactional
public class ProductService {

    private static final double STOCK_EPSILON = 1e-6;
    private static final String SKU_PREFIX = "PI";

    private final ProductRepository repository;
    private final InventoryRepository inventoryRepository;

    public ProductService(
            ProductRepository repository,
            InventoryRepository inventoryRepository) {
        this.repository = repository;
        this.inventoryRepository = inventoryRepository;
    }

    // ── READ ALL ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        return repository.findByIsActiveTrue();
    }

    @Transactional(readOnly = true)
    public ProductPageResponse getProductsPage(int page, int limit, String search) {
        int safePage = Math.max(page, 0);
        int safeLimit = Math.max(1, Math.min(limit, 200));
        String searchTerm = normalizeOptional(search);

        Pageable pageable = PageRequest.of(safePage, safeLimit, Sort.by(Sort.Direction.ASC, "id"));
        Page<Product> result;

        try {
            result = (searchTerm == null)
                    ? repository.findByIsActiveTrue(pageable)
                    : repository.searchActiveProducts(searchTerm, pageable);
        } catch (RuntimeException ex) {
            // Fallback prevents UI outages if database dialect/runtime rejects the JPQL.
            return getProductsPageFallback(safePage, safeLimit, searchTerm);
        }

        return new ProductPageResponse(
                result.getContent(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                result.getSize(),
                result.hasNext(),
                result.hasPrevious()
        );
    }

    private ProductPageResponse getProductsPageFallback(int page, int limit, String searchTerm) {
        List<Product> allActive = repository.findByIsActiveTrue();

        List<Product> filtered = allActive.stream()
                .filter(product -> matchesSearch(product, searchTerm))
                .sorted(Comparator.comparing(Product::getId))
                .collect(Collectors.toList());

        int totalElements = filtered.size();
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / limit);

        int safeStart = Math.min(page * limit, totalElements);
        int safeEnd = Math.min(safeStart + limit, totalElements);
        List<Product> content = filtered.subList(safeStart, safeEnd);

        return new ProductPageResponse(
                content,
                totalElements,
                totalPages,
                page,
                limit,
                page + 1 < totalPages,
                page > 0
        );
    }

    private boolean matchesSearch(Product product, String searchTerm) {
        if (searchTerm == null) return true;

        String needle = searchTerm.toLowerCase(Locale.ROOT);
        String name = product.getProductName() == null ? "" : product.getProductName().toLowerCase(Locale.ROOT);
        String sku = product.getSku() == null ? "" : product.getSku().toLowerCase(Locale.ROOT);
        String category = product.getCategory() == null ? "" : product.getCategory().toLowerCase(Locale.ROOT);

        return name.contains(needle) || sku.contains(needle) || category.contains(needle);
    }

    // ── AVAILABLE FOR INVENTORY ────────────────────────────────────────────────

    /** Returns only products that have no Inventory record yet. */
    @Transactional(readOnly = true)
    public List<Product> getProductsAvailableForInventory() {
        return repository.findProductsNotInInventory();
    }

    // ── READ ONE ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Product getProductById(Long id) {
        return repository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product not found with id: " + id));
    }

    // ── CREATE ────────────────────────────────────────────────────────────────

    public Product createProduct(ProductRequest request) {
        String sku = normalizeOptional(request.sku());
        String barcode = normalizeOptional(request.barcode());

        if (sku == null) {
            sku = generateSku();
        }

        if (sku != null && repository.existsBySkuAndIsActiveTrue(sku)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A product with SKU '" + sku + "' already exists.");
        }

        if (barcode != null && repository.existsByBarcodeAndIsActiveTrue(barcode)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A product with barcode '" + barcode + "' already exists.");
        }

        Product product = new Product(
            request.productName(),
            sku,
            barcode,
            request.category(),
            request.buyingPrice(),
            request.sellingPrice(),
            request.unit(),
            request.stockQuantity(),
            request.reorderLevel()
        );

        if (product.getSku() == null || product.getSku().isBlank()) {
            product.setSku(generateSku());
        }

        try {
            return repository.save(product);
        } catch (DataIntegrityViolationException ex) {
            Throwable cause = ex.getCause();
            String rootMessage = safeMessage(cause == null
                    ? ex.getMessage()
                    : cause.getMessage());

            if (rootMessage.contains("column \"sku\"") && rootMessage.contains("not-null")) {
                product.setSku(generateSku());
                return repository.save(product);
            }

            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Product could not be created because SKU or barcode already exists.");
        }
    }

    // ── BULK IMPORT ──────────────────────────────────────────────────────────

    /**
     * Imports multiple products (usually from a CSV file parsed in the frontend).
     * Each row is validated and imported independently so one bad row does not
     * block other valid rows.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ProductBulkImportResponse importProducts(List<ProductRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CSV import list is empty.");
        }

        List<ProductImportSuccess> importedProducts = new ArrayList<>();
        List<ProductImportError> errors = new ArrayList<>();
        Set<String> seenSkus = new HashSet<>();

        for (int i = 0; i < requests.size(); i++) {
            int rowNumber = i + 2; // row 1 is CSV header
            ProductRequest request = requests.get(i);
            String skuForError = null;

            try {
                if (request == null) {
                    errors.add(new ProductImportError(rowNumber, null, "Row payload is missing."));
                    continue;
                }

                String productName = normalizeRequired(request.productName());
                String sku = normalizeRequired(request.sku());
                String category = normalizeRequired(request.category());
                BigDecimal buyingPrice = request.buyingPrice();
                BigDecimal sellingPrice = request.sellingPrice();
                String unit = normalizeOptional(request.unit());
                Double stockQuantity = request.stockQuantity();
                Double reorderLevel = request.reorderLevel();

                skuForError = sku;

                List<String> validationErrors = validateBulkValues(
                        productName,
                        sku,
                        category,
                        buyingPrice,
                        sellingPrice,
                        unit,
                        stockQuantity,
                        reorderLevel
                );
                if (!validationErrors.isEmpty()) {
                    errors.add(new ProductImportError(rowNumber, sku, String.join(" ", validationErrors)));
                    continue;
                }

                String skuKey = sku.toLowerCase(Locale.ROOT);
                if (!seenSkus.add(skuKey)) {
                    errors.add(new ProductImportError(rowNumber, sku, "Duplicate SKU in CSV file."));
                    continue;
                }

                if (repository.existsBySkuAndIsActiveTrue(sku)) {
                    errors.add(new ProductImportError(rowNumber, sku, "SKU already exists in the database."));
                    continue;
                }

                Product product = new Product(
                        productName,
                        sku,
                    null,
                        category,
                        buyingPrice,
                        sellingPrice,
                        unit,
                        stockQuantity,
                        reorderLevel
                );

                Product saved = repository.saveAndFlush(product);
                importedProducts.add(toImportSuccess(saved));
            } catch (DataIntegrityViolationException ex) {
                errors.add(new ProductImportError(
                        rowNumber,
                        skuForError,
                        "Could not import row because SKU or database constraints were violated."
                ));
            } catch (Exception ex) {
                errors.add(new ProductImportError(
                        rowNumber,
                        skuForError,
                        "Unexpected import error: " + safeMessage(ex.getMessage())
                ));
            }
        }

        return new ProductBulkImportResponse(
                requests.size(),
                importedProducts.size(),
                errors.size(),
                importedProducts,
                errors
        );
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    public Product updateProduct(Long id, ProductRequest request) {
        Product existing = getProductById(id);
        String sku = normalizeOptional(request.sku());
        String barcode = normalizeOptional(request.barcode());

        if (sku != null) {
            repository.findBySkuAndIsActiveTrue(sku)
                    .filter(other -> !other.getId().equals(id))
                    .ifPresent(other -> {
                        throw new ResponseStatusException(
                                HttpStatus.CONFLICT,
                                "SKU '" + sku + "' is already used by another product.");
                    });
        }

        // Guard: reject if the new barcode is already taken by a *different* active product.
        if (barcode != null) {
            repository.findByBarcodeAndIsActiveTrue(barcode)
                    .filter(other -> !other.getId().equals(id))
                    .ifPresent(other -> {
                        throw new ResponseStatusException(
                                HttpStatus.CONFLICT,
                                "Barcode '" + barcode + "' is already used by another product.");
                    });
        }

        existing.setProductName(request.productName());
        existing.setSku(sku);
        existing.setBarcode(barcode);
        existing.setCategory(request.category());
        existing.setBuyingPrice(request.buyingPrice());
        existing.setSellingPrice(request.sellingPrice());
        existing.setUnit(request.unit());
        if (request.stockQuantity() != null) {
            existing.setStockQuantity(request.stockQuantity());
        }
        existing.setReorderLevel(request.reorderLevel());

        return repository.save(existing);
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    public void deleteProduct(Long id) {
        // Ensures 404 for unknown/inactive id before attempting hard delete.
        Product product = getProductById(id);

        boolean hasRemainingStock = hasStock(product);
        boolean isAssignedToSupplier = isSupplierAssigned(product);

        if (hasRemainingStock || isAssignedToSupplier) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    buildDeleteBlockedMessage(hasRemainingStock, isAssignedToSupplier));
        }

        if (inventoryRepository.existsByProductId(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot delete product: It is currently present in the Inventory.");
        }

        repository.deleteById(id);
    }

    private boolean hasStock(Product product) {
        Double stockQuantity = product.getStockQuantity();
        return stockQuantity != null && stockQuantity > STOCK_EPSILON;
    }

    private boolean isSupplierAssigned(Product product) {
        return product.getSupplierId() != null || product.getSupplier() != null;
    }

    private String buildDeleteBlockedMessage(boolean hasRemainingStock, boolean isAssignedToSupplier) {
        if (hasRemainingStock && isAssignedToSupplier) {
            return "Cannot delete product. There is still remaining stock in the inventory. Cannot delete product. This item is currently assigned to a supplier.";
        }

        if (hasRemainingStock) {
            return "Cannot delete product. There is still remaining stock in the inventory.";
        }

        return "Cannot delete product. This item is currently assigned to a supplier.";
    }

    // ── GET UNASSIGNED ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Product> getUnassignedProducts() {
        return repository.findBySupplierIsNullAndIsActiveTrue();
    }

    // ── GET BY SUPPLIER ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Product> getProductsBySupplierId(Long supplierId) {
        return repository.findBySupplierIdAndIsActiveTrue(supplierId);
    }

    // ── SEARCH BY SKU ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Product getProductBySku(String sku) {
        return repository.findBySkuAndIsActiveTrue(sku)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No product found with SKU: '" + sku + "'"));
    }

    // ── UNASSIGN ────────────────────────────────────────────────────────────

    public Product unassignProduct(Long id) {
        Product product = getProductById(id);
        product.setSupplier(null);
        return repository.save(product);
    }

    private List<String> validateBulkValues(
            String productName,
            String sku,
            String category,
            BigDecimal buyingPrice,
            BigDecimal sellingPrice,
            String unit,
            Double stockQuantity,
            Double reorderLevel
    ) {
        List<String> errors = new ArrayList<>();

        if (productName == null || productName.isEmpty()) {
            errors.add("Product name is required.");
        } else if (productName.length() > 255) {
            errors.add("Product name must be 255 characters or fewer.");
        }

        if (sku == null || sku.isEmpty()) {
            errors.add("SKU / ProductID is required.");
        } else if (sku.length() > 100) {
            errors.add("SKU must be 100 characters or fewer.");
        }

        if (category == null || category.isEmpty()) {
            errors.add("Category is required.");
        } else if (category.length() > 100) {
            errors.add("Category must be 100 characters or fewer.");
        }

        if (buyingPrice == null) {
            errors.add("Buying price is required.");
        } else if (buyingPrice.compareTo(BigDecimal.ZERO) < 0) {
            errors.add("Buying price must be 0 or greater.");
        }

        if (sellingPrice == null) {
            errors.add("Selling price is required.");
        } else if (sellingPrice.compareTo(BigDecimal.ZERO) < 0) {
            errors.add("Selling price must be 0 or greater.");
        }

        if (unit != null && unit.length() > 50) {
            errors.add("Unit must be 50 characters or fewer.");
        }

        if (stockQuantity != null && stockQuantity < 0) {
            errors.add("Stock quantity must be 0 or greater.");
        }

        if (reorderLevel != null && reorderLevel < 0) {
            errors.add("Reorder level must be 0 or greater.");
        }

        return errors;
    }

    private static String normalizeRequired(String value) {
        return value == null ? null : value.trim();
    }

    private static String normalizeOptional(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String generateSku() {
        for (int i = 0; i < 10; i++) {
            long timePart = System.currentTimeMillis() % 1_000_000_000L;
            int randomPart = ThreadLocalRandom.current().nextInt(100, 1000);
            String candidate = SKU_PREFIX + timePart + randomPart;
            if (!repository.existsBySkuAndIsActiveTrue(candidate)) {
                return candidate;
            }
        }

        throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unable to generate a unique SKU. Please try again.");
    }

    private static ProductImportSuccess toImportSuccess(Product saved) {
        return new ProductImportSuccess(
                saved.getId(),
                saved.getProductName(),
                saved.getSku(),
                saved.getCategory(),
                saved.getBuyingPrice(),
                saved.getSellingPrice(),
                saved.getUnit(),
                saved.getStockQuantity(),
                saved.getReorderLevel()
        );
    }

    private static String safeMessage(String message) {
        return message == null || message.isBlank() ? "No details available." : message;
    }
}
