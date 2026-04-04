package com.dissayakesuper.web_pos_backend.product.dto;

import java.util.List;

/** Summary returned after bulk importing products. */
public record ProductBulkImportResponse(
        int totalRows,
        int importedCount,
        int failedCount,
        List<ProductImportSuccess> importedProducts,
        List<ProductImportError> errors
) {}
