package com.example.order_service.repository;

import com.example.order_service.entity.Product;
import com.example.order_service.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findBySeller(User seller);

    List<Product> findBySellerAndStatus(User seller, Product.ProductStatus status);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.seller = :seller")
    Long countBySeller(@Param("seller") User seller);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.seller = :seller AND p.status = :status")
    Long countBySellerAndStatus(@Param("seller") User seller, @Param("status") Product.ProductStatus status);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.seller = :seller AND p.stockQuantity = 0")
    Long countOutOfStockBySeller(@Param("seller") User seller);

    // Public browsing methods
    Page<Product> findByCategory(String category, Pageable pageable);

    @Query("SELECT DISTINCT p.category FROM Product p WHERE p.category IS NOT NULL")
    List<String> findDistinctCategories();
}