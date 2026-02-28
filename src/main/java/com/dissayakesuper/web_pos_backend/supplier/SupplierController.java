package com.dissayakesuper.web_pos_backend.supplier;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
@CrossOrigin(origins = "*")   // allow React dev server (Vite default: http://localhost:5173)
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

    // ── DELETE /api/suppliers/{id} ────────────────────────────────────────────
    /** Deletes a supplier by id. Returns 204 No Content. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteSupplier(id);
        return ResponseEntity.noContent().build();
    }
}
