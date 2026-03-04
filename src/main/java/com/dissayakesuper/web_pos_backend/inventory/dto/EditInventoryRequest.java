package com.dissayakesuper.web_pos_backend.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

/**
 * Request body for PUT /api/inventory/edit/{id}
 * Both fields are optional — null values are ignored (partial update).
 */
public record EditInventoryRequest(

        @DecimalMin(value = "0.0", message = "reorderLevel must be 0 or greater.")
        Double reorderLevel,

        @Size(max = 20, message = "unit must be 20 characters or fewer.")
        String unit
) {}
