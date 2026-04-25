package com.dissayakesuper.web_pos_backend.shift.dto;

import com.dissayakesuper.web_pos_backend.shift.entity.Shift;
import com.dissayakesuper.web_pos_backend.shift.entity.ShiftStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ShiftResponse(
        Long id,
        Long userId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        BigDecimal initialCash,
        BigDecimal finalCash,
        ShiftStatus status,
        Double totalSales
) {
    public static ShiftResponse fromEntity(Shift shift, Double totalSales) {
        return new ShiftResponse(
                shift.getId(),
                shift.getUserId(),
                shift.getStartTime(),
                shift.getEndTime(),
                shift.getInitialCash(),
                shift.getFinalCash(),
                shift.getStatus(),
                totalSales
        );
    }
}
