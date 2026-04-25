package com.dissayakesuper.web_pos_backend.sale.service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.dissayakesuper.web_pos_backend.inventory.entity.Inventory;
import com.dissayakesuper.web_pos_backend.inventory.entity.InventoryLog;
import com.dissayakesuper.web_pos_backend.inventory.repository.InventoryLogRepository;
import com.dissayakesuper.web_pos_backend.inventory.repository.InventoryRepository;
import com.dissayakesuper.web_pos_backend.product.entity.Product;
import com.dissayakesuper.web_pos_backend.product.repository.ProductRepository;
import com.dissayakesuper.web_pos_backend.sale.dto.SaleItemRequest;
import com.dissayakesuper.web_pos_backend.sale.dto.SaleReturnItemRequest;
import com.dissayakesuper.web_pos_backend.sale.dto.SaleReturnRequest;
import com.dissayakesuper.web_pos_backend.sale.dto.SaleUpdateRequest;
import com.dissayakesuper.web_pos_backend.sale.entity.Sale;
import com.dissayakesuper.web_pos_backend.sale.entity.SaleItem;
import com.dissayakesuper.web_pos_backend.sale.repository.SaleRepository;

@Service
@Transactional
public class SaleService {

    private static final DateTimeFormatter EXPORT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter EXPORT_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final List<String> ML_EXPORT_HEADERS = List.of(
            "Date",
            "Time",
            "TransactionID",
            "ProductID",
            "ProductName",
            "Category",
            "PricingUnit",
            "Quantity",
            "UnitPrice",
            "BuyingPrice",
            "SellingPrice",
            "Total_LKR"
    );

    private final SaleRepository        saleRepository;
    private final InventoryRepository   inventoryRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final ProductRepository productRepository;
    private final TransactionIdService transactionIdService;

    public SaleService(SaleRepository saleRepository,
                       InventoryRepository inventoryRepository,
                       InventoryLogRepository inventoryLogRepository,
                       ProductRepository productRepository,
                       TransactionIdService transactionIdService) {
        this.saleRepository       = saleRepository;
        this.inventoryRepository  = inventoryRepository;
        this.inventoryLogRepository = inventoryLogRepository;
        this.productRepository = productRepository;
        this.transactionIdService = transactionIdService;
    }

    // ── CREATE ────────────────────────────────────────────────────────────────

    public Sale createSale(Sale sale) {
        // Transaction IDs are generated server-side to guarantee sequential continuity.
        sale.setReceiptNo(transactionIdService.nextTransactionId());

        // ── Deduct stock for every item in this sale ──────────────────────────
        for (SaleItem item : sale.getItems()) {
            item.setSale(sale);

            // Look up inventory by product_id (preferred) or fall back to product name
            Inventory inventory = (item.getProductId() != null
                    ? inventoryRepository.findByProductId(item.getProductId())
                    : inventoryRepository.findByProductProductName(item.getProductName()))
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "No inventory record found for product: '" + item.getProductName() + "'"));

