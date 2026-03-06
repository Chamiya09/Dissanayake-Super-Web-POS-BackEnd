package com.dissayakesuper.web_pos_backend.sale.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Request body for PUT /api/sales/{id}.
 * Contains the updated payment method, total, and the full list of line items.
 * The service uses this to reverse the old inventory deductions and apply new ones.
 */
public record SaleUpdateRequest(

        @NotBlank(message = "paymentMethod is required.")
        String paymentMethod,

        @NotNull(message = "totalAmount is required.")
        @DecimalMin(value = "0.0", inclusive = true, message = "totalAmount must be 0 or greater.")
        Double totalAmount,

        @NotEmpty(message = "items must not be empty.")
        @Valid
        List<SaleItemRequest> items
) {}
