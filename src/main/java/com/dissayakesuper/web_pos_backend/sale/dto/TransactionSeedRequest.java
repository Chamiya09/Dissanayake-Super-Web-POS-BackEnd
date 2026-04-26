package com.dissayakesuper.web_pos_backend.sale.dto;

import jakarta.validation.constraints.NotBlank;

public record TransactionSeedRequest(
        @NotBlank(message = "lastTransactionId is required")
        String lastTransactionId
) {
}
