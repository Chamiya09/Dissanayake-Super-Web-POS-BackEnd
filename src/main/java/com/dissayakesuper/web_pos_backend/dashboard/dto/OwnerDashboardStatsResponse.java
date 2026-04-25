package com.dissayakesuper.web_pos_backend.dashboard.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record OwnerDashboardStatsResponse(
        Kpis kpis,
        List<RevenuePoint> last30DaysRevenueTrend,
        List<CategoryPoint> salesByCategory,
        List<TopSellingProduct> topSellingProducts,
        List<RecentAlert> recentAlerts
) {
    public record Kpis(
            double totalRevenue,
            double netProfit,
            long totalUsers,
            long lowStockItems
    ) {
    }

    public record RevenuePoint(
            LocalDate date,
            double revenue
    ) {
    }

    public record CategoryPoint(
            String category,
            double value
    ) {
    }

    public record TopSellingProduct(
            String name,
            double qty
    ) {
    }

    public record RecentAlert(
            Long id,
            String action,
            LocalDateTime timestamp
    ) {
    }
}
