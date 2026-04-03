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
        return repository.findAll();
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
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product not found with id: " + id));
    }

    // ── CREATE ────────────────────────────────────────────────────────────────

    public Product createProduct(ProductRequest request) {
        if (repository.existsBySku(request.sku())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A product with SKU '" + request.sku() + "' already exists.");
        }

        Product product = new Product(
                request.productName(),
                request.sku(),
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

        List<Product> importedProducts = new ArrayList<>();
        List<ProductImportError> errors = new ArrayList<>();
        Set<String> seenSkus = new HashSet<>();

        for (int i = 0; i < requests.size(); i++) {
            int rowNumber = i + 2; // row 1 is CSV header
            ProductRequest request = requests.get(i);

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

            if (repository.existsBySku(sku)) {
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

            try {
                importedProducts.add(repository.saveAndFlush(product));
            } catch (DataIntegrityViolationException ex) {
                errors.add(new ProductImportError(
                        rowNumber,
                        sku,
                        "Could not import row because SKU or database constraints were violated."
                ));
            } catch (Exception ex) {
                errors.add(new ProductImportError(
                        rowNumber,
                        sku,
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

        // Guard: reject if the new SKU is already taken by a *different* product
        repository.findBySku(request.sku())
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "SKU '" + request.sku() + "' is already used by another product.");
                });

        existing.setProductName(request.productName());
        existing.setSku(request.sku());
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
        Product product = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product not found with id: " + id));

        // Explicitly remove supplier assignment before delete so all supplier views
        // and queries observe this relationship cleanup in the same transaction.
        if (product.getSupplierId() != null) {
            product.setSupplier(null);
            repository.save(product);
        }

        repository.delete(product);
    }

    // ── GET UNASSIGNED ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Product> getUnassignedProducts() {
        return repository.findBySupplierIsNull();
    }

    // ── GET BY SUPPLIER ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Product> getProductsBySupplierId(Long supplierId) {
        return repository.findBySupplierId(supplierId);
    }

    // ── SEARCH BY SKU ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Product getProductBySku(String sku) {
        return repository.findBySku(sku)
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

    private static String safeMessage(String message) {
        return message == null || message.isBlank() ? "No details available." : message;
    }
}
