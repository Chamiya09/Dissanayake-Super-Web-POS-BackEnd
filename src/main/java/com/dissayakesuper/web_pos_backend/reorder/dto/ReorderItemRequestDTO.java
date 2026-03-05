package com.dissayakesuper.web_pos_backend.reorder.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ReorderItemRequestDTO(

        @NotBlank(message = "Product name is required")
        String productName,

        /**
         * Optional soft-link to the products table.
         * The frontend sets this when placing an order from the Low Stock Alerts
         * page (where the product ID is available in the inventory record).
         * Null is accepted when the product cannot be resolved.
         */
        Long productId,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        Integer quantity,

        @NotNull(message = "Unit price is required")
        @PositiveOrZero(message = "Unit price must be zero or positive")
        Double unitPrice
) {}
