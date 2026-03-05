package com.dissayakesuper.web_pos_backend.reorder.dto;

public record LowStockItemDTO(
        Long   productId,
        String productName,
        String sku,
        String category,
        double currentStock,
        double reorderLevel,
        String unit
) {}
