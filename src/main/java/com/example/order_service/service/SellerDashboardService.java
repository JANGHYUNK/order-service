package com.example.order_service.service;

import com.example.order_service.dto.DashboardStats;
import com.example.order_service.dto.SellerDashboardStats;
import com.example.order_service.entity.Order;
import com.example.order_service.entity.Product;
import com.example.order_service.entity.User;
import com.example.order_service.repository.OrderItemRepository;
import com.example.order_service.repository.OrderRepository;
import com.example.order_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SellerDashboardService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;

    public SellerDashboardStats getSellerDashboardStats(User seller) {
        LocalDateTime today = LocalDate.now().atStartOfDay();
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime last30Days = LocalDateTime.now().minusDays(30);

        BigDecimal totalRevenue = orderRepository.getTotalRevenueBySeller(seller);
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;

        BigDecimal todayRevenue = orderRepository.getRevenueBySellerSince(seller, today);
        if (todayRevenue == null) todayRevenue = BigDecimal.ZERO;

        BigDecimal monthRevenue = orderRepository.getRevenueBySellerSince(seller, monthStart);
        if (monthRevenue == null) monthRevenue = BigDecimal.ZERO;

        Long totalOrders = orderRepository.countOrdersBySeller(seller);
        Long totalProducts = productRepository.countBySeller(seller);
        Long activeProducts = productRepository.countBySellerAndStatus(seller, Product.ProductStatus.ACTIVE);
        Long outOfStockProducts = productRepository.countOutOfStockBySeller(seller);
        Long pendingOrders = orderRepository.countOrdersBySellerAndStatus(seller, Order.OrderStatus.PENDING);
        Long processingOrders = orderRepository.countOrdersBySellerAndStatus(seller, Order.OrderStatus.PROCESSING);

        List<DashboardStats.DailySales> dailySales = getDailySalesBySeller(seller, last30Days);
        List<DashboardStats.ProductStats> topProducts = getTopProductsBySeller(seller);
        List<DashboardStats.RecentOrder> recentOrders = getRecentOrdersBySeller(seller);

        return SellerDashboardStats.builder()
                .sellerId(seller.getId())
                .sellerName(seller.getName())
                .totalRevenue(totalRevenue)
                .totalOrders(totalOrders)
                .totalProducts(totalProducts)
                .activeProducts(activeProducts)
                .outOfStockProducts(outOfStockProducts)
                .pendingOrders(pendingOrders)
                .processingOrders(processingOrders)
                .todayRevenue(todayRevenue)
                .monthRevenue(monthRevenue)
                .dailySales(dailySales)
                .topProducts(topProducts)
                .recentOrders(recentOrders)
                .build();
    }

    private List<DashboardStats.DailySales> getDailySalesBySeller(User seller, LocalDateTime startDate) {
        List<Object[]> results = orderItemRepository.getDailySalesBySeller(seller, startDate);
        return results.stream()
                .map(row -> DashboardStats.DailySales.builder()
                        .date(((Date) row[0]).toLocalDate())
                        .revenue((BigDecimal) row[1])
                        .orderCount((Long) row[2])
                        .build())
                .collect(Collectors.toList());
    }

    private List<DashboardStats.ProductStats> getTopProductsBySeller(User seller) {
        List<Object[]> results = orderItemRepository.getTopProductsBySeller(seller);
        return results.stream()
                .limit(5)
                .map(row -> DashboardStats.ProductStats.builder()
                        .productId((Long) row[0])
                        .productName((String) row[1])
                        .salesCount((Long) row[2])
                        .revenue((BigDecimal) row[3])
                        .build())
                .collect(Collectors.toList());
    }

    private List<DashboardStats.RecentOrder> getRecentOrdersBySeller(User seller) {
        List<Order> orders = orderRepository.findOrdersBySeller(seller);
        return orders.stream()
                .limit(10)
                .map(order -> DashboardStats.RecentOrder.builder()
                        .orderId(order.getId())
                        .orderNumber(order.getOrderNumber())
                        .customerName(order.getCustomer().getName())
                        .amount(order.getTotalAmount())
                        .status(order.getStatus().name())
                        .createdAt(order.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                        .build())
                .collect(Collectors.toList());
    }
}