package com.dissayakesuper.web_pos_backend.inventory.dto;

/** Row-level error details returned by inventory CSV import. */
public record InventoryImportError(
        int rowNumber,
        String sku,
        String message
) {}
