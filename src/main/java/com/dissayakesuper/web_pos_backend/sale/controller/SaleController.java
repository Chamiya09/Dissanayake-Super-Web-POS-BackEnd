package com.dissayakesuper.web_pos_backend.sale.controller;

import com.dissayakesuper.web_pos_backend.sale.dto.StatusRequest;
import com.dissayakesuper.web_pos_backend.sale.entity.Sale;
import com.dissayakesuper.web_pos_backend.sale.service.SaleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sales")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class SaleController {

    private final SaleService saleService;

    public SaleController(SaleService saleService) {
        this.saleService = saleService;
    }

    // ── GET /api/sales ────────────────────────────────────────────────────────
    /** Returns the full sales history. */
    @GetMapping
    public ResponseEntity<List<Sale>> getAll() {
        return ResponseEntity.ok(saleService.getAllSales());
    }

    // ── GET /api/sales/{id} ───────────────────────────────────────────────────
    /** Returns a single sale (with its line items). 404 if not found. */
    @GetMapping("/{id}")
    public ResponseEntity<Sale> getById(@PathVariable Long id) {
        return ResponseEntity.ok(saleService.getSaleById(id));
    }

    // ── POST /api/sales ───────────────────────────────────────────────────────
    /** Records a new sale from the POS checkout. Returns 201 Created. */
    @PostMapping
    public ResponseEntity<Sale> create(@Valid @RequestBody Sale sale) {
        Sale created = saleService.createSale(sale);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ── PUT /api/sales/{id}/status ────────────────────────────────────────────
    /** Updates the status of a sale (e.g., "Completed" → "Voided"). */
    @PutMapping("/{id}/status")
    public ResponseEntity<Sale> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusRequest request) {
        Sale updated = saleService.updateSaleStatus(id, request.status());
        return ResponseEntity.ok(updated);
    }
}
