package com.dissayakesuper.web_pos_backend.product.controller;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.dissayakesuper.web_pos_backend.product.dto.ProductBulkImportResponse;
import com.dissayakesuper.web_pos_backend.product.dto.ProductPageResponse;
import com.dissayakesuper.web_pos_backend.product.dto.ProductRequest;
import com.dissayakesuper.web_pos_backend.product.entity.Product;
import com.dissayakesuper.web_pos_backend.product.service.ProductService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class ProductController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    // ── GET /api/products ─────────────────────────────────────────────────────
    /** Returns all products. */
    @GetMapping
    public ResponseEntity<List<Product>> getAll() {
        return ResponseEntity.ok(service.getAllProducts());
    }

    // ── GET /api/products/page?page=0&limit=50&search=milk ─────────────────
    /** Returns active products in pages for large catalogs. */
    @GetMapping("/page")
    public ResponseEntity<ProductPageResponse> getPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String search) {
        int safePage = Math.max(0, page);
        int safeLimit = Math.max(1, Math.min(limit, 200));

        try {
            return ResponseEntity.ok(service.getProductsPage(safePage, safeLimit, search));
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error(
                    "Failed to fetch products page. page={}, limit={}, search='{}'",
                    safePage,
                    safeLimit,
                    search,
                    ex
            );
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch paginated products.");
        }
    }

    // ── GET /api/products/{id} ────────────────────────────────────────────────
    /** Returns a single product by id. 404 if not found. */
    @GetMapping("/{id:\\d+}")
    public ResponseEntity<Product> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getProductById(id));
    }

    // ── POST /api/products ────────────────────────────────────────────────────
    /** Creates a new product. Returns 201 Created. */
    @PostMapping
    public ResponseEntity<Product> create(@Valid @RequestBody ProductRequest request) {
        Product created = service.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ── POST /api/products/bulk-import ───────────────────────────────────────
    /** Imports multiple products (typically parsed from a CSV file). */
    @PostMapping("/bulk-import")
    public ResponseEntity<ProductBulkImportResponse> bulkImport(
            @Valid @RequestBody List<@Valid ProductRequest> requests) {
        ProductBulkImportResponse result = service.importProducts(requests);
        HttpStatus status = result.failedCount() > 0 ? HttpStatus.MULTI_STATUS : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(result);
    }

    // ── PUT /api/products/{id} ────────────────────────────────────────────────
    /** Replaces all mutable fields of an existing product. 404 if not found. */
    @PutMapping("/{id:\\d+}")
    public ResponseEntity<Product> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(service.updateProduct(id, request));
    }

    // ── DELETE /api/products/{id} ─────────────────────────────────────────────
    /** Deletes a product by id. Returns 204 No Content. */
    @DeleteMapping("/{id:\\d+}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    // ── GET /api/products/unassigned ─────────────────────────────────────────
    /** Returns only products with no supplier assigned (supplier_id IS NULL). */
    @GetMapping("/unassigned")
    public ResponseEntity<List<Product>> getUnassigned() {
        return ResponseEntity.ok(service.getUnassignedProducts());
    }

    // ── GET /api/products/by-supplier/{supplierId} ────────────────────────────
    /** Returns all products assigned to a specific supplier. */
    @GetMapping("/by-supplier/{supplierId}")
    public ResponseEntity<List<Product>> getBySupplierId(@PathVariable Long supplierId) {
        return ResponseEntity.ok(service.getProductsBySupplierId(supplierId));
    }

    // ── GET /api/products/search?sku={sku} ───────────────────────────────────
    /** Returns the product matching the exact SKU. 404 if not found. */
    @GetMapping("/search")
    public ResponseEntity<Product> searchBySku(@RequestParam String sku) {
        return ResponseEntity.ok(service.getProductBySku(sku));
    }

    // ── GET /api/products/available-for-inventory ─────────────────────────────
    /** Returns products that have no Inventory record yet (safe to add as new stock entries). */
    @GetMapping("/available-for-inventory")
    public ResponseEntity<List<Product>> getAvailableForInventory() {
        return ResponseEntity.ok(service.getProductsAvailableForInventory());
    }

    // ── PATCH /api/products/{id}/unassign ──────────────────────────────────────
    /** Removes the supplier association from a product. Returns the updated product. */
    @PatchMapping("/{id:\\d+}/unassign")
    public ResponseEntity<Product> unassign(@PathVariable Long id) {
        return ResponseEntity.ok(service.unassignProduct(id));
    }
}
