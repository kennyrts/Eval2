package com.example.newapp.dto;

import lombok.Data;
import java.util.List;

@Data
public class SupplierQuotationDTO {
    private String name;
    private String supplier;
    private String supplierName;
    private String transactionDate;
    private String validTill;
    private String grandTotal;
    private String status;
    private String terms;
    private String totalTaxesAndCharges;
    private String netTotal;
    private List<SupplierQuotationItemDTO> items;
} 