package com.example.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDashboardStats {

    private BigDecimal totalRevenue;
    private Long totalOrders;
    private Long totalProducts;
    private Long totalCustomers;
    private BigDecimal todayRevenue;
    private Long todayOrders;
    private BigDecimal monthRevenue;
    private Long monthOrders;
    private Long totalSellers;
    private Long activeSellers;
    private Long totalUsers;

    private List<DashboardStats.DailySales> dailySales;
    private List<DashboardStats.ProductStats> topProducts;
    private List<DashboardStats.RecentOrder> recentOrders;
    private List<SellerPerformance> topSellers;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SellerPerformance {
        private Long sellerId;
        private String sellerName;
        private Long totalOrders;
        private BigDecimal totalRevenue;
        private Long productCount;
    }
}