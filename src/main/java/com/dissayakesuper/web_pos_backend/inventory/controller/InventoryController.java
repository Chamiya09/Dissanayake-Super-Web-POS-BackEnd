package com.dissayakesuper.web_pos_backend.inventory.controller;

import com.dissayakesuper.web_pos_backend.inventory.dto.AddStockRequest;
import com.dissayakesuper.web_pos_backend.inventory.dto.EditInventoryRequest;
import com.dissayakesuper.web_pos_backend.inventory.dto.InventoryStatusResponse;
import com.dissayakesuper.web_pos_backend.inventory.entity.Inventory;
import com.dissayakesuper.web_pos_backend.inventory.entity.InventoryLog;
import com.dissayakesuper.web_pos_backend.inventory.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class InventoryController {

    private final InventoryService service;

    public InventoryController(InventoryService service) {
        this.service = service;
    }

    // ── GET /api/inventory/status ─────────────────────────────────────────────
    /**
     * Returns all products with their linked inventory details.
     * Includes a computed {@code stockStatus} field per row.
     */
    @GetMapping("/status")
    public ResponseEntity<List<InventoryStatusResponse>> getAllStatus() {
        return ResponseEntity.ok(service.getInventoryStatus());
    }

    // ── GET /api/inventory/low-stock ──────────────────────────────────────────
    /**
     * Returns only items where {@code stockQuantity < reorderLevel}.
     */
    @GetMapping("/low-stock")
    public ResponseEntity<List<InventoryStatusResponse>> getLowStock() {
        List<InventoryStatusResponse> result = service.getLowStockInventory()
                .stream()
                .map(InventoryStatusResponse::from)
                .toList();
        return ResponseEntity.ok(result);
    }

    // ── GET /api/inventory/logs/{productId} ───────────────────────────────────
    /**
     * Returns the full stock-change history for a product, newest first.
     */
    @GetMapping("/logs/{productId}")
    public ResponseEntity<List<InventoryLog>> getLogs(@PathVariable Long productId) {
        return ResponseEntity.ok(service.getLogsByProductId(productId));
    }

    // ── PUT /api/inventory/add-stock/{productId} ──────────────────────────────
    /**
     * Adds the given quantity to a product's current stock.
     * Creates an inventory record automatically if none exists.
     * Returns the updated inventory as a status response.
     */
    @PutMapping("/add-stock/{productId}")
    public ResponseEntity<InventoryStatusResponse> addStock(
            @PathVariable Long productId,
            @Valid @RequestBody AddStockRequest request) {
        Inventory updated = service.updateStock(productId, request.quantity());
        return ResponseEntity.ok(InventoryStatusResponse.from(updated));
    }

    // ── PUT /api/inventory/edit/{id} ──────────────────────────────────────────
    /**
     * Updates the reorderLevel and/or unit of an existing inventory record.
     * Does NOT change stockQuantity — use add-stock for that.
     */
    @PutMapping("/edit/{id}")
    public ResponseEntity<InventoryStatusResponse> editInventory(
            @PathVariable Long id,
            @Valid @RequestBody EditInventoryRequest request) {
        Inventory updated = service.editInventory(id, request);
        return ResponseEntity.ok(InventoryStatusResponse.from(updated));
    }

    // ── DELETE /api/inventory/{id} ────────────────────────────────────────────
    /**
     * Removes the inventory tracking record — does NOT delete the Product.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInventory(@PathVariable Long id) {
        service.deleteInventory(id);
        return ResponseEntity.noContent().build();
    }
}
