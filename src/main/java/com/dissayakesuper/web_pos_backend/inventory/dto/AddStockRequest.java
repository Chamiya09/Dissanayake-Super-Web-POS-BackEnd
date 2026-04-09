package com.dissayakesuper.web_pos_backend.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for PUT /api/inventory/add-stock/{productId}
 */
public record AddStockRequest(

        @NotNull(message = "quantity is required.")
        @DecimalMin(value = "0.001", message = "quantity must be greater than zero.")
        @Digits(integer = 10, fraction = 3, message = "quantity supports up to 3 decimal places.")
        Double quantity
) {}
