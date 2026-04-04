package com.dissayakesuper.web_pos_backend.product.dto;

/** Row-level error details returned by bulk product import. */
public record ProductImportError(
        int rowNumber,
        String sku,
        String message
) {}
