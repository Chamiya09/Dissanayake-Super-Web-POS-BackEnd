package com.dissayakesuper.web_pos_backend.inventory.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.dissayakesuper.web_pos_backend.inventory.dto.InventoryBulkImportResponse;
import com.dissayakesuper.web_pos_backend.inventory.dto.InventoryAnalyticsDTO;
import com.dissayakesuper.web_pos_backend.inventory.dto.InventoryImportError;
import com.dissayakesuper.web_pos_backend.inventory.dto.InventoryImportRequest;
import com.dissayakesuper.web_pos_backend.inventory.dto.InventoryImportSuccess;
import com.dissayakesuper.web_pos_backend.inventory.dto.InventoryStatusResponse;
import com.dissayakesuper.web_pos_backend.inventory.entity.Inventory;
import com.dissayakesuper.web_pos_backend.inventory.entity.InventoryLog;
import com.dissayakesuper.web_pos_backend.inventory.repository.InventoryLogRepository;
import com.dissayakesuper.web_pos_backend.inventory.repository.InventoryRepository;
import com.dissayakesuper.web_pos_backend.product.entity.Product;
import com.dissayakesuper.web_pos_backend.product.entity.ProductStatus;
import com.dissayakesuper.web_pos_backend.product.repository.ProductRepository;

@Service
@Transactional
public class InventoryService {

    private final InventoryRepository    inventoryRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final ProductRepository      productRepository;

    public InventoryService(InventoryRepository inventoryRepository,
                            InventoryLogRepository inventoryLogRepository,
                            ProductRepository productRepository) {
        this.inventoryRepository    = inventoryRepository;
        this.inventoryLogRepository = inventoryLogRepository;
        this.productRepository      = productRepository;
    }

    // ── GET ALL ───────────────────────────────────────────────────────────────

    /**
     * Returns all inventory records.
     * Each record is already linked to its Product via @OneToOne.
     */
    @Transactional(readOnly = true)
    public List<Inventory> getAllInventory() {
        return inventoryRepository.findAll();
    }

    // ── ANALYTICS ─────────────────────────────────────────────────────────────

    /**
     * Computes aggregated inventory metrics in a single pass over all records.
     * Uses Stream API — no extra JPQL query needed.
     */
    @Transactional(readOnly = true)
    public InventoryAnalyticsDTO getAnalytics() {
        List<Inventory> all = inventoryRepository.findAll();

        long tracked    = all.size();
        long lowStock   = all.stream().filter(i -> {
                if (isDiscontinued(i.getProduct())) return false;
                double qty = i.getStockQuantity() != null ? i.getStockQuantity() : 0.0;
                double reorder = i.getReorderLevel() != null ? i.getReorderLevel() : 0.0;
                return qty > 0 && qty <= reorder;
            }).count();
        long outOfStock = all.stream().filter(i -> {
                if (isDiscontinued(i.getProduct())) return false;
                double qty = i.getStockQuantity() != null ? i.getStockQuantity() : 0.0;
                return qty <= 0;
            }).count();
        double totalValue = all.stream()
                .mapToDouble(i -> {
                double qty   = i.getStockQuantity() != null ? i.getStockQuantity() : 0.0;
                double price = i.getProduct() != null && i.getProduct().getSellingPrice() != null
                    ? i.getProduct().getSellingPrice().doubleValue() : 0.0;
                    return qty * price;
                })
                .sum();

        return new InventoryAnalyticsDTO(tracked, lowStock, outOfStock, totalValue);
    }

    /**
     * Convenience method: returns all inventory records as flattened status DTOs
     * (Product Name, Category, Price + Stock, Unit, stockStatus).
     */
    @Transactional(readOnly = true)
    public List<InventoryStatusResponse> getInventoryStatus() {
        return inventoryRepository.findAll()
                .stream()
                .map(InventoryStatusResponse::from)
                .toList();
    }

    // ── LOW STOCK ─────────────────────────────────────────────────────────────

    /**
     * Returns only inventory records where stockQuantity &lt;= reorderLevel.
     */
    @Transactional(readOnly = true)
    public List<Inventory> getLowStockInventory() {
        return inventoryRepository.findAllLowStock();
    }

    // ── GET BY PRODUCT ────────────────────────────────────────────────────────

    /**
     * Fetches the inventory record for a specific product.
     * Throws 404 if the product or its inventory record does not exist.
     */
    @Transactional(readOnly = true)
    public Inventory getInventoryByProductId(Long productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No inventory record found for product id: " + productId));
    }

    // ── UPDATE STOCK ──────────────────────────────────────────────────────────

    /**
     * Adds {@code quantityToAdd} to the current stock of the given product.
     * <p>New = Current + Added</p>
     * If no inventory record exists yet one is automatically created with
     * default reorderLevel (10.0) and unit copied from the Product entity.
     *
     * @param productId   the target product
     * @param quantityToAdd amount to add (must be > 0)
     * @return the saved Inventory record
     */
    public Inventory updateStock(Long productId, Double quantityToAdd) {
        if (quantityToAdd == null || quantityToAdd <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "quantityToAdd must be greater than zero.");
        }

