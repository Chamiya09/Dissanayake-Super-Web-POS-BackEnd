package com.dissayakesuper.web_pos_backend.reorder.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;

import java.util.List;

/**
 * Request body for PUT /api/v1/reorder/{id}.
 * All fields are optional (null = leave unchanged).
 */
public record ReorderUpdateDTO(

        /** Updated supplier email address. Null means keep the existing value. */
        @Email(message = "supplierEmail must be a valid email address")
        String supplierEmail,

        /**
         * Replacement item list. When non-null and non-empty the existing items
         * are replaced with these; null means "don't touch the items".
         */
        @Valid
        List<ReorderItemRequestDTO> items
) {}
