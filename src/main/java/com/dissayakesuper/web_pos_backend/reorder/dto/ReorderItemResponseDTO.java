package com.dissayakesuper.web_pos_backend.reorder.dto;

import java.math.BigDecimal;

import com.dissayakesuper.web_pos_backend.reorder.entity.ReorderItem;

public record ReorderItemResponseDTO(
        Long   id,
        Long   productId,
        String productName,
        BigDecimal quantity,
        double     unitPrice,
        double     lineTotal
) {
    /** Factory — maps a persisted {@link ReorderItem} to this DTO. */
    public static ReorderItemResponseDTO from(ReorderItem item) {
        return new ReorderItemResponseDTO(
                item.getId(),
                item.getProductId(),
                item.getProductName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getQuantity().doubleValue() * item.getUnitPrice()
        );
    }
}