        Inventory inventory = inventoryRepository.findByProductId(productId)
                .orElseGet(() -> createInventoryForProduct(productId));

        double newQty = inventory.getStockQuantity() + quantityToAdd;
        inventory.setStockQuantity(newQty);
        inventory.setLastUpdated(LocalDateTime.now());

        Inventory saved = inventoryRepository.save(inventory);

        // Record the manual stock addition.
        inventoryLogRepository.save(InventoryLog.builder()
                .productId(saved.getProduct().getId())
                .productName(saved.getProduct().getProductName())
                .action("MANUAL_ADDITION")
                .quantityChanged(quantityToAdd)
                .stockAfter(newQty)
                .build());

        return saved;
    }

    // ── BULK IMPORT ──────────────────────────────────────────────────────────

    /**
     * Imports inventory stock and reorder values by SKU (typically from CSV rows).
     * Each row is validated and processed independently so one invalid row does not
     * block other valid rows.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public InventoryBulkImportResponse importInventory(List<InventoryImportRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CSV import list is empty.");
        }

        List<InventoryImportSuccess> importedItems = new ArrayList<>();
        List<InventoryImportError> errors = new ArrayList<>();
        Set<String> seenSkus = new HashSet<>();

        for (int i = 0; i < requests.size(); i++) {
            int rowNumber = i + 2; // row 1 is CSV header
            InventoryImportRequest request = requests.get(i);
            String skuForError = null;

            try {
                if (request == null) {
                    errors.add(new InventoryImportError(rowNumber, null, "Row payload is missing."));
                    continue;
                }

                String sku = normalizeRequired(request.sku());
                Double stockQuantity = request.stockQuantity();
                Double reorderLevel = request.reorderLevel();
                String unit = normalizeOptional(request.unit());

                skuForError = sku;

                List<String> validationErrors = validateBulkValues(sku, stockQuantity, reorderLevel, unit);
                if (!validationErrors.isEmpty()) {
                    errors.add(new InventoryImportError(rowNumber, sku, String.join(" ", validationErrors)));
                    continue;
                }

                String skuKey = sku.toLowerCase(Locale.ROOT);
                if (!seenSkus.add(skuKey)) {
                    errors.add(new InventoryImportError(rowNumber, sku, "Duplicate SKU in CSV file."));
                    continue;
                }

                Product product = productRepository.findBySkuAndIsActiveTrue(sku).orElse(null);
                if (product == null) {
                    errors.add(new InventoryImportError(rowNumber, sku, "SKU was not found in the database."));
                    continue;
                }

                Inventory inventory = inventoryRepository.findByProductId(product.getId())
                        .orElseGet(() -> Inventory.builder()
                                .product(product)
                                .stockQuantity(0.0)
                                .reorderLevel(10.0)
                                .unit(product.getUnit())
                                .build());

                double previousStock = inventory.getStockQuantity() != null
                        ? inventory.getStockQuantity()
                        : 0.0;

                inventory.setStockQuantity(stockQuantity);
                inventory.setReorderLevel(reorderLevel);
                if (unit != null) {
                    inventory.setUnit(unit);
                }
                inventory.setLastUpdated(LocalDateTime.now());

                Inventory saved = inventoryRepository.saveAndFlush(inventory);

                if (Double.compare(previousStock, stockQuantity) != 0) {
                    inventoryLogRepository.save(InventoryLog.builder()
                            .productId(product.getId())
                            .productName(product.getProductName())
                            .action("CSV_IMPORT")
                            .quantityChanged(stockQuantity - previousStock)
                            .stockAfter(stockQuantity)
                            .notes("Imported from CSV")
                            .build());
                }

                importedItems.add(toImportSuccess(saved));
            } catch (DataIntegrityViolationException ex) {
                errors.add(new InventoryImportError(
                        rowNumber,
                        skuForError,
                        "Could not import row because database constraints were violated."
                ));
            } catch (Exception ex) {
                errors.add(new InventoryImportError(
                        rowNumber,
                        skuForError,
                        "Unexpected import error: " + safeMessage(ex.getMessage())
                ));
            }
        }

        return new InventoryBulkImportResponse(
                requests.size(),
                importedItems.size(),
                errors.size(),
                importedItems,
                errors
        );
    }

    // ── ADJUST STOCK (positive or negative) ───────────────────────────────────

    /**
     * Adjusts the stock of an inventory record by the given amount.
     * Positive values increase stock, negative values decrease it.
     * The resulting stock must be >= 0.
     *
     * @param inventoryId      the target inventory record
     * @param adjustmentAmount positive or negative delta
     * @param notes            mandatory reason for the adjustment
     * @return the saved Inventory record
     */
    public Inventory adjustStock(Long inventoryId, Double adjustmentAmount, String notes) {
        if (adjustmentAmount == null || adjustmentAmount == 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Adjustment amount must not be zero.");
        }

        Inventory inventory = inventoryRepository.findByIdWithProduct(inventoryId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Inventory record not found with id: " + inventoryId));

        double newQty = inventory.getStockQuantity() + adjustmentAmount;
        if (newQty < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Adjustment would result in negative stock ("
                            + newQty + "). Current stock is "
                            + inventory.getStockQuantity() + ".");
        }

        inventory.setStockQuantity(newQty);
        inventory.setLastUpdated(LocalDateTime.now());
        Inventory saved = inventoryRepository.save(inventory);

        inventoryLogRepository.save(InventoryLog.builder()
                .productId(saved.getProduct().getId())
                .productName(saved.getProduct().getProductName())
                .action("ADJUSTMENT")
                .quantityChanged(adjustmentAmount)
                .stockAfter(newQty)
                .notes(notes)
                .build());

        return saved;
    }

    // ── DELETE INVENTORY RECORD ───────────────────────────────────────────────

    /**
     * Removes the inventory tracking record for the given inventory id.
     * The underlying Product is NOT deleted.
     */
    public void deleteInventory(Long inventoryId) {
        if (!inventoryRepository.existsById(inventoryId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Inventory record not found with id: " + inventoryId);
        }
        inventoryRepository.deleteById(inventoryId);
    }

    // ── EDIT INVENTORY RECORD ─────────────────────────────────────────────────

    /**
     * Updates the {@code reorderLevel} and/or {@code unit} of an inventory record.
     * Null fields are ignored (partial update semantics).
     */
    public Inventory editInventory(Long inventoryId,
                                   com.dissayakesuper.web_pos_backend.inventory.dto.EditInventoryRequest request) {
        // JOIN FETCH ensures the product is initialized before the transaction closes,
        // preventing LazyInitializationException when the controller maps to a response DTO.
        Inventory inv = inventoryRepository.findByIdWithProduct(inventoryId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Inventory record not found with id: " + inventoryId));

        if (request.reorderLevel() != null) inv.setReorderLevel(request.reorderLevel());
        if (request.unit()         != null) inv.setUnit(request.unit());

        // ── Stock increment: new_stock = existing_stock + quantityToAdd ──────
        if (request.quantityToAdd() != null && request.quantityToAdd() > 0) {
            double newQty = inv.getStockQuantity() + request.quantityToAdd();
            inv.setStockQuantity(newQty);
            inv.setLastUpdated(LocalDateTime.now());

            Inventory saved = inventoryRepository.save(inv);

            inventoryLogRepository.save(InventoryLog.builder()
                    .productId(saved.getProduct().getId())
                    .productName(saved.getProduct().getProductName())
                    .action("MANUAL_ADDITION")
                    .quantityChanged(request.quantityToAdd())
                    .stockAfter(newQty)
                    .build());

            return saved;
        }

        return inventoryRepository.save(inv);
    }

    // Stock history

    /**
     * Returns the full stock-change history for a product, newest first.
     */
    @Transactional(readOnly = true)
    public List<InventoryLog> getLogsByProductId(Long productId) {
        return inventoryLogRepository.findByProductIdOrderByTimestampDesc(productId);
    }

    /**
     * Returns all stock-change log entries across all products, newest first.
     */
    @Transactional(readOnly = true)
    public List<InventoryLog> getAllLogs() {
        return inventoryLogRepository.findAllByOrderByTimestampDesc();
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────

    /**
     * Creates a brand-new Inventory record for a product that has none yet.
     * Inherits {@code unit} from the Product; stockQuantity starts at 0.0.
     */
    private Inventory createInventoryForProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product not found with id: " + productId));

        return Inventory.builder()
                .product(product)
                .stockQuantity(0.0)
                .reorderLevel(10.0)
                .unit(product.getUnit())
                .build();
    }

    private List<String> validateBulkValues(
            String sku,
            Double stockQuantity,
            Double reorderLevel,
            String unit
    ) {
        List<String> errors = new ArrayList<>();

        if (sku == null || sku.isEmpty()) {
            errors.add("SKU / ProductID is required.");
        } else if (sku.length() > 100) {
            errors.add("SKU must be 100 characters or fewer.");
        }

        if (stockQuantity == null) {
            errors.add("Stock quantity is required.");
        } else if (stockQuantity < 0) {
            errors.add("Stock quantity must be 0 or greater.");
        }

        if (reorderLevel == null) {
            errors.add("Reorder level is required.");
        } else if (reorderLevel < 0) {
            errors.add("Reorder level must be 0 or greater.");
        }

        if (unit != null && unit.length() > 20) {
            errors.add("Unit must be 20 characters or fewer.");
        }

        return errors;
    }

    private static String normalizeRequired(String value) {
        return value == null ? null : value.trim();
    }

    private static String normalizeOptional(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static InventoryImportSuccess toImportSuccess(Inventory saved) {
        return new InventoryImportSuccess(
                saved.getId(),
                saved.getProduct().getId(),
                saved.getProduct().getProductName(),
                saved.getProduct().getSku(),
                saved.getStockQuantity(),
                saved.getReorderLevel(),
                saved.getUnit()
        );
    }

    private static String safeMessage(String message) {
        return message == null || message.isBlank() ? "No details available." : message;
    }

    private static boolean isDiscontinued(Product product) {
        return product != null && product.getStatus() == ProductStatus.DISCONTINUED;
    }
}
