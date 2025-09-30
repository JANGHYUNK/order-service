package com.example.order_service.controller;

import com.example.order_service.dto.SellerDashboardStats;
import com.example.order_service.entity.User;
import com.example.order_service.security.CustomUserDetailsService;
import com.example.order_service.service.SellerDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Controller
@RequestMapping("/seller")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SELLER')")
public class SellerDashboardController {

    private final SellerDashboardService sellerDashboardService;
    private final CustomUserDetailsService userDetailsService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        return "seller-dashboard";
    }

    @GetMapping("/api/stats")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<SellerDashboardStats> getSellerStats() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();

        User seller = userDetailsService.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Seller not found"));

        if (seller.getRole() != User.Role.SELLER) {
            return ResponseEntity.status(403).build();
        }

        SellerDashboardStats stats = sellerDashboardService.getSellerDashboardStats(seller);
        return ResponseEntity.ok(stats);
    }
}