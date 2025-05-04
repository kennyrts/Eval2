package com.example.newapp.controller;

import com.example.newapp.service.AccountingService;
import com.example.newapp.dto.PaymentEntryDTO;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
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

    @PostMapping("/accounting/payments")
    public String createPayment(@ModelAttribute PaymentEntryDTO paymentDTO,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        log.debug("Création d'un nouveau paiement pour la facture: {}", paymentDTO.getInvoiceId());
        
        String sessionCookie = (String) session.getAttribute("sid");
        if (sessionCookie == null) {
            return "redirect:/login";
        }
        
        boolean success = accountingService.createPaymentEntry(sessionCookie, paymentDTO);
        
        if (success) {
            redirectAttributes.addFlashAttribute("success", "Paiement enregistré avec succès");
        } else {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de l'enregistrement du paiement");
        }
        
        return "redirect:/accounting/invoices";
    }
} 