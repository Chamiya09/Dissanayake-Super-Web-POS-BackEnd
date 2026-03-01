package com.dissayakesuper.web_pos_backend.sale.dto;

import jakarta.validation.constraints.NotBlank;

public record StatusRequest(
        @NotBlank(message = "Status must not be blank")
        String status
) {}
