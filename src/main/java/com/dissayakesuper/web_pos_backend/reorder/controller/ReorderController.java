package com.dissayakesuper.web_pos_backend.reorder.controller;

import com.dissayakesuper.web_pos_backend.reorder.dto.LowStockItemDTO;
import com.dissayakesuper.web_pos_backend.reorder.dto.ReorderRequestDTO;
import com.dissayakesuper.web_pos_backend.reorder.dto.ReorderResponseDTO;
import com.dissayakesuper.web_pos_backend.reorder.service.ReorderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}
