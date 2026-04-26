package com.dissayakesuper.web_pos_backend.inventory.dto;

import java.util.List;

/** Summary returned after bulk importing inventory values. */
public record InventoryBulkImportResponse(
        int totalRows,
        int importedCount,
        int failedCount,
        List<InventoryImportSuccess> importedItems,
        List<InventoryImportError> errors
) {}
