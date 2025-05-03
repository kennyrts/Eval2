package com.example.newapp.dto;

import lombok.Data;

@Data
public class SupplierQuotationItemDTO {
    private String itemCode;
    private String itemName;
    private String description;
    private Double qty;
    private String uom;
    private Double rate;
    private Double amount;
    private String warehouse;
    private Double leadTimeDays;
} 