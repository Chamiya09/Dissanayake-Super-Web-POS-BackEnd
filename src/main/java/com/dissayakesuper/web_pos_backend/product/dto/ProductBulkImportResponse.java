package com.dissayakesuper.web_pos_backend.product.dto;

import java.util.List;

import com.dissayakesuper.web_pos_backend.product.entity.Product;

/** Summary returned after bulk importing products. */
public record ProductBulkImportResponse(
        int totalRows,
        int importedCount,
        int failedCount,
        List<Product> importedProducts,
        List<ProductImportError> errors
) {}
