package com.dissayakesuper.web_pos_backend.supplier.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dissayakesuper.web_pos_backend.supplier.dto.AssignProductsRequest;
import com.dissayakesuper.web_pos_backend.supplier.dto.SupplierRequest;
import com.dissayakesuper.web_pos_backend.supplier.dto.SupplierStatusRequest;
import com.dissayakesuper.web_pos_backend.supplier.entity.Supplier;
import com.dissayakesuper.web_pos_backend.supplier.service.SupplierService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/suppliers")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class SupplierController {

    private final SupplierService service;

    public SupplierController(SupplierService service) {
        this.service = service;
    }

    // ── GET /api/suppliers ────────────────────────────────────────────────────
    /** Returns all suppliers. */
    @GetMapping
    public ResponseEntity<List<Supplier>> getAll() {
        return ResponseEntity.ok(service.getAllSuppliers());
    }

    // ── GET /api/suppliers/{id} ───────────────────────────────────────────────
    /** Returns a single supplier by id. 404 if not found. */
    @GetMapping("/{id}")
    public ResponseEntity<Supplier> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getSupplierById(id));
    }

    // ── POST /api/suppliers ───────────────────────────────────────────────────
    /** Creates a new supplier. Returns 201 Created. */
    @PostMapping
    public ResponseEntity<Supplier> create(@Valid @RequestBody SupplierRequest request) {
        Supplier created = service.createSupplier(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ── PUT /api/suppliers/{id} ───────────────────────────────────────────────
    /** Replaces all mutable fields of an existing supplier. 404 if not found. */
    @PutMapping("/{id}")
    public ResponseEntity<Supplier> update(
            @PathVariable Long id,
            @Valid @RequestBody SupplierRequest request) {
        return ResponseEntity.ok(service.updateSupplier(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Supplier> updateActiveStatus(
            @PathVariable Long id,
            @Valid @RequestBody SupplierStatusRequest request) {
        return ResponseEntity.ok(service.updateSupplierActiveStatus(id, request.isActive()));
    }

    // ── DELETE /api/suppliers/{id} ────────────────────────────────────────────
    /** Deletes a supplier by id. Returns 204 No Content. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteSupplier(id);
        return ResponseEntity.noContent().build();
    }
    // ── POST /api/suppliers/{id}/products ────────────────────────────────
    /** Assigns a list of products to a supplier. Returns 204 No Content. */
    @PostMapping("/{id}/products")
    public ResponseEntity<Void> assignProducts(
            @PathVariable Long id,
            @Valid @RequestBody AssignProductsRequest request) {
        service.assignProducts(id, request.productIds());
        return ResponseEntity.noContent().build();
    }}
