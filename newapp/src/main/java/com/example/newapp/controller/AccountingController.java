package com.example.newapp.controller;

import com.example.newapp.service.AccountingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpSession;

@Controller
@Slf4j
public class AccountingController {

    private final AccountingService accountingService;

    public AccountingController(AccountingService accountingService) {
        this.accountingService = accountingService;
    }

    @GetMapping("/accounting/invoices")
    public String listInvoices(Model model, HttpSession session) {
        log.debug("Récupération de la liste des factures");
        
        String sessionCookie = (String) session.getAttribute("sid");
        if (sessionCookie == null) {
            return "redirect:/login";
        }
        
        model.addAttribute("invoices", accountingService.getAllPurchaseInvoices(sessionCookie));
        return "accounting/invoices";
    }
} 