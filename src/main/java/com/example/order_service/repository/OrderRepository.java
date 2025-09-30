package com.example.order_service.repository;

import com.example.order_service.entity.Order;
import com.example.order_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomer(User customer);

    @Query("SELECT o FROM Order o JOIN o.orderItems oi WHERE oi.product.seller = :seller ORDER BY o.createdAt DESC")
    List<Order> findOrdersBySeller(@Param("seller") User seller);

    @Query("SELECT o FROM Order o JOIN o.orderItems oi WHERE oi.product.seller = :seller AND o.status = :status")
    List<Order> findOrdersBySellerAndStatus(@Param("seller") User seller, @Param("status") Order.OrderStatus status);

    @Query("SELECT COUNT(o) FROM Order o JOIN o.orderItems oi WHERE oi.product.seller = :seller")
    Long countOrdersBySeller(@Param("seller") User seller);

    @Query("SELECT COUNT(o) FROM Order o JOIN o.orderItems oi WHERE oi.product.seller = :seller AND o.status = :status")
    Long countOrdersBySellerAndStatus(@Param("seller") User seller, @Param("status") Order.OrderStatus status);

    @Query("SELECT SUM(oi.totalPrice) FROM OrderItem oi WHERE oi.product.seller = :seller")
    BigDecimal getTotalRevenueBySeller(@Param("seller") User seller);

    @Query("SELECT SUM(oi.totalPrice) FROM OrderItem oi WHERE oi.product.seller = :seller AND oi.order.createdAt >= :startDate")
    BigDecimal getRevenueBySellerSince(@Param("seller") User seller, @Param("startDate") LocalDateTime startDate);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt >= :startDate")
    Long countOrdersSince(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.createdAt >= :startDate")
    BigDecimal getTotalRevenueSince(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT o FROM Order o ORDER BY o.createdAt DESC")
    List<Order> findRecentOrders();
}