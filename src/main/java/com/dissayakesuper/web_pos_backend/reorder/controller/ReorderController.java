package com.dissayakesuper.web_pos_backend.reorder.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.dissayakesuper.web_pos_backend.reorder.dto.LowStockItemDTO;
import com.dissayakesuper.web_pos_backend.reorder.dto.ReorderRequestDTO;
import com.dissayakesuper.web_pos_backend.reorder.dto.ReorderResponseDTO;
import com.dissayakesuper.web_pos_backend.reorder.dto.ReorderUpdateDTO;
import com.dissayakesuper.web_pos_backend.reorder.entity.Status;
import com.dissayakesuper.web_pos_backend.reorder.service.ReorderService;

import jakarta.validation.Valid;

/**
 * REST controller for the Reorder module.
 *
 * <pre>
 *   POST   /api/v1/reorder/create      → createOrder
 *   GET    /api/v1/reorder/low-stock   → getLowStockItems
 *   GET    /api/v1/reorder/history     → getOrderHistory (sorted newest-first)
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/reorder")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class ReorderController {

    private final ReorderService reorderService;

    public ReorderController(ReorderService reorderService) {
        this.reorderService = reorderService;
    }

    // ── POST /api/v1/reorder/create ───────────────────────────────────────────

    /**
     * Creates a new purchase order and fires a purchase-order email to the
     * supplier asynchronously.
     *
     * @param dto validated request body containing orderRef, supplierEmail and
     *            at least one line item
     * @return {@code 201 Created} with the persisted {@link ReorderResponseDTO}
     */
    @PostMapping("/create")
    public ResponseEntity<ReorderResponseDTO> createOrder(
            @Valid @RequestBody ReorderRequestDTO dto,
            Authentication authentication) {

        String managerName = (authentication != null && authentication.getName() != null)
                ? authentication.getName()
                : "Store Manager";
        ReorderResponseDTO created = reorderService.createOrder(dto, managerName);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // ── PUT /api/v1/reorder/{id} ──────────────────────────────────────────────

    /**
     * Updates the supplierEmail and/or items of an existing purchase order.
     * Returns 409 if the order is in a terminal state (CANCELLED / RECEIVED).
     */
    @PutMapping("/{id}")
    public ResponseEntity<ReorderResponseDTO> updateOrder(
            @PathVariable Long id,
            @Valid @RequestBody ReorderUpdateDTO dto) {
        return ResponseEntity.ok(reorderService.updateOrder(id, dto));
    }

    // ── GET /api/v1/reorder/low-stock ─────────────────────────────────────────

    /**
     * Returns products whose current stock is at or below their reorder level.
     *
     * @return {@code 200 OK} with a list of {@link LowStockItemDTO}
     */
    @GetMapping("/low-stock")
    public ResponseEntity<List<LowStockItemDTO>> getLowStockItems() {
        return ResponseEntity.ok(reorderService.getLowStockItems());
    }

    // ── GET /api/v1/reorder/history ───────────────────────────────────────────

    /**
     * Returns all purchase orders sorted by creation date descending (newest
     * first), suitable for rendering a history table.
     *
     * @return {@code 200 OK} with a list of {@link ReorderResponseDTO}
     */
    @GetMapping("/history")
    public ResponseEntity<List<ReorderResponseDTO>> getOrderHistory() {
        return ResponseEntity.ok(reorderService.getOrderHistory());
    }

    // ── PATCH /api/v1/reorder/{id}/status ────────────────────────────────────

    /**
     * Transitions the status of an existing purchase order.
     * Accepts {@code {"status": "CANCELLED"}} (or CONFIRMED / RECEIVED).
     *
     * @param id   numeric DB primary key of the order
     * @param body JSON object with a single "status" key
     * @return {@code 200 OK} with the updated {@link ReorderResponseDTO}
     * @throws ResponseStatusException 400 if the status value is missing or unknown
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ReorderResponseDTO> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String statusStr = body.get("status");
        if (statusStr == null || statusStr.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing 'status' field.");
        }
        Status newStatus;
        try {
            newStatus = Status.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unknown status '" + statusStr + "'. Valid values: PENDING, CONFIRMED, CANCELLED, RECEIVED.");
        }
        return ResponseEntity.ok(reorderService.updateStatus(id, newStatus));
    }
}
