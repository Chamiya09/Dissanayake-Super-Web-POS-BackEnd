package com.dissayakesuper.web_pos_backend.reorder.dto;

import com.dissayakesuper.web_pos_backend.reorder.entity.Reorder;
import com.dissayakesuper.web_pos_backend.reorder.entity.Status;

import java.time.LocalDateTime;
import java.util.List;

public record ReorderResponseDTO(
        Long                        id,
        String                      orderRef,
        String                      supplierEmail,
        LocalDateTime               createdAt,
        Status                      status,
        double                      totalAmount,
        List<ReorderItemResponseDTO> items
) {
    /** Factory — maps a persisted {@link Reorder} to this DTO. */
    public static ReorderResponseDTO from(Reorder reorder) {
        List<ReorderItemResponseDTO> itemDTOs = reorder.getItems()
                .stream()
                .map(ReorderItemResponseDTO::from)
                .toList();

        return new ReorderResponseDTO(
                reorder.getId(),
                reorder.getOrderRef(),
                reorder.getSupplierEmail(),
                reorder.getCreatedAt(),
                reorder.getStatus(),
                reorder.getTotalAmount(),
                itemDTOs
        );
    }
}
