package com.example.newapp.dto;

import lombok.Data;

@Data
public class PurchaseOrderDTO {
    private String name;
    private String supplier;
    private String supplierName;
    private String transactionDate;
    private String status;
    private String grandTotal;
    private String company;
    private String requiredBy;
} 