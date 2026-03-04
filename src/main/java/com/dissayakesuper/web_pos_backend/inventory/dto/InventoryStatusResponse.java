package com.dissayakesuper.web_pos_backend.inventory.dto;

import com.dissayakesuper.web_pos_backend.inventory.entity.Inventory;

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

        // ── Inventory fields ────────────────────────────────────────────────
        Double stockQuantity,
        Double reorderLevel,
        String unit,
        String stockStatus,        // "IN_STOCK" | "LOW_STOCK" | "OUT_OF_STOCK"
        LocalDateTime lastUpdated
) {
    /** Convenience factory — builds from an Inventory entity. */
    public static InventoryStatusResponse from(Inventory inv) {
        double qty     = inv.getStockQuantity();
        double reorder = inv.getReorderLevel();

        String status;
        if (qty <= 0)           status = "OUT_OF_STOCK";
        else if (qty < reorder) status = "LOW_STOCK";
        else                    status = "IN_STOCK";

        return new InventoryStatusResponse(
                inv.getId(),
                inv.getProduct().getId(),
                inv.getProduct().getProductName(),
                inv.getProduct().getSku(),
                inv.getProduct().getCategory(),
                qty,
                reorder,
                inv.getUnit(),
                status,
                inv.getLastUpdated()
        );
    }
}
