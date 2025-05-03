package com.example.newapp.controller;

import com.example.newapp.service.ErpAuthService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
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
                       HttpSession session,
                       HttpServletResponse response) {
        ResponseEntity<String> authResponse = erpAuthService.authenticate(username, password);
        
        if (authResponse.getStatusCode().is2xxSuccessful()) {
            String sessionCookie = erpAuthService.extractSessionId(authResponse);
            if (sessionCookie != null) {
                session.setAttribute("sid", sessionCookie);
                response.addHeader("Set-Cookie", sessionCookie);
                log.debug("Session cookie set: {}", sessionCookie);
                return "redirect:/suppliers";
            }
        }
        
        model.addAttribute("error", "Invalid credentials");
        return "auth/login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
} 