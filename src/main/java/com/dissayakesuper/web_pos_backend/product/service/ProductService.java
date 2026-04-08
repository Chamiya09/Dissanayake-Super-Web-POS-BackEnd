package com.dissayakesuper.web_pos_backend.product.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.dissayakesuper.web_pos_backend.product.dto.ProductBulkImportResponse;
import com.dissayakesuper.web_pos_backend.product.dto.ProductImportError;
import com.dissayakesuper.web_pos_backend.product.dto.ProductImportSuccess;
import com.dissayakesuper.web_pos_backend.product.dto.ProductRequest;
import com.dissayakesuper.web_pos_backend.product.entity.Product;
import com.dissayakesuper.web_pos_backend.product.repository.ProductRepository;

@Service
@Transactional
public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    // ── READ ALL ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        return repository.findByIsActiveTrue();
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
        String barcode = normalizeOptional(request.sku());

        if (barcode != null && repository.existsBySkuAndIsActiveTrue(barcode)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A product with barcode '" + barcode + "' already exists.");
        }

        Product product = new Product(
                request.productName(),
                barcode,
                request.category(),
                request.buyingPrice(),
                request.sellingPrice(),
                request.unit(),
                request.stockQuantity(),
                request.reorderLevel()
        );

        return repository.save(product);
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
        String barcode = normalizeOptional(request.sku());

        // Guard: reject if the new barcode is already taken by a *different* active product.
        if (barcode != null) {
            repository.findBySkuAndIsActiveTrue(barcode)
                    .filter(other -> !other.getId().equals(id))
                    .ifPresent(other -> {
                        throw new ResponseStatusException(
                                HttpStatus.CONFLICT,
                                "Barcode '" + barcode + "' is already used by another product.");
                    });
        }

        existing.setProductName(request.productName());
        existing.setSku(barcode);
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
        // Ensures 404 for unknown/inactive id before attempting update query.
        getProductById(id);

        // Soft-delete + barcode release in one write operation.
        repository.softDeleteAndReleaseBarcode(id);
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
