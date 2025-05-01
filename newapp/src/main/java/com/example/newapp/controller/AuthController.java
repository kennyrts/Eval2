package com.example.newapp.controller;

import com.example.newapp.service.ErpAuthService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;

@Controller
public class AuthController {

    private final ErpAuthService erpAuthService;

    public AuthController(ErpAuthService erpAuthService) {
        this.erpAuthService = erpAuthService;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                       @RequestParam String password,
                       Model model,
                       HttpSession session) {
        if (erpAuthService.authenticate(username, password)) {
            session.setAttribute("user", username);
            return "redirect:/suppliers";
        }
        
        model.addAttribute("error", "Invalid credentials");
        return "auth/login";
    }
} 