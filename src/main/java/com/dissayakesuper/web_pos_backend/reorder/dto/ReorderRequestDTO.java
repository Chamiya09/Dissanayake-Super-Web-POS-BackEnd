package com.dissayakesuper.web_pos_backend.reorder.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ReorderRequestDTO(

        @NotBlank(message = "Order reference is required")
        String orderRef,

        @NotBlank(message = "Supplier email is required")
        @Email(message = "Supplier email must be a valid email address")
        String supplierEmail,

        @NotEmpty(message = "At least one item is required")
        @Valid
        List<ReorderItemRequestDTO> items
) {}
