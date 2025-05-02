package com.example.newapp.controller;

import com.example.newapp.service.SupplierService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping("/suppliers")
    public String listSuppliers(Model model) {
        log.debug("Récupération de la liste des fournisseurs");
        model.addAttribute("suppliers", supplierService.getAllSuppliers());
        return "suppliers/list";
    }
} 