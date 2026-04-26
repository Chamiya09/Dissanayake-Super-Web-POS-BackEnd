package com.dissayakesuper.web_pos_backend.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

/**
 * Request body for PUT /api/inventory/edit/{id}
 * All fields are optional — null values are ignored (partial update).
 * quantityToAdd, when present and positive, is added to the current stock.
 */
public record EditInventoryRequest(

        @DecimalMin(value = "0.0", message = "reorderLevel must be 0 or greater.")
        Double reorderLevel,

        @Size(max = 20, message = "unit must be 20 characters or fewer.")
        String unit,

        @DecimalMin(value = "0.0", inclusive = false, message = "quantityToAdd must be greater than zero.")
        Double quantityToAdd
) {}
