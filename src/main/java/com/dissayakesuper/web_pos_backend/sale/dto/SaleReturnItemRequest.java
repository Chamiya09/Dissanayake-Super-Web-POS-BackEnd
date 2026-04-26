package com.dissayakesuper.web_pos_backend.sale.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record SaleReturnItemRequest(
        @NotNull(message = "saleItemId is required.")
        Long saleItemId,

        @NotNull(message = "quantity is required.")
        @DecimalMin(value = "0.001", message = "quantity must be greater than zero.")
        BigDecimal quantity
) {}
