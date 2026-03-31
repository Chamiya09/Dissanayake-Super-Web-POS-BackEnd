package com.dissayakesuper.web_pos_backend.sale.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.dissayakesuper.web_pos_backend.inventory.entity.Inventory;
import com.dissayakesuper.web_pos_backend.inventory.entity.InventoryLog;
import com.dissayakesuper.web_pos_backend.inventory.repository.InventoryLogRepository;
import com.dissayakesuper.web_pos_backend.inventory.repository.InventoryRepository;
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

    private final SaleRepository        saleRepository;
    private final InventoryRepository   inventoryRepository;
    private final InventoryLogRepository inventoryLogRepository;

    public SaleService(SaleRepository saleRepository,
                       InventoryRepository inventoryRepository,
                       InventoryLogRepository inventoryLogRepository) {
        this.saleRepository       = saleRepository;
        this.inventoryRepository  = inventoryRepository;
        this.inventoryLogRepository = inventoryLogRepository;
    }

    // ── CREATE ────────────────────────────────────────────────────────────────

    public Sale createSale(Sale sale) {
        if (saleRepository.existsByReceiptNo(sale.getReceiptNo())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A sale with receipt number '" + sale.getReceiptNo() + "' already exists.");
        }

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

            // ── Audit log: record the stock deduction caused by this sale ─────
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
                    return new SaleReturnItemRequest(item.getId(), remainingQty, null);
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

            Optional<Inventory> inventoryOpt = (saleItem.getProductId() != null
                    ? inventoryRepository.findByProductId(saleItem.getProductId())
                    : inventoryRepository.findByProductProductName(saleItem.getProductName()));

            if (inventoryOpt.isEmpty()) {
                continue;
            }

            Inventory inventory = inventoryOpt.get();
                var returnQtyInStockUnit = convertToInventoryUnit(
                    returnItem.quantity(),
                    returnItem.unit(),
                    inventory.getUnit(),
                    saleItem.getProductName());

                if (returnQtyInStockUnit.compareTo(remainingQty) > 0) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Return quantity for '" + saleItem.getProductName() + "' exceeds available returnable quantity after unit conversion. Available: "
                        + remainingQty + " " + safeUnit(inventory.getUnit()) + ", requested: "
                        + returnItem.quantity() + " " + safeUnit(returnItem.unit()));
                }

                double returnedQty = returnQtyInStockUnit.doubleValue();
            double restoredStock = inventory.getStockQuantity() + returnedQty;
            inventory.setStockQuantity(restoredStock);
            inventoryRepository.save(inventory);

                saleItem.setReturnedQuantity(getReturnedQty(saleItem).add(returnQtyInStockUnit));

            inventoryLogRepository.save(InventoryLog.builder()
                    .productId(inventory.getProduct().getId())
                    .productName(inventory.getProduct().getProductName())
                    .action("RESTOCK_RETURNED_SALE")
                    .quantityChanged(returnedQty)
                    .stockAfter(restoredStock)
                        .notes("Restocked from returned sale ID: " + saleId + ", sale item ID: " + saleItem.getId() +
                            ", input: " + returnItem.quantity() + " " + safeUnit(returnItem.unit()) +
                            ", stock unit: " + safeUnit(inventory.getUnit()))
                    .build());
        }

        boolean allReturned = sale.getItems().stream().allMatch(item -> remainingQty(item).signum() == 0);
        boolean anyReturned = sale.getItems().stream().anyMatch(item -> getReturnedQty(item).signum() > 0);

        if (allReturned) {
            sale.setStatus("Returned");
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

    private java.math.BigDecimal convertToInventoryUnit(
            java.math.BigDecimal quantity,
            String inputUnit,
            String inventoryUnit,
            String productName) {
        String source = normalizeUnit(inputUnit);
        String target = normalizeUnit(inventoryUnit);

        if (source == null || source.isBlank()) {
            return quantity;
        }

        if (target == null || target.isBlank()) {
            if (!source.equals(target)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Cannot convert return unit for '" + productName + "' because inventory unit is not configured.");
            }
            return quantity;
        }

        if (source.equals(target)) {
            return quantity;
        }

        if (isWeightUnit(source) && isWeightUnit(target)) {
            var grams = toGrams(quantity, source);
            return fromGrams(grams, target);
        }

        if (isVolumeUnit(source) && isVolumeUnit(target)) {
            var milliliters = toMilliliters(quantity, source);
            return fromMilliliters(milliliters, target);
        }

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Incompatible return unit conversion for '" + productName + "'. Cannot convert " + inputUnit + " to " + inventoryUnit + ".");
    }

    private String normalizeUnit(String unit) {
        if (unit == null) return null;
        String normalized = unit.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) return null;

        return switch (normalized) {
            case "kg", "kilogram", "kilograms", "kilo", "kilos" -> "kg";
            case "g", "gram", "grams" -> "g";
            case "l", "lt", "liter", "liters", "litre", "litres" -> "l";
            case "ml", "milliliter", "milliliters", "millilitre", "millilitres" -> "ml";
            case "pcs", "pc", "piece", "pieces" -> "pcs";
            default -> normalized;
        };
    }

    private boolean isWeightUnit(String unit) {
        return "kg".equals(unit) || "g".equals(unit);
    }

    private boolean isVolumeUnit(String unit) {
        return "l".equals(unit) || "ml".equals(unit);
    }

    private java.math.BigDecimal toGrams(java.math.BigDecimal qty, String unit) {
        if ("kg".equals(unit)) {
            return qty.multiply(java.math.BigDecimal.valueOf(1000));
        }
        return qty;
    }

    private java.math.BigDecimal fromGrams(java.math.BigDecimal grams, String targetUnit) {
        if ("kg".equals(targetUnit)) {
            return grams.divide(java.math.BigDecimal.valueOf(1000), 3, java.math.RoundingMode.HALF_UP);
        }
        return grams;
    }

    private java.math.BigDecimal toMilliliters(java.math.BigDecimal qty, String unit) {
        if ("l".equals(unit)) {
            return qty.multiply(java.math.BigDecimal.valueOf(1000));
        }
        return qty;
    }

    private java.math.BigDecimal fromMilliliters(java.math.BigDecimal milliliters, String targetUnit) {
        if ("l".equals(targetUnit)) {
            return milliliters.divide(java.math.BigDecimal.valueOf(1000), 3, java.math.RoundingMode.HALF_UP);
        }
        return milliliters;
    }

    private String safeUnit(String unit) {
        return unit == null || unit.isBlank() ? "(default)" : unit;
    }
}
