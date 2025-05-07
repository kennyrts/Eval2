package com.example.newapp.controller;

import com.example.newapp.service.AleaService;
import com.example.newapp.dto.SupplierQuotationDTO;
import com.example.newapp.dto.SupplierQuotationItemDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpSession;

import java.util.ArrayList;
import java.util.List;

@Controller
@Slf4j
@RequestMapping("/alea")
public class AleaController {

    private final AleaService aleaService;

    public AleaController(AleaService aleaService) {
        this.aleaService = aleaService;
    }

    @GetMapping("/form")
    public String showQuotationForm(Model model, HttpSession session) {
        log.debug("Affichage du formulaire de création de devis fournisseur");
        
        String sessionCookie = (String) session.getAttribute("sid");
        if (sessionCookie == null) {
            return "redirect:/login";
        }
        
        // Récupérer les données nécessaires pour les select
        model.addAttribute("suppliers", aleaService.getAllSuppliers(sessionCookie));
        model.addAttribute("items", aleaService.getAllItems(sessionCookie));
        model.addAttribute("warehouses", aleaService.getAllWarehouses(sessionCookie));
        
        // Créer un objet vide pour le formulaire
        model.addAttribute("quotation", new SupplierQuotationDTO());
        model.addAttribute("quotationItem", new SupplierQuotationItemDTO());
        
        return "alea/form";
    }

    @PostMapping("/submit-quotation")
    public String submitQuotation(
            @RequestParam String supplier,
            @RequestParam String supplierDate,
            @RequestParam String requiredByDate,
            @RequestParam List<String> itemName,
            @RequestParam List<Double> quantity,
            @RequestParam List<String> warehouse,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        
        log.debug("Soumission d'un nouveau devis fournisseur pour {}", supplier);
        
        String sessionCookie = (String) session.getAttribute("sid");
        if (sessionCookie == null) {
            return "redirect:/login";
        }
        
        List<SupplierQuotationItemDTO> items = new ArrayList<>();
        for (int i = 0; i < itemName.size(); i++) {
            if (i < quantity.size() && i < warehouse.size()) {
                SupplierQuotationItemDTO item = new SupplierQuotationItemDTO();
                item.setItemName(itemName.get(i));
                item.setQty(quantity.get(i));
                item.setWarehouse(warehouse.get(i));
                items.add(item);
            }
        }
        
        if (items.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Vous devez spécifier au moins un article");
            return "redirect:/alea/form";
        }
        
        SupplierQuotationDTO quotation = new SupplierQuotationDTO();
        quotation.setSupplier(supplier);
        quotation.setTransactionDate(supplierDate);
        quotation.setValidTill(requiredByDate);
        quotation.setItems(items);
        
        String quotationName = aleaService.createSupplierQuotation(sessionCookie, quotation);
        
        if (quotationName != null) {
            redirectAttributes.addFlashAttribute("success", "Devis fournisseur créé avec succès: " + quotationName);
            return "redirect:/suppliers/quotations/" + quotationName;
        } else {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de la création du devis fournisseur");
            return "redirect:/alea/form";
        }
    }
    
    @GetMapping("/items")
    @ResponseBody
    public ResponseEntity<List<String>> getItems(HttpSession session) {
        String sessionCookie = (String) session.getAttribute("sid");
        if (sessionCookie == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        List<String> items = aleaService.getAllItemNames(sessionCookie);
        return ResponseEntity.ok(items);
    }
    
    @GetMapping("/warehouses")
    @ResponseBody
    public ResponseEntity<List<String>> getWarehouses(HttpSession session) {
        String sessionCookie = (String) session.getAttribute("sid");
        if (sessionCookie == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        List<String> warehouses = aleaService.getAllWarehouseNames(sessionCookie);
        return ResponseEntity.ok(warehouses);
    }
} 