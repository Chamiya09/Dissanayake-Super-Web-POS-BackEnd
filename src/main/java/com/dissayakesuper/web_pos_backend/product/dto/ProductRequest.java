package com.dissayakesuper.web_pos_backend.product.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Inbound DTO for create and update operations.
 * Keeps the JPA entity free from request-body concerns.
 */
public record ProductRequest(

        @NotBlank(message = "Product name is required.")
        @Size(max = 255)
        String productName,

        @Size(max = 100)
        String sku,

        @Size(max = 100)
        String barcode,

        @NotBlank(message = "Category is required.")
        @Size(max = 100)
        String category,

        @NotNull(message = "Buying price is required.")
        @DecimalMin(value = "0.0", inclusive = true, message = "Buying price must be 0 or greater.")
        @Digits(integer = 8, fraction = 2, message = "Buying price must have at most 2 decimal places.")
        BigDecimal buyingPrice,

        @NotNull(message = "Selling price is required.")
        @DecimalMin(value = "0.0", inclusive = true, message = "Selling price must be 0 or greater.")
        @Digits(integer = 8, fraction = 2, message = "Selling price must have at most 2 decimal places.")
        BigDecimal sellingPrice,

        @Size(max = 50, message = "Unit must be 50 characters or fewer.")
        String unit,         // optional — e.g. kg, grams, liters, pieces, bottles

        @DecimalMin(value = "0.0", inclusive = true, message = "Stock quantity must be 0 or greater.")
        Double stockQuantity, // optional — defaults to 0.0 on the entity

        @DecimalMin(value = "0.0", inclusive = true, message = "Reorder level must be 0 or greater.")
        Double reorderLevel   // optional — alert threshold, null = no alert
) {}
