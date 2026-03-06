package com.dissayakesuper.web_pos_backend.sale.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Represents a single line-item inside a {@link SaleUpdateRequest}.
 */
public record SaleItemRequest(

        /** FK to the products table — used to look up inventory. Nullable for legacy items. */
        Long productId,

        @NotBlank(message = "productName is required.")
        String productName,

        @NotNull(message = "quantity is required.")
        @DecimalMin(value = "0.001", message = "quantity must be greater than zero.")
        BigDecimal quantity,

        @NotNull(message = "unitPrice is required.")
        @DecimalMin(value = "0.0", inclusive = true, message = "unitPrice must be 0 or greater.")
        Double unitPrice,

        @NotNull(message = "lineTotal is required.")
        @DecimalMin(value = "0.0", inclusive = true, message = "lineTotal must be 0 or greater.")
        Double lineTotal
) {}
