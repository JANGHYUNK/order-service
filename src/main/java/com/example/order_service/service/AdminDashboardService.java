package com.example.order_service.service;

import com.example.order_service.dto.AdminDashboardStats;
import com.example.order_service.dto.DashboardStats;
import com.example.order_service.entity.Order;
import com.example.order_service.entity.User;
import com.example.order_service.repository.OrderItemRepository;
import com.example.order_service.repository.OrderRepository;
import com.example.order_service.repository.ProductRepository;
import com.example.order_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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
public class AdminDashboardService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;

    public AdminDashboardStats getAdminDashboardStats() {
        LocalDateTime today = LocalDate.now().atStartOfDay();
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime last30Days = LocalDateTime.now().minusDays(30);

        BigDecimal totalRevenue = orderRepository.getTotalRevenueSince(LocalDateTime.of(2000, 1, 1, 0, 0));
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;

        BigDecimal todayRevenue = orderRepository.getTotalRevenueSince(today);
        if (todayRevenue == null) todayRevenue = BigDecimal.ZERO;

        BigDecimal monthRevenue = orderRepository.getTotalRevenueSince(monthStart);
        if (monthRevenue == null) monthRevenue = BigDecimal.ZERO;

        Long totalOrders = orderRepository.count();
        Long todayOrders = orderRepository.countOrdersSince(today);
        Long monthOrders = orderRepository.countOrdersSince(monthStart);
        Long totalProducts = productRepository.count();
        Long totalUsers = userRepository.countByRole(User.Role.USER);
        Long totalSellers = userRepository.countByRole(User.Role.SELLER);
        Long totalCustomers = totalUsers;

        List<DashboardStats.DailySales> dailySales = getDailySalesAll(last30Days);
        List<DashboardStats.ProductStats> topProducts = getTopProductsAll();
        List<DashboardStats.RecentOrder> recentOrders = getRecentOrdersAll();
        List<AdminDashboardStats.SellerPerformance> topSellers = getTopSellers();

        return AdminDashboardStats.builder()
                .totalRevenue(totalRevenue)
                .totalOrders(totalOrders)
                .totalProducts(totalProducts)
                .totalCustomers(totalCustomers)
                .totalSellers(totalSellers)
                .activeSellers(totalSellers) // Simplified for now
                .totalUsers(totalUsers + totalSellers)
                .todayRevenue(todayRevenue)
                .todayOrders(todayOrders)
                .monthRevenue(monthRevenue)
                .monthOrders(monthOrders)
                .dailySales(dailySales)
                .topProducts(topProducts)
                .recentOrders(recentOrders)
                .topSellers(topSellers)
                .build();
    }

    private List<DashboardStats.DailySales> getDailySalesAll(LocalDateTime startDate) {
        List<Object[]> results = orderItemRepository.getDailySalesAll(startDate);
        return results.stream()
                .map(row -> DashboardStats.DailySales.builder()
                        .date(((Date) row[0]).toLocalDate())
                        .revenue((BigDecimal) row[1])
                        .orderCount((Long) row[2])
                        .build())
                .collect(Collectors.toList());
    }

    private List<DashboardStats.ProductStats> getTopProductsAll() {
        // This would need a global query across all sellers
        return List.of(); // Simplified for now
    }

    private List<DashboardStats.RecentOrder> getRecentOrdersAll() {
        List<Order> orders = orderRepository.findRecentOrders();
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

    private List<AdminDashboardStats.SellerPerformance> getTopSellers() {
        List<User> sellers = userRepository.findByRole(User.Role.SELLER);
        return sellers.stream()
                .limit(5)
                .map(seller -> {
                    BigDecimal revenue = orderRepository.getTotalRevenueBySeller(seller);
                    if (revenue == null) revenue = BigDecimal.ZERO;

                    Long orders = orderRepository.countOrdersBySeller(seller);
                    Long products = productRepository.countBySeller(seller);

                    return AdminDashboardStats.SellerPerformance.builder()
                            .sellerId(seller.getId())
                            .sellerName(seller.getName())
                            .totalRevenue(revenue)
                            .totalOrders(orders)
                            .productCount(products)
                            .build();
                })
                .collect(Collectors.toList());
    }
}