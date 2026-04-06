package com.dissayakesuper.web_pos_backend.sale.dto;

public record TransactionSeedResponse(
        String appliedSeed,
        String nextTransactionPreview,
        String message
) {
}
