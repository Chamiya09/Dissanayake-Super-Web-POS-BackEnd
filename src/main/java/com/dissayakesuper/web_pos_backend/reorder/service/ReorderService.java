package com.dissayakesuper.web_pos_backend.reorder.service;

import com.dissayakesuper.web_pos_backend.inventory.entity.Inventory;
import com.dissayakesuper.web_pos_backend.inventory.repository.InventoryRepository;
import com.dissayakesuper.web_pos_backend.reorder.dto.*;
import com.dissayakesuper.web_pos_backend.reorder.entity.Reorder;
import com.dissayakesuper.web_pos_backend.reorder.entity.ReorderItem;
import com.dissayakesuper.web_pos_backend.reorder.entity.Status;
import com.dissayakesuper.web_pos_backend.reorder.repository.ReorderRepository;
import com.dissayakesuper.web_pos_backend.supplier.repository.SupplierRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class ReorderService {

    private final ReorderRepository    reorderRepository;
    private final EmailService          emailService;
    private final InventoryRepository   inventoryRepository;
    private final SupplierRepository    supplierRepository;

    public ReorderService(ReorderRepository reorderRepository,
                          EmailService emailService,
                          InventoryRepository inventoryRepository,
                          SupplierRepository supplierRepository) {
        this.reorderRepository  = reorderRepository;
        this.emailService       = emailService;
        this.inventoryRepository = inventoryRepository;
        this.supplierRepository  = supplierRepository;
    }

    // ── CREATE ORDER ──────────────────────────────────────────────────────────

    /**
     * Maps {@link ReorderRequestDTO} → entities, calculates total, persists and
     * returns the saved order as a {@link ReorderResponseDTO}.
     *
     * @throws ResponseStatusException 409 if orderRef already exists
     */
    public ReorderResponseDTO createOrder(ReorderRequestDTO dto, String managerName) {
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
                    .productId(itemDTO.productId())      // nullable soft-link
                    .quantity(itemDTO.quantity())
                    .unitPrice(itemDTO.unitPrice())
                    .build();

            reorder.addItem(item);
            total += itemDTO.quantity() * itemDTO.unitPrice();
        }

        reorder.setTotalAmount(total);

        Reorder saved = reorderRepository.save(reorder);

        // Resolve supplier display name for emails (null-safe: supplier link is optional)
        String supplierName = supplierRepository.findByEmail(dto.supplierEmail())
                .map(s -> s.getCompanyName())
                .orElse(null);
        String today = java.time.LocalDate.now().toString();

        // Fire-and-forget — runs on the Spring async executor so it does not
        // block the HTTP response or roll back the transaction on mail failure.
        emailService.sendSupplierPO(
                saved.getSupplierEmail(),
                supplierName,
                saved.getOrderRef(),
                dto.items(),
                saved.getTotalAmount(),
                managerName
        );
        emailService.sendAdminNotification(
                saved.getOrderRef(),
                supplierName,
                saved.getSupplierEmail(),
                saved.getTotalAmount(),
                managerName,
                today
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

    /**
     * Returns all {@code Inventory} rows where {@code stock_quantity <= reorder_level},
     * fetched live from the database and mapped to {@link LowStockItemDTO}.
     * The JPQL query is declared in {@link InventoryRepository#findAllLowStock()}.
     */
    @Transactional(readOnly = true)
    public List<LowStockItemDTO> getLowStockItems() {
        return inventoryRepository.findAllLowStock()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * Maps a live {@link Inventory} row (with its joined {@code Product})
     * to the lightweight {@link LowStockItemDTO} consumed by the frontend.
     */
    private LowStockItemDTO toDTO(Inventory inv) {
        var product  = inv.getProduct();
        var supplier = product.getSupplier();
        return new LowStockItemDTO(
                product.getId(),
                product.getProductName(),
                product.getSku(),
                product.getCategory(),
                inv.getStockQuantity(),
                inv.getReorderLevel(),
                inv.getUnit(),
                product.getSellingPrice() != null ? product.getSellingPrice().doubleValue() : 0.0,
                supplier != null ? supplier.getCompanyName() : null,
                supplier != null ? supplier.getEmail()       : null
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
