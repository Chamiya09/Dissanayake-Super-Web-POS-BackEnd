package com.dissayakesuper.web_pos_backend.shift.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record StartShiftRequest(
        @NotNull(message = "Opening cash is required.")
        @DecimalMin(value = "0.0", inclusive = true, message = "Opening cash must be 0 or greater.")
        BigDecimal initialCash
) {
}
