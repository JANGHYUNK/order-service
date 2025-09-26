package com.example.order_service.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/api/test/user")
    public String getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated()) {
            return "Authenticated user: " + auth.getName() + " | Principal: " + auth.getPrincipal().getClass().getSimpleName();
        }

        return "Not authenticated";
    }

    @GetMapping("/api/test/health")
    public String health() {
        return "OK";
    }
}