package com.dissayakesuper.web_pos_backend.inventory.dto;

/**
 * Aggregated inventory metrics returned by GET /api/inventory/analytics.
 *
 * @param totalTrackedItems  Total number of products currently tracked in the inventory table.
 * @param lowStockAlerts     Count of items where stockQuantity {@literal <=} reorderLevel.
 * @param outOfStock         Count of items where stockQuantity {@literal <=} 0.
 * @param totalInventoryValue Sum of (stockQuantity * sellingPrice) for all tracked items.
 */
public record InventoryAnalyticsDTO(
        long   totalTrackedItems,
        long   lowStockAlerts,
        long   outOfStock,
        double totalInventoryValue
) {}
