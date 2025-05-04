package com.example.newapp.dto;

import lombok.Data;

@Data
public class PaymentEntryDTO {
    private String invoiceId;
    private String supplier;
    private String paymentDate;
    private String paymentMode;
    private Double paymentAmount;
    private String reference;
} 