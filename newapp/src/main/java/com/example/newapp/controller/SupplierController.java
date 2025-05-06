package com.example.newapp.controller;

import com.example.newapp.service.SupplierService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
@Slf4j
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping("/suppliers")
    public String listSuppliers(Model model, HttpSession session, HttpServletRequest request) {
        log.debug("Récupération de la liste des fournisseurs");
        
        String sessionCookie = (String) session.getAttribute("sid");
        if (sessionCookie == null) {
            return "redirect:/login";
        }
        
        model.addAttribute("suppliers", supplierService.getAllSuppliers(sessionCookie));
        return "suppliers/list";
    }

    @GetMapping("/suppliers/{supplierName}/quotations")
    public String listSupplierQuotations(@PathVariable String supplierName, Model model, HttpSession session) {
        log.debug("Récupération des devis pour le fournisseur: {}", supplierName);
        
        String sessionCookie = (String) session.getAttribute("sid");
        if (sessionCookie == null) {
            return "redirect:/login";
        }
        
        model.addAttribute("supplier", supplierName);
        model.addAttribute("quotations", supplierService.getSupplierQuotations(sessionCookie, supplierName));
        return "suppliers/quotations";
    }

    @GetMapping("/suppliers/{supplierName}/orders")
    public String listSupplierOrders(@PathVariable String supplierName, Model model, HttpSession session) {
        log.debug("Récupération des commandes pour le fournisseur: {}", supplierName);
        
        String sessionCookie = (String) session.getAttribute("sid");
        if (sessionCookie == null) {
            return "redirect:/login";
        }
        
        model.addAttribute("supplier", supplierName);
        model.addAttribute("orders", supplierService.getSupplierPurchaseOrders(sessionCookie, supplierName));
        return "suppliers/orders";
    }

    @GetMapping("/suppliers/quotations/{quotationName}")
    public String showQuotationDetails(@PathVariable String quotationName, Model model, HttpSession session) {
        log.debug("Récupération des détails du devis: {}", quotationName);
        
        String sessionCookie = (String) session.getAttribute("sid");
        if (sessionCookie == null) {
            return "redirect:/login";
        }
        
        model.addAttribute("quotation", supplierService.getQuotationDetails(sessionCookie, quotationName));
        return "suppliers/quotation-details";
    }

    @PostMapping("/suppliers/quotations/{quotationName}/updateRate")
    public String updateItemRate(
            @PathVariable String quotationName,
            @RequestParam String itemCode,
            @RequestParam Double newRate,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        
        log.debug("Mise à jour du prix unitaire pour l'item {} du devis {}: {}", itemCode, quotationName, newRate);
        
        String sessionCookie = (String) session.getAttribute("sid");
        if (sessionCookie == null) {
            return "redirect:/login";
        }
        
        boolean success = supplierService.updateItemRate(sessionCookie, quotationName, itemCode, newRate);
        
        if (success) {
            redirectAttributes.addFlashAttribute("success", 
                "Prix unitaire mis à jour et devis soumis automatiquement avec succès");
        } else {
            redirectAttributes.addFlashAttribute("error", 
                "Erreur lors de la mise à jour du prix unitaire ou de la soumission du devis");
        }
        
        return "redirect:/suppliers/quotations/" + quotationName;
    }
} 