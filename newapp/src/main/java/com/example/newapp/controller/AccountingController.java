package com.example.newapp.controller;

import com.example.newapp.service.AccountingService;
import com.example.newapp.dto.PaymentEntryDTO;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ContentDisposition;
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
        
        try {
            boolean success = accountingService.createPaymentEntry(sessionCookie, paymentDTO);
            
            if (success) {
                redirectAttributes.addFlashAttribute("success", "Paiement enregistré avec succès");
            } else {
                redirectAttributes.addFlashAttribute("error", "Erreur lors de l'enregistrement du paiement");
            }
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        
        return "redirect:/accounting/invoices";
    }

    @GetMapping("/accounting/invoices/{invoiceName}/pdf")
    public ResponseEntity<byte[]> downloadPurchaseInvoicePdf(
            @PathVariable String invoiceName,
            HttpSession session) {
        log.debug("Téléchargement du PDF pour la facture: {}", invoiceName);
        
        String sessionCookie = (String) session.getAttribute("sid");
        if (sessionCookie == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        byte[] pdf = accountingService.downloadPurchaseInvoicePdf(sessionCookie, invoiceName);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename(invoiceName + ".pdf").build());
        
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }
} 