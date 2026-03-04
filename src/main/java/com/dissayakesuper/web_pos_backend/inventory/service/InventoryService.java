package com.dissayakesuper.web_pos_backend.inventory.service;

import com.dissayakesuper.web_pos_backend.inventory.entity.Inventory;
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

    private final InventoryRepository inventoryRepository;
    private final ProductRepository   productRepository;

    public InventoryService(InventoryRepository inventoryRepository,
                            ProductRepository productRepository) {
        this.inventoryRepository = inventoryRepository;
        this.productRepository   = productRepository;
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

        inventory.setStockQuantity(inventory.getStockQuantity() + quantityToAdd);
        inventory.setLastUpdated(LocalDateTime.now());

        return inventoryRepository.save(inventory);
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
