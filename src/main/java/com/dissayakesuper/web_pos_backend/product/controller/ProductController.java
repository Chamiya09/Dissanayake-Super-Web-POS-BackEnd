package com.dissayakesuper.web_pos_backend.product.controller;

import java.util.List;

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

import com.dissayakesuper.web_pos_backend.product.dto.ProductRequest;
import com.dissayakesuper.web_pos_backend.product.entity.Product;
import com.dissayakesuper.web_pos_backend.product.service.ProductService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class ProductController {

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

    // ── GET /api/products/{id} ────────────────────────────────────────────────
    /** Returns a single product by id. 404 if not found. */
    @GetMapping("/{id}")
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

    // ── PUT /api/products/{id} ────────────────────────────────────────────────
    /** Replaces all mutable fields of an existing product. 404 if not found. */
    @PutMapping("/{id}")
    public ResponseEntity<Product> update(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(service.updateProduct(id, request));
    }

    // ── DELETE /api/products/{id} ─────────────────────────────────────────────
    /** Deletes a product by id. Returns 204 No Content. */
    @DeleteMapping("/{id}")
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

    // ── PATCH /api/products/{id}/unassign ──────────────────────────────────────
    /** Removes the supplier association from a product. Returns the updated product. */
    @PatchMapping("/{id}/unassign")
    public ResponseEntity<Product> unassign(@PathVariable Long id) {
        return ResponseEntity.ok(service.unassignProduct(id));
    }
}
