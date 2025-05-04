package com.example.newapp.dto;

import lombok.Data;

@Data
public class PurchaseInvoiceDTO {
    private String name;
    private String supplier;
    private String supplierName;
    private String postingDate;
    private String dueDate;
    private String status;
    private String grandTotal;
    private String outstandingAmount;
    private String billNo;
    private String company;
} 