            double soldQty = item.getQuantity().doubleValue();
            if (inventory.getStockQuantity() < soldQty) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Insufficient stock for '" + item.getProductName() +
                        "'. Available: " + inventory.getStockQuantity() +
                        ", requested: " + soldQty);
            }

            double newQty = inventory.getStockQuantity() - soldQty;
            inventory.setStockQuantity(newQty);
            inventoryRepository.save(inventory);

            // Record the stock deduction caused by this sale.
            inventoryLogRepository.save(InventoryLog.builder()
                    .productId(inventory.getProduct().getId())
                    .productName(inventory.getProduct().getProductName())
                    .action("SALE_REDUCTION")
                    .quantityChanged(-soldQty)
                    .stockAfter(newQty)
                    .build());
        }

        return saleRepository.save(sale);
    }

    // ── READ ALL ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Sale> getAllSales() {
        return saleRepository.findAll();
    }

    @Transactional(readOnly = true)
    public byte[] exportSalesForMlCsv() {
        List<Sale> sales = saleRepository.findAllByOrderBySaleDateAscIdAsc();

        Set<Long> productIds = sales.stream()
                .flatMap(sale -> sale.getItems().stream())
                .map(SaleItem::getProductId)
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toSet());

        Map<Long, Product> productsById = productIds.isEmpty()
                ? Map.of()
                : productRepository.findAllById(productIds).stream()
                    .collect(Collectors.toMap(Product::getId, product -> product));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes((String.join(",", ML_EXPORT_HEADERS) + "\n").getBytes(StandardCharsets.UTF_8));

        for (Sale sale : sales) {
            if (sale.getSaleDate() == null || sale.getItems() == null || sale.getItems().isEmpty()) {
                continue;
            }

            String date = sale.getSaleDate().format(EXPORT_DATE_FORMAT);
            String time = sale.getSaleDate().format(EXPORT_TIME_FORMAT);
            String transactionId = safe(sale.getReceiptNo());

            for (SaleItem item : sale.getItems()) {
                Product product = item.getProductId() == null ? null : productsById.get(item.getProductId());

                String productId = product != null
                        ? safe(product.getSku())
                        : (item.getProductId() == null ? "" : String.valueOf(item.getProductId()));
                String productName = product != null
                        ? safe(product.getProductName())
                        : safe(item.getProductName());
                String category = product != null ? safe(product.getCategory()) : "";
                String pricingUnit = product != null ? safe(product.getUnit()) : "unit";

                String quantity = toQuantityString(item.getQuantity());

                int unitPrice = toIntegerAmount(item.getUnitPrice());
                int buyingPrice = product != null ? toIntegerAmount(product.getBuyingPrice()) : unitPrice;
                int sellingPrice = product != null ? toIntegerAmount(product.getSellingPrice()) : unitPrice;
                int totalLkr = toIntegerAmount(item.getLineTotal());

                List<String> row = List.of(
                        date,
                        time,
                        transactionId,
                        productId,
                        productName,
                        category,
                        pricingUnit,
                        quantity,
                        String.valueOf(unitPrice),
                        String.valueOf(buyingPrice),
                        String.valueOf(sellingPrice),
                        String.valueOf(totalLkr)
                );

                out.writeBytes((row.stream().map(this::escapeCsv).collect(Collectors.joining(",")) + "\n")
                        .getBytes(StandardCharsets.UTF_8));
            }
        }

        return out.toByteArray();
    }

    // ── READ ONE ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Sale getSaleById(Long id) {
        return saleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Sale not found with id: " + id));
    }

    // ── UPDATE SALE (reverse + re-adjust) ──────────────────────────────────────

    /**
     * Atomically updates a sale using a "Reverse and Re-adjust" strategy:
     * <ol>
     *   <li><b>Step 1 — Reverse Inventory</b>: add each old item's quantity back to stock.</li>
     *   <li><b>Step 2 — Update Sale</b>: apply the new paymentMethod and totalAmount.</li>
     *   <li><b>Step 3 — Apply New Inventory</b>: replace items and deduct new quantities.
     *       Throws 400 if any product has insufficient stock (rolls back everything).</li>
     * </ol>
     * Voided sales cannot be edited (throws 409).
     */
    public Sale updateSale(Long saleId, SaleUpdateRequest request) {

        // ── Load ──────────────────────────────────────────────────────────────
        Sale sale = getSaleById(saleId);

        if ("Voided".equalsIgnoreCase(sale.getStatus()) ||
            "Returned".equalsIgnoreCase(sale.getStatus()) ||
            "Partially Returned".equalsIgnoreCase(sale.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Sale " + sale.getReceiptNo() + " is " + sale.getStatus() + " and cannot be edited.");
        }

        // ── Step 1: Reverse old inventory deductions ──────────────────────────
        // Snapshot the list before clearing so we iterate the original items safely.
        List<SaleItem> oldItems = new ArrayList<>(sale.getItems());
        for (SaleItem oldItem : oldItems) {
            if (oldItem.getProductId() == null) continue;

            inventoryRepository.findByProductId(oldItem.getProductId()).ifPresent(inv -> {
                double returnedQty  = oldItem.getQuantity().doubleValue();
                double restoredStock = inv.getStockQuantity() + returnedQty;
                inv.setStockQuantity(restoredStock);
                inventoryRepository.save(inv);

                inventoryLogRepository.save(InventoryLog.builder()
                        .productId(inv.getProduct().getId())
                        .productName(inv.getProduct().getProductName())
                        .action("SALE_UPDATE_REVERSAL")
                        .quantityChanged(returnedQty)
                        .stockAfter(restoredStock)
                        .build());
            });
        }

        // ── Step 2: Update top-level sale fields ──────────────────────────────
        sale.setPaymentMethod(request.paymentMethod());

        // ── Step 3: Replace items, recalculate subtotals, and deduct new quantities ──
        // Clearing with orphanRemoval=true schedules DELETE for all old SaleItem rows.
        sale.getItems().clear();

        double grandTotal = 0.0;

        for (SaleItemRequest itemReq : request.items()) {
            // Validate unit price is positive
            if (itemReq.unitPrice() <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Unit price for '" + itemReq.productName() + "' must be a positive value.");
            }

            Inventory inv = (itemReq.productId() != null
                    ? inventoryRepository.findByProductId(itemReq.productId())
                    : inventoryRepository.findByProductProductName(itemReq.productName()))
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "No inventory record found for product: '" + itemReq.productName() + "'"));

            double neededQty = itemReq.quantity().doubleValue();
            if (inv.getStockQuantity() < neededQty) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Insufficient stock for '" + itemReq.productName() +
                        "'. Available: " + inv.getStockQuantity() +
                        ", requested: " + neededQty);
            }

            double newStock = inv.getStockQuantity() - neededQty;
            inv.setStockQuantity(newStock);
            inventoryRepository.save(inv);

            // Server-side recalculation: subtotal = quantity × unit_price
            double recalculatedLineTotal = itemReq.quantity().doubleValue() * itemReq.unitPrice();

            SaleItem newItem = new SaleItem(
                    itemReq.productName(),
                    itemReq.quantity(),
                    itemReq.unitPrice(),
                    recalculatedLineTotal);
            newItem.setProductId(itemReq.productId());
            sale.addItem(newItem);

            grandTotal += recalculatedLineTotal;

            inventoryLogRepository.save(InventoryLog.builder()
                    .productId(inv.getProduct().getId())
                    .productName(inv.getProduct().getProductName())
                    .action("SALE_UPDATE_REDUCTION")
                    .quantityChanged(-neededQty)
                    .stockAfter(newStock)
                    .build());
        }

        // Grand total update: sum of all recalculated subtotals (overrides frontend value)
        sale.setTotalAmount(grandTotal);

        return saleRepository.save(sale);
    }

    // ── VOID / UPDATE STATUS ──────────────────────────────────────────────────

    public Sale updateSaleStatus(Long id, String newStatus) {
        Sale sale = getSaleById(id);

        if ("Voided".equalsIgnoreCase(sale.getStatus()) ||
                "Returned".equalsIgnoreCase(sale.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Sale " + sale.getReceiptNo() + " is already " + sale.getStatus() + " and cannot be modified.");
        }

        sale.setStatus(newStatus);
        return saleRepository.save(sale);
    }

    // ── RETURN SALE (restock inventory) ──────────────────────────────────────

    /**
     * Processes a sales return:
     * <ol>
     *   <li>Validates the sale exists and its status is "Completed".</li>
     *   <li>For every {@link SaleItem}, restores the sold quantity back to stock.</li>
     *   <li>Writes a {@link InventoryLog} entry (action = RESTOCK_RETURNED_SALE) per item.</li>
     *   <li>Marks the sale status as "Returned" and persists it.</li>
     * </ol>
     *
     * @param saleId the ID of the sale to return
     * @return the updated {@link Sale} with status "Returned"
     * @throws ResponseStatusException 404 if sale not found, 409 if already Voided/Returned
     */
    @Transactional
    public Sale returnSale(Long saleId) {
        Sale sale = getSaleById(saleId);
        List<SaleReturnItemRequest> fullReturnItems = sale.getItems().stream()
                .map(item -> {
                    var remainingQty = remainingQty(item);
                    if (remainingQty.signum() <= 0) {
                        return null;
                    }
                    return new SaleReturnItemRequest(item.getId(), remainingQty);
                })
                .filter(item -> item != null)
                .toList();

        return returnSelectedItems(saleId, new SaleReturnRequest(fullReturnItems));
    }

    @Transactional
    public Sale returnSelectedItems(Long saleId, SaleReturnRequest request) {
        Sale sale = getSaleById(saleId);

        String currentStatus = sale.getStatus();
        if ("Voided".equalsIgnoreCase(currentStatus) || "Returned".equalsIgnoreCase(currentStatus)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Sale " + sale.getReceiptNo() + " is already " + currentStatus + " and cannot be returned.");
        }

        Map<Long, SaleItem> itemsById = new HashMap<>();
        for (SaleItem item : sale.getItems()) {
            itemsById.put(item.getId(), item);
        }

        for (SaleReturnItemRequest returnItem : request.items()) {
            SaleItem saleItem = itemsById.get(returnItem.saleItemId());
            if (saleItem == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Sale item " + returnItem.saleItemId() + " does not belong to sale " + sale.getReceiptNo() + ".");
            }

            var remainingQty = remainingQty(saleItem);
            if (returnItem.quantity().compareTo(remainingQty) > 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Return quantity for '" + saleItem.getProductName() + "' exceeds available returnable quantity. Available: "
                                + remainingQty + ", requested: " + returnItem.quantity());
            }

            saleItem.setReturnedQuantity(getReturnedQty(saleItem).add(returnItem.quantity()));

            Optional<Inventory> inventoryOpt = (saleItem.getProductId() != null
                    ? inventoryRepository.findByProductId(saleItem.getProductId())
                    : inventoryRepository.findByProductProductName(saleItem.getProductName()));

            if (inventoryOpt.isEmpty()) {
                continue;
            }

            Inventory inventory = inventoryOpt.get();
            if (isDiscreteUnit(inventory.getUnit()) && !isWholeNumber(returnItem.quantity())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Return quantity for '" + saleItem.getProductName() + "' must be a whole number for unit '" + safeUnit(inventory.getUnit()) + "'.");
            }

            double returnedQty = returnItem.quantity().doubleValue();
            double restoredStock = inventory.getStockQuantity() + returnedQty;
            inventory.setStockQuantity(restoredStock);
            inventoryRepository.save(inventory);

            inventoryLogRepository.save(InventoryLog.builder()
                    .productId(inventory.getProduct().getId())
                    .productName(inventory.getProduct().getProductName())
                    .action("RESTOCK_RETURNED_SALE")
                    .quantityChanged(returnedQty)
                    .stockAfter(restoredStock)
                    .notes("Restocked from returned sale ID: " + saleId + ", sale item ID: " + saleItem.getId())
                    .build());
        }

        boolean allReturned = sale.getItems().stream().allMatch(item -> remainingQty(item).signum() == 0);
        boolean anyReturned = sale.getItems().stream().anyMatch(item -> getReturnedQty(item).signum() > 0);

        BigDecimal recalculatedTotal = sale.getItems().stream()
                .map(item -> remainingQty(item).multiply(BigDecimal.valueOf(item.getUnitPrice())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        if (recalculatedTotal.signum() < 0) {
            recalculatedTotal = BigDecimal.ZERO;
        }
        sale.setTotalAmount(recalculatedTotal.doubleValue());

        if (allReturned) {
            sale.setStatus("Returned");
            sale.setTotalAmount(0.0);
        } else if (anyReturned) {
            sale.setStatus("Partially Returned");
        }

        return saleRepository.save(sale);
    }

    private java.math.BigDecimal getReturnedQty(SaleItem item) {
        return item.getReturnedQuantity() == null ? java.math.BigDecimal.ZERO : item.getReturnedQuantity();
    }

    private java.math.BigDecimal remainingQty(SaleItem item) {
        var remainingQty = item.getQuantity().subtract(getReturnedQty(item));
        return remainingQty.signum() < 0 ? java.math.BigDecimal.ZERO : remainingQty;
    }

    private String normalizeUnit(String unit) {
        if (unit == null) return null;
        String normalized = unit.trim().toLowerCase(java.util.Locale.ROOT);
        if (normalized.isEmpty()) return null;

        return switch (normalized) {
            case "pcs", "pc", "piece", "pieces" -> "pcs";
            case "packet", "packets", "pack", "packs" -> "packet";
            default -> normalized;
        };
    }

    private boolean isDiscreteUnit(String unit) {
        String normalized = normalizeUnit(unit);
        return "pcs".equals(normalized)
                || "packet".equals(normalized)
                || "bottle".equals(normalized)
                || "box".equals(normalized)
                || "item".equals(normalized)
                || "unit".equals(normalized);
    }

    private boolean isWholeNumber(java.math.BigDecimal qty) {
        return qty.stripTrailingZeros().scale() <= 0;
    }

    private String safeUnit(String unit) {
        return unit == null || unit.isBlank() ? "(default)" : unit;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private int toIntegerAmount(Number value) {
        if (value == null) {
            return 0;
        }
        return (int) Math.round(value.doubleValue());
    }

    private int toIntegerAmount(BigDecimal value) {
        if (value == null) {
            return 0;
        }
        return value.setScale(0, RoundingMode.HALF_UP).intValue();
    }

    private String toQuantityString(BigDecimal quantity) {
        if (quantity == null) {
            return "0";
        }
        return quantity.stripTrailingZeros().toPlainString();
    }

    private String escapeCsv(String value) {
        String normalized = value == null ? "" : value;
        if (normalized.contains(",") || normalized.contains("\"") || normalized.contains("\n")) {
            return "\"" + normalized.replace("\"", "\"\"") + "\"";
        }
        return normalized;
    }
}
