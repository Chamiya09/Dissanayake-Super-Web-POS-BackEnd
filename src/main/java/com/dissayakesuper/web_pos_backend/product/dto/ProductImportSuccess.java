package com.dissayakesuper.web_pos_backend.product.dto;

import java.math.BigDecimal;

/** Plain response DTO for a successfully imported product row. */
public record ProductImportSuccess(
        Long id,
        String productName,
        String sku,
        String category,
        BigDecimal buyingPrice,
        BigDecimal sellingPrice,
        String unit,
        Double stockQuantity,
        Double reorderLevel
) {}