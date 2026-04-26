package com.dissayakesuper.web_pos_backend.dashboard.dto;

import java.util.List;

public record ManagerDashboardStatsResponse(
        Kpis kpis,
        List<HourlySalesPoint> todaysHourlySales,
        List<LowStockItem> lowStockActionList
) {
    public record Kpis(
            double todaysSales,
            long pendingReturnsVoids,
            long outOfStockItems
    ) {
    }

    public record HourlySalesPoint(
            String hour,
            double amount
    ) {
    }

    public record LowStockItem(
            long productId,
            String productName,
            double stockQuantity,
            double reorderLevel
    ) {
    }
}
