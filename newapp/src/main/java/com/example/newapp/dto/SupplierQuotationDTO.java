package com.example.newapp.dto;

import lombok.Data;

@Data
public class SupplierQuotationDTO {
    private String name;
    private String supplier;
    private String supplierName;
    private String transactionDate;
    private String validTill;
    private String grandTotal;
    private String status;
} 