package com.dissayakesuper.web_pos_backend.inventory.dto;

/** Row payload for inventory CSV import. */
public record InventoryImportRequest(
        String sku,
        Double stockQuantity,
        Double reorderLevel,
        String unit
) {}
