package com.dissayakesuper.web_pos_backend.reorder.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.dissayakesuper.web_pos_backend.inventory.entity.Inventory;
import com.dissayakesuper.web_pos_backend.inventory.repository.InventoryRepository;
import com.dissayakesuper.web_pos_backend.reorder.dto.LowStockItemDTO;
import com.dissayakesuper.web_pos_backend.reorder.dto.ReorderItemRequestDTO;
import com.dissayakesuper.web_pos_backend.reorder.dto.ReorderRequestDTO;
import com.dissayakesuper.web_pos_backend.reorder.dto.ReorderResponseDTO;
import com.dissayakesuper.web_pos_backend.reorder.dto.ReorderUpdateDTO;
import com.dissayakesuper.web_pos_backend.reorder.entity.Reorder;
import com.dissayakesuper.web_pos_backend.reorder.entity.ReorderItem;
import com.dissayakesuper.web_pos_backend.reorder.entity.Status;
import com.dissayakesuper.web_pos_backend.reorder.repository.ReorderRepository;
import com.dissayakesuper.web_pos_backend.supplier.repository.SupplierRepository;

import java.time.LocalDateTime;
import java.util.UUID;

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
            .supplierAcceptToken(newAcceptToken())
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
            total += itemDTO.quantity().doubleValue() * itemDTO.unitPrice();
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
            managerName,
            saved.getSupplierAcceptToken(),
            false
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

    // ── UPDATE ORDER ──────────────────────────────────────────────────────────

    /**
     * Partially updates an existing purchase order.
     * <ul>
     *   <li>If {@code dto.supplierEmail()} is non-null/non-blank, updates the email.</li>
     *   <li>If {@code dto.items()} is non-null and non-empty, replaces all line items
     *       and recalculates {@code totalAmount}.</li>
     * </ul>
     * Terminal-state orders (CANCELLED / RECEIVED) cannot be edited.
     *
     * @throws ResponseStatusException 404 if order not found
     * @throws ResponseStatusException 409 if order is in a terminal state
     */
    public ReorderResponseDTO updateOrder(Long id, ReorderUpdateDTO dto, String managerName) {
        Reorder reorder = findOrThrow(id);

        if (reorder.getStatus() == Status.CANCELLED
            || reorder.getStatus() == Status.RECEIVED
            || reorder.getStatus() == Status.CONFIRMED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Order '" + reorder.getOrderRef() + "' is " + reorder.getStatus()
                    + " and cannot be edited or resent.");
        }

        // ── Update supplierEmail ──────────────────────────────────────────────
        if (dto.supplierEmail() != null && !dto.supplierEmail().isBlank()) {
            reorder.setSupplierEmail(dto.supplierEmail());
        }

        // ── Replace items and recalculate total ───────────────────────────────
        if (dto.items() != null && !dto.items().isEmpty()) {
            // orphanRemoval = true on the collection will DELETE the old rows on flush
            List<ReorderItem> oldItems = new ArrayList<>(reorder.getItems());
            oldItems.forEach(reorder::removeItem);

            double total = 0.0;
            for (ReorderItemRequestDTO itemDTO : dto.items()) {
                ReorderItem item = ReorderItem.builder()
                        .productName(itemDTO.productName())
                        .productId(itemDTO.productId())
                        .quantity(itemDTO.quantity())
                        .unitPrice(itemDTO.unitPrice())
                        .build();
                reorder.addItem(item);
                total += itemDTO.quantity().doubleValue() * itemDTO.unitPrice();
            }
            reorder.setTotalAmount(total);
        }

        reorder.setSupplierAcceptToken(newAcceptToken());
        reorder.setAcceptedAt(null);

        Reorder saved = reorderRepository.save(reorder);

        String supplierName = supplierRepository.findByEmail(saved.getSupplierEmail())
                .map(s -> s.getCompanyName())
                .orElse(null);

        List<ReorderItemRequestDTO> itemRequestDTOs = saved.getItems().stream()
                .map(i -> new ReorderItemRequestDTO(
                        i.getProductName(),
                        i.getProductId(),
                    i.getQuantity(),
                        i.getUnitPrice()
                ))
                .toList();

        emailService.sendSupplierPO(
                saved.getSupplierEmail(),
                supplierName,
                saved.getOrderRef(),
                itemRequestDTOs,
                saved.getTotalAmount(),
            managerName,
                saved.getSupplierAcceptToken(),
                true
        );

        return ReorderResponseDTO.from(saved);
    }

    public ReorderResponseDTO acceptOrderByToken(String token) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing accept token.");
        }

        Reorder reorder = reorderRepository.findBySupplierAcceptToken(token)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Invalid or expired acceptance link."));

        if (reorder.getStatus() == Status.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order is cancelled and cannot be accepted.");
        }

        if (reorder.getStatus() == Status.RECEIVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order is already received.");
        }

        if (reorder.getStatus() == Status.CONFIRMED) {
            return ReorderResponseDTO.from(reorder);
        }

        reorder.setStatus(Status.CONFIRMED);
        reorder.setAcceptedAt(LocalDateTime.now());
        reorder.setSupplierAcceptToken(null);
        Reorder saved = reorderRepository.save(reorder);

        String confirmedAt = saved.getAcceptedAt() != null
            ? saved.getAcceptedAt().toString()
            : LocalDateTime.now().toString();
        emailService.sendSupplierConfirmationDoneMail(
            saved.getOrderRef(),
            saved.getSupplierEmail(),
            saved.getTotalAmount(),
            confirmedAt
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
        Reorder saved = reorderRepository.save(reorder);
        return ReorderResponseDTO.from(saved);
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

    private String newAcceptToken() {
        return UUID.randomUUID().toString();
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
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "Order '" + orderRef + "' is already " + current
                            + " and cannot be transitioned to " + requested + ".");
        }

        // PENDING can only move to CONFIRMED or CANCELLED
        if (current == Status.PENDING
                && requested != Status.CONFIRMED
                && requested != Status.CANCELLED) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "Order '" + orderRef + "' is PENDING; allowed transitions: CONFIRMED, CANCELLED.");
        }

        // CONFIRMED can only move to RECEIVED or CANCELLED
        if (current == Status.CONFIRMED
                && requested != Status.RECEIVED
                && requested != Status.CANCELLED) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "Order '" + orderRef + "' is CONFIRMED; allowed transitions: RECEIVED, CANCELLED.");
        }
    }
}
