package com.example.order_service.repository;

import com.example.order_service.entity.OrderItem;
import com.example.order_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("SELECT oi.product.id, oi.product.name, SUM(oi.quantity), SUM(oi.totalPrice) " +
           "FROM OrderItem oi WHERE oi.product.seller = :seller " +
           "GROUP BY oi.product.id, oi.product.name ORDER BY SUM(oi.totalPrice) DESC")
    List<Object[]> getTopProductsBySeller(@Param("seller") User seller);

    @Query("SELECT DATE(oi.order.createdAt), SUM(oi.totalPrice), COUNT(DISTINCT oi.order.id) " +
           "FROM OrderItem oi WHERE oi.product.seller = :seller " +
           "AND oi.order.createdAt >= :startDate " +
           "GROUP BY DATE(oi.order.createdAt) ORDER BY DATE(oi.order.createdAt)")
    List<Object[]> getDailySalesBySeller(@Param("seller") User seller, @Param("startDate") LocalDateTime startDate);

    @Query("SELECT DATE(o.createdAt), SUM(o.totalAmount), COUNT(o.id) " +
           "FROM Order o WHERE o.createdAt >= :startDate " +
           "GROUP BY DATE(o.createdAt) ORDER BY DATE(o.createdAt)")
    List<Object[]> getDailySalesAll(@Param("startDate") LocalDateTime startDate);
}