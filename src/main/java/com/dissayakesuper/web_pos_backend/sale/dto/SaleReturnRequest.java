package com.dissayakesuper.web_pos_backend.sale.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record SaleReturnRequest(
        @NotEmpty(message = "items must not be empty.")
        @Valid
        List<SaleReturnItemRequest> items
) {}
