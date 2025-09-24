package com.example.order_service.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class OAuth2Controller {

    @GetMapping("/auth/callback")
    public String oauth2Callback(@RequestParam(value = "token", required = false) String token,
                                @RequestParam(value = "error", required = false) String error) {
        if (error != null) {
            return "redirect:/login.html?error=" + error;
        }

        if (token != null) {
            return "redirect:/oauth2-success.html?token=" + token;
        }

        return "redirect:/login.html";
    }
}