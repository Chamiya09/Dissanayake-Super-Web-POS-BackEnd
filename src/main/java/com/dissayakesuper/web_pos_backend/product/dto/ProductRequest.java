package com.dissayakesuper.web_pos_backend.product.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * Inbound DTO for create and update operations.
 * Keeps the JPA entity free from request-body concerns.
 */
public record ProductRequest(

        @NotBlank(message = "Product name is required.")
        @Size(max = 255)
        String productName,

        @NotBlank(message = "SKU / Barcode is required.")
        @Size(max = 100)
        String sku,

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
        BigDecimal sellingPrice
) {}
