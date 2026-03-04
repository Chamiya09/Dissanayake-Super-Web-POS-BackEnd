package com.dissayakesuper.web_pos_backend.inventory.service;

import com.dissayakesuper.web_pos_backend.inventory.dto.InventoryAnalyticsDTO;
import com.dissayakesuper.web_pos_backend.inventory.dto.InventoryStatusResponse;
import com.dissayakesuper.web_pos_backend.inventory.entity.Inventory;
import com.dissayakesuper.web_pos_backend.inventory.entity.InventoryLog;
import com.dissayakesuper.web_pos_backend.inventory.repository.InventoryLogRepository;
import com.dissayakesuper.web_pos_backend.inventory.repository.InventoryRepository;
import com.dissayakesuper.web_pos_backend.product.entity.Product;
import com.dissayakesuper.web_pos_backend.product.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

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
        long lowStock   = all.stream().filter(i -> i.getStockQuantity() > 0
                                                   && i.getStockQuantity() <= i.getReorderLevel()).count();
        long outOfStock = all.stream().filter(i -> i.getStockQuantity() <= 0).count();
        double totalValue = all.stream()
                .mapToDouble(i -> {
                    double qty   = i.getStockQuantity();
                    double price = i.getProduct().getSellingPrice() != null
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
     * Returns only inventory records where stockQuantity &lt; reorderLevel.
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

        // ── Audit log ──────────────────────────────────────────────────────
        inventoryLogRepository.save(InventoryLog.builder()
                .productId(saved.getProduct().getId())
                .productName(saved.getProduct().getProductName())
                .quantityChanged(quantityToAdd)
                .stockAfter(newQty)
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
        Inventory inv = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Inventory record not found with id: " + inventoryId));

        if (request.reorderLevel() != null) inv.setReorderLevel(request.reorderLevel());
        if (request.unit()         != null) inv.setUnit(request.unit());

        return inventoryRepository.save(inv);
    }

    // ── AUDIT LOG ─────────────────────────────────────────────────────────────

    /**
     * Returns the full stock-change history for a product, newest first.
     */
    @Transactional(readOnly = true)
    public List<InventoryLog> getLogsByProductId(Long productId) {
        return inventoryLogRepository.findByProductIdOrderByTimestampDesc(productId);
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
}
