package com.dissayakesuper.web_pos_backend.reorder.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
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
        @DecimalMin(value = "0.001", message = "Quantity must be at least 0.001")
        @Digits(integer = 7, fraction = 3)
        BigDecimal quantity,

        @NotNull(message = "Unit price is required")
        @PositiveOrZero(message = "Unit price must be zero or positive")
        Double unitPrice
) {}
