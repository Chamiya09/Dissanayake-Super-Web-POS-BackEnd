package com.dissayakesuper.web_pos_backend.reorder.dto;

import com.dissayakesuper.web_pos_backend.reorder.entity.ReorderItem;

public record ReorderItemResponseDTO(
        Long   id,
        String productName,
        int    quantity,
        double unitPrice,
        double lineTotal
) {
    /** Factory — maps a persisted {@link ReorderItem} to this DTO. */
    public static ReorderItemResponseDTO from(ReorderItem item) {
        return new ReorderItemResponseDTO(
                item.getId(),
                item.getProductName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getQuantity() * item.getUnitPrice()
        );
    }
}
