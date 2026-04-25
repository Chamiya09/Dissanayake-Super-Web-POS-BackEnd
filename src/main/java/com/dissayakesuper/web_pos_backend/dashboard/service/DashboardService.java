package com.dissayakesuper.web_pos_backend.dashboard.service;

import com.dissayakesuper.web_pos_backend.dashboard.dto.ManagerDashboardStatsResponse;
import com.dissayakesuper.web_pos_backend.dashboard.dto.OwnerDashboardStatsResponse;
import com.dissayakesuper.web_pos_backend.inventory.entity.Inventory;
import com.dissayakesuper.web_pos_backend.inventory.repository.InventoryRepository;
import com.dissayakesuper.web_pos_backend.product.entity.Product;
import com.dissayakesuper.web_pos_backend.product.repository.ProductRepository;
import com.dissayakesuper.web_pos_backend.sale.entity.Sale;
import com.dissayakesuper.web_pos_backend.sale.entity.SaleItem;
import com.dissayakesuper.web_pos_backend.sale.repository.SaleRepository;
import com.dissayakesuper.web_pos_backend.shift.entity.ShiftStatus;
import com.dissayakesuper.web_pos_backend.shift.repository.ShiftRepository;
import com.dissayakesuper.web_pos_backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class DashboardService {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final InventoryRepository inventoryRepository;
    private final ShiftRepository shiftRepository;

    public DashboardService(SaleRepository saleRepository,
                            ProductRepository productRepository,
                            UserRepository userRepository,
                            InventoryRepository inventoryRepository,
                            ShiftRepository shiftRepository) {
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.inventoryRepository = inventoryRepository;
        this.shiftRepository = shiftRepository;
    }

    public OwnerDashboardStatsResponse getOwnerStats() {
        List<Sale> allSales = saleRepository.findAll();
        List<Sale> completedSales = allSales.stream()
                .filter(sale -> isStatus(sale.getStatus(), "completed"))
                .toList();

        List<Product> products = productRepository.findAll();
        Map<Long, Product> productById = new HashMap<>();
        for (Product product : products) {
            productById.put(product.getId(), product);
        }

        double totalRevenue = completedSales.stream()
                .mapToDouble(sale -> nvl(sale.getTotalAmount()))
                .sum();

        double netProfit = 0.0;
        for (Sale sale : completedSales) {
            for (SaleItem item : sale.getItems()) {
                Product product = productById.get(item.getProductId());
                double sellingUnitPrice = nvl(item.getUnitPrice());
                double buyingUnitPrice = product == null ? 0.0 : nvl(product.getBuyingPrice());
                double qty = nvl(item.getQuantity());
                netProfit += (sellingUnitPrice - buyingUnitPrice) * qty;
            }
        }

        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(29);
        Map<LocalDate, Double> revenueByDate = new LinkedHashMap<>();
        for (int i = 0; i < 30; i++) {
            LocalDate d = start.plusDays(i);
            revenueByDate.put(d, 0.0);
        }
        for (Sale sale : completedSales) {
            if (sale.getSaleDate() == null) {
                continue;
            }
            LocalDate d = sale.getSaleDate().toLocalDate();
            if (!d.isBefore(start) && !d.isAfter(today)) {
                revenueByDate.put(d, revenueByDate.getOrDefault(d, 0.0) + nvl(sale.getTotalAmount()));
            }
        }

        List<OwnerDashboardStatsResponse.RevenuePoint> trend = revenueByDate.entrySet().stream()
                .map(entry -> new OwnerDashboardStatsResponse.RevenuePoint(entry.getKey(), round2(entry.getValue())))
                .toList();

        Map<String, Double> categoryTotals = new HashMap<>();
        Map<String, Double> topSellingQty = new HashMap<>();
        for (Sale sale : completedSales) {
            for (SaleItem item : sale.getItems()) {
                Product product = productById.get(item.getProductId());
                String category = product == null || product.getCategory() == null || product.getCategory().isBlank()
                        ? "Uncategorized"
                        : product.getCategory();
                categoryTotals.put(category, categoryTotals.getOrDefault(category, 0.0) + nvl(item.getLineTotal()));

                String itemName = item.getProductName() == null || item.getProductName().isBlank()
                        ? "Unknown Product"
                        : item.getProductName();
                topSellingQty.put(itemName, topSellingQty.getOrDefault(itemName, 0.0) + nvl(item.getQuantity()));
            }
        }

        List<OwnerDashboardStatsResponse.CategoryPoint> categoryPoints = categoryTotals.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(entry -> new OwnerDashboardStatsResponse.CategoryPoint(entry.getKey(), round2(entry.getValue())))
                .toList();

        List<OwnerDashboardStatsResponse.TopSellingProduct> topSellingProducts = topSellingQty.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(8)
                .map(entry -> new OwnerDashboardStatsResponse.TopSellingProduct(entry.getKey(), round3(entry.getValue())))
                .toList();

        OwnerDashboardStatsResponse.Kpis kpis = new OwnerDashboardStatsResponse.Kpis(
                round2(totalRevenue),
                round2(netProfit),
                userRepository.count(),
                inventoryRepository.findAllLowStock().size()
        );

        return new OwnerDashboardStatsResponse(kpis, trend, categoryPoints, topSellingProducts);
    }

    public ManagerDashboardStatsResponse getManagerStats() {
        List<Sale> allSales = saleRepository.findAll();
        LocalDate today = LocalDate.now();

        List<Sale> todayCompletedSales = allSales.stream()
                .filter(sale -> isStatus(sale.getStatus(), "completed"))
                .filter(sale -> sale.getSaleDate() != null && sale.getSaleDate().toLocalDate().isEqual(today))
                .toList();

        double todaysSales = todayCompletedSales.stream()
                .mapToDouble(sale -> nvl(sale.getTotalAmount()))
                .sum();

        long pendingReturnsVoids = allSales.stream()
                .filter(sale -> {
                    String status = normalize(sale.getStatus());
                    return Set.of("voided", "returned", "partially returned").contains(status);
                })
                .count();

        List<ManagerDashboardStatsResponse.HourlySalesPoint> hourlySales = buildHourlySales(todayCompletedSales);

        List<Inventory> allInventory = inventoryRepository.findAll();
        long outOfStockItems = allInventory.stream()
                .filter(inv -> nvl(inv.getStockQuantity()) <= 0.0)
                .count();

        List<ManagerDashboardStatsResponse.LowStockItem> lowStockList = inventoryRepository.findAllLowStock().stream()
                .sorted(Comparator.comparingDouble(inv -> nvl(inv.getStockQuantity())))
                .limit(20)
                .map(inv -> new ManagerDashboardStatsResponse.LowStockItem(
                        inv.getProduct().getId(),
                        inv.getProduct().getProductName(),
                        round3(nvl(inv.getStockQuantity())),
                        round3(nvl(inv.getReorderLevel()))
                ))
                .toList();

        ManagerDashboardStatsResponse.Kpis kpis = new ManagerDashboardStatsResponse.Kpis(
                round2(todaysSales),
                shiftRepository.countByStatus(ShiftStatus.OPEN),
                pendingReturnsVoids,
                outOfStockItems
        );

        return new ManagerDashboardStatsResponse(kpis, hourlySales, lowStockList);
    }

    private List<ManagerDashboardStatsResponse.HourlySalesPoint> buildHourlySales(List<Sale> sales) {
        List<ManagerDashboardStatsResponse.HourlySalesPoint> points = new ArrayList<>();
        Map<Integer, Double> amountByHour = new HashMap<>();

        for (Sale sale : sales) {
            if (sale.getSaleDate() == null) {
                continue;
            }
            int hour = sale.getSaleDate().getHour();
            amountByHour.put(hour, amountByHour.getOrDefault(hour, 0.0) + nvl(sale.getTotalAmount()));
        }

        for (int hour = 0; hour < 24; hour++) {
            String label = String.format(Locale.ROOT, "%02d:00", hour);
            points.add(new ManagerDashboardStatsResponse.HourlySalesPoint(label, round2(amountByHour.getOrDefault(hour, 0.0))));
        }

        return points;
    }

    private boolean isStatus(String actual, String expected) {
        return expected.equalsIgnoreCase(normalize(actual));
    }

    private String normalize(String input) {
        return input == null ? "" : input.trim().toLowerCase(Locale.ROOT);
    }

    private double nvl(Double value) {
        return value == null ? 0.0 : value;
    }

    private double nvl(BigDecimal value) {
        return value == null ? 0.0 : value.doubleValue();
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private double round3(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
