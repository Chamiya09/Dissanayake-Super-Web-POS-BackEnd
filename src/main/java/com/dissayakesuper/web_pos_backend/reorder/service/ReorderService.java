package com.dissayakesuper.web_pos_backend.reorder.service;

import com.dissayakesuper.web_pos_backend.reorder.dto.*;
import com.dissayakesuper.web_pos_backend.reorder.entity.Reorder;
import com.dissayakesuper.web_pos_backend.reorder.entity.ReorderItem;
import com.dissayakesuper.web_pos_backend.reorder.entity.Status;
import com.dissayakesuper.web_pos_backend.reorder.repository.ReorderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class ReorderService {

    private final ReorderRepository reorderRepository;
    private final EmailService emailService;

    public ReorderService(ReorderRepository reorderRepository,
                          EmailService emailService) {
        this.reorderRepository = reorderRepository;
        this.emailService = emailService;
    }

    // ── CREATE ORDER ──────────────────────────────────────────────────────────

    /**
     * Maps {@link ReorderRequestDTO} → entities, calculates total, persists and
     * returns the saved order as a {@link ReorderResponseDTO}.
     *
     * @throws ResponseStatusException 409 if orderRef already exists
     */
    public ReorderResponseDTO createOrder(ReorderRequestDTO dto) {
        if (reorderRepository.existsByOrderRef(dto.orderRef())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An order with ref '" + dto.orderRef() + "' already exists.");
        }

        // Build item entities and accumulate total
        double total = 0.0;
        Reorder reorder = Reorder.builder()
                .orderRef(dto.orderRef())
                .supplierEmail(dto.supplierEmail())
                .status(Status.PENDING)
                .totalAmount(0.0)   // updated below after items are added
                .build();

        for (ReorderItemRequestDTO itemDTO : dto.items()) {
            ReorderItem item = ReorderItem.builder()
                    .productName(itemDTO.productName())
                    .quantity(itemDTO.quantity())
                    .unitPrice(itemDTO.unitPrice())
                    .build();

            reorder.addItem(item);
            total += itemDTO.quantity() * itemDTO.unitPrice();
        }

        reorder.setTotalAmount(total);

        Reorder saved = reorderRepository.save(reorder);

        // Fire-and-forget — runs on the Spring async executor so it does not
        // block the HTTP response or roll back the transaction on mail failure.
        emailService.sendPurchaseOrderEmail(
                saved.getSupplierEmail(),
                saved.getOrderRef(),
                dto.items(),
                saved.getTotalAmount(),
                "Purchasing Manager"   // TODO: pass authenticated user's name here
        );

        return ReorderResponseDTO.from(saved);
    }

    // ── READ ALL ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ReorderResponseDTO> getAllOrders() {
        return reorderRepository.findAll()
                .stream()
                .map(ReorderResponseDTO::from)
                .toList();
    }

    // ── ORDER HISTORY (sorted newest-first) ───────────────────────────────────

    /**
     * Returns all orders sorted by {@code createdAt} descending so the
     * front-end history table always shows the most recent orders at the top.
     */
    @Transactional(readOnly = true)
    public List<ReorderResponseDTO> getOrderHistory() {
        return reorderRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(ReorderResponseDTO::from)
                .toList();
    }

    // ── READ ONE ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ReorderResponseDTO getOrderById(Long id) {
        Reorder reorder = findOrThrow(id);
        return ReorderResponseDTO.from(reorder);
    }

    // ── UPDATE STATUS ─────────────────────────────────────────────────────────

    /**
     * Updates the status of an existing order.
     * Rules:
     * <ul>
     *   <li>A CANCELLED or RECEIVED order cannot be transitioned to any other status.</li>
     *   <li>PENDING → CONFIRMED, CANCELLED</li>
     *   <li>CONFIRMED → RECEIVED, CANCELLED</li>
     * </ul>
     *
     * @throws ResponseStatusException 404 if order not found
     * @throws ResponseStatusException 422 if the transition is illegal
     */
    public ReorderResponseDTO updateStatus(Long id, Status newStatus) {
        Reorder reorder = findOrThrow(id);
        validateTransition(reorder.getStatus(), newStatus, reorder.getOrderRef());
        reorder.setStatus(newStatus);
        return ReorderResponseDTO.from(reorder);   // dirty-checking flushes on commit
    }

    // ── GET LOW-STOCK ITEMS (MOCKED) ──────────────────────────────────────────

    /**
     * Returns a list of products whose current stock is at or below their
     * reorder level.
     *
     * <p><strong>Note:</strong> This list is currently mocked. Once the
     * Inventory module exposes a suitable query method this should be replaced
     * with a real repository call via an injected {@code InventoryRepository}.
     */
    @Transactional(readOnly = true)
    public List<LowStockItemDTO> getLowStockItems() {
        return List.of(
                new LowStockItemDTO(101L, "Basmati Rice (5 kg)",           "RCE-001", "Dry Goods",   4.0,  20.0, "bags"),
                new LowStockItemDTO(102L, "Sunflower Cooking Oil (1 L)",    "OIL-002", "Oils & Fats", 0.0,  15.0, "bottles"),
                new LowStockItemDTO(103L, "Full Cream Milk Powder (400 g)", "MLK-003", "Dairy",       7.0,  25.0, "tins"),
                new LowStockItemDTO(104L, "White Sugar (1 kg)",             "SUG-004", "Dry Goods",   3.0,  30.0, "bags"),
                new LowStockItemDTO(105L, "Coconut Oil (750 ml)",           "COL-005", "Oils & Fats", 2.0,  10.0, "bottles")
        );
    }

    // ── INTERNAL HELPERS ──────────────────────────────────────────────────────

    private Reorder findOrThrow(Long id) {
        return reorderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Reorder not found with id: " + id));
    }

    /**
     * Guards against illegal status transitions.
     *
     * @throws ResponseStatusException 422 (UNPROCESSABLE_ENTITY) if the
     *                                 transition is not permitted
     */
    private void validateTransition(Status current, Status requested, String orderRef) {
        // Terminal states — no further transitions allowed
        if (current == Status.CANCELLED || current == Status.RECEIVED) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Order '" + orderRef + "' is already " + current
                            + " and cannot be transitioned to " + requested + ".");
        }

        // PENDING can only move to CONFIRMED or CANCELLED
        if (current == Status.PENDING
                && requested != Status.CONFIRMED
                && requested != Status.CANCELLED) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Order '" + orderRef + "' is PENDING; allowed transitions: CONFIRMED, CANCELLED.");
        }

        // CONFIRMED can only move to RECEIVED or CANCELLED
        if (current == Status.CONFIRMED
                && requested != Status.RECEIVED
                && requested != Status.CANCELLED) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Order '" + orderRef + "' is CONFIRMED; allowed transitions: RECEIVED, CANCELLED.");
        }
    }
}
