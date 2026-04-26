package com.dissayakesuper.web_pos_backend.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Row payload for inventory CSV import. */
public record InventoryImportRequest(
        @NotBlank(message = "SKU is required.")
        @Size(max = 100, message = "SKU must be 100 characters or fewer.")
        String sku,

        @NotNull(message = "Stock quantity is required.")
        @DecimalMin(value = "0.0", inclusive = true, message = "Stock quantity must be 0 or greater.")
        Double stockQuantity,

        @DecimalMin(value = "0.0", inclusive = true, message = "Reorder level must be 0 or greater.")
        Double reorderLevel,

        @Size(max = 20, message = "Unit must be 20 characters or fewer.")
        String unit
) {}
