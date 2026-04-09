package com.dissayakesuper.web_pos_backend.reorder.dto;

import jakarta.validation.constraints.NotBlank;

public record StatusUpdateRequest(
        @NotBlank(message = "Status is required.")
        String status
) {}
