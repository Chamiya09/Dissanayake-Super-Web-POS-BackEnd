package com.dissayakesuper.web_pos_backend.reorder.dto;

public record LowStockItemDTO(
        Long   productId,
        String productName,
        String sku,
        String category,
        double currentStock,
        double reorderLevel,
        String unit,
        /** Selling price (LKR) — forwarded to the frontend for order cost calculation. */
        double sellingPrice,
        /** Supplier company name — null if no supplier is assigned to this product. */
        String supplierName,
        /** Supplier contact email — null if no supplier is assigned to this product. */
        String supplierEmail,
        String productStatus
) {}
