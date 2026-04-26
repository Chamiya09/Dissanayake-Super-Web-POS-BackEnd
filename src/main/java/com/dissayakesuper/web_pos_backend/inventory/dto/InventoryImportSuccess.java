package com.dissayakesuper.web_pos_backend.inventory.dto;

/** Plain response DTO for a successfully imported inventory row. */
public record InventoryImportSuccess(
        Long inventoryId,
        Long productId,
        String productName,
        String sku,
        Double stockQuantity,
        Double reorderLevel,
        String unit
) {}
