package com.dissayakesuper.web_pos_backend.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for POST /api/inventory/adjust/{id}
 */
public record AdjustStockRequest(

        @NotNull(message = "adjustmentAmount is required.")
        Double adjustmentAmount,

        @NotBlank(message = "notes is required — please explain the reason for this adjustment.")
        String notes
) {}
