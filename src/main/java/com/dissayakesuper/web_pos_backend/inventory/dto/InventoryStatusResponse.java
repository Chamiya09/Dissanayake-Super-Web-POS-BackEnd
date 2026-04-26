package com.dissayakesuper.web_pos_backend.inventory.dto;

import com.dissayakesuper.web_pos_backend.inventory.entity.Inventory;
import com.dissayakesuper.web_pos_backend.product.entity.Product;

import java.time.LocalDateTime;

/**
 * Flattened read-only view combining Product and Inventory data.
 * Returned by GET /api/inventory/status and GET /api/inventory/low-stock.
 */
public record InventoryStatusResponse(

        Long   inventoryId,

        // ── Product fields ──────────────────────────────────────────────────
        Long   productId,
        String productName,
        String sku,
        String category,
        Double sellingPrice,

        // ── Inventory fields ────────────────────────────────────────────────
        Double stockQuantity,
        Double reorderLevel,
        String unit,
        String stockStatus,        // "IN_STOCK" | "LOW_STOCK" | "OUT_OF_STOCK"
        String productStatus,
        Long supplierId,
        String supplierEmail,
        Boolean supplierActive,
        LocalDateTime lastUpdated
) {
    /** Convenience factory — builds from an Inventory entity. */
    public static InventoryStatusResponse from(Inventory inv) {
                double qty     = inv.getStockQuantity() != null ? inv.getStockQuantity() : 0.0;
                double reorder = inv.getReorderLevel() != null ? inv.getReorderLevel() : 0.0;
                Product product = inv.getProduct();

        String status;
        if (qty <= 0)            status = "OUT_OF_STOCK";
        else if (qty <= reorder) status = "LOW_STOCK";
        else                     status = "IN_STOCK";

        return new InventoryStatusResponse(
                inv.getId(),
                product != null ? product.getId() : null,
                product != null ? product.getProductName() : "Unknown Product",
                product != null ? product.getSku() : "N/A",
                product != null ? product.getCategory() : "Uncategorized",
                product != null && product.getSellingPrice() != null
                        ? product.getSellingPrice().doubleValue() : 0.0,
                qty,
                reorder,
                inv.getUnit() != null ? inv.getUnit() : "N/A",
                status,
                product != null && product.getStatus() != null ? product.getStatus().name() : null,
                product != null ? product.getSupplierId() : null,
                product != null && product.getSupplier() != null ? product.getSupplier().getEmail() : null,
                product != null && product.getSupplier() != null ? product.getSupplier().isActive() : null,
                inv.getLastUpdated()
        );
    }
}
