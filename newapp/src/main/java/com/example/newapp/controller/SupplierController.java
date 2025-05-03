package com.example.newapp.controller;

import com.example.newapp.service.SupplierService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
} 