package com.dissayakesuper.web_pos_backend.sale.dto;

import java.util.List;

import jakarta.validation.Valid;

public record SaleReturnRequest(
        @Valid
        List<SaleReturnItemRequest> items,

        String approverId,

        String approverEmail,

        String approverPassword
) {}
