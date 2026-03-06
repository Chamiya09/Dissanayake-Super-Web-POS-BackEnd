package com.dissayakesuper.web_pos_backend.supplier.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AssignProductsRequest(
        @NotNull List<Long> productIds
) {}
