package com.dissayakesuper.web_pos_backend.supplier.dto;

import jakarta.validation.constraints.*;

/**
 * Inbound DTO for create and update operations.
 * Keeps the JPA entity free from request-body concerns.
 */
public record SupplierRequest(

        @NotBlank(message = "Company name is required.")
        @Size(max = 255)
        String companyName,

        @NotBlank(message = "Contact person is required.")
        @Size(max = 255)
        String contactPerson,

        @NotBlank(message = "Email is required.")
        @Email(message = "Enter a valid email address.")
        @Size(max = 255)
        String email,

        @NotBlank(message = "Phone number is required.")
        @Size(max = 20)
        String phone,

        @NotNull(message = "Lead time is required.")
        @Min(value = 1, message = "Lead time must be at least 1 day.")
        Integer leadTime,

        boolean isAutoReorderEnabled
) {}
