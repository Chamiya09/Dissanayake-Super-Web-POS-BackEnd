package com.dissayakesuper.web_pos_backend.supplier.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotNull;

public record SupplierStatusRequest(
        @NotNull
        @JsonProperty("isActive")
        Boolean isActive
) {}
