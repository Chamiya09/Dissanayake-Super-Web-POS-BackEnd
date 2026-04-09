package com.dissayakesuper.web_pos_backend.supplier.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record AssignProductsRequest(
        @NotNull(message = "Product IDs are required.")
        @NotEmpty(message = "Select at least one product.")
        List<@NotNull(message = "Product ID cannot be null.") Long> productIds
) {}
