package com.dissayakesuper.web_pos_backend.sale.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

public record SaleReturnRequest(
        @Valid
        List<SaleReturnItemRequest> items,

        @NotBlank(message = "Approver email is required.")
        String approverEmail,

        @NotBlank(message = "Approver password is required.")
        String approverPassword
) {}
