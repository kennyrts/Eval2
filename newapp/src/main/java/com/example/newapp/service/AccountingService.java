package com.example.newapp.service;

import com.example.newapp.dto.PurchaseInvoiceDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import lombok.extern.slf4j.Slf4j;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class AccountingService {

    @Value("${erpnext.api.url}")
    private String erpUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public AccountingService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public List<PurchaseInvoiceDTO> getAllPurchaseInvoices(String sessionCookie) {
        try {
            if (sessionCookie == null) {
                log.error("Cookie de session manquant");
                return new ArrayList<>();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("Cookie", sessionCookie);

            HttpEntity<?> entity = new HttpEntity<>(headers);
            String url = erpUrl + "/api/resource/Purchase Invoice?fields=[\"*\"]";
            log.debug("URL de récupération des factures: {}", url);

            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                List<PurchaseInvoiceDTO> invoices = new ArrayList<>();

                if (jsonResponse.has("data")) {
                    JsonNode data = jsonResponse.get("data");
                    for (JsonNode invoiceNode : data) {
                        PurchaseInvoiceDTO invoice = new PurchaseInvoiceDTO();
                        invoice.setName(getStringValue(invoiceNode, "name"));
                        invoice.setSupplier(getStringValue(invoiceNode, "supplier"));
                        invoice.setSupplierName(getStringValue(invoiceNode, "supplier_name"));
                        invoice.setPostingDate(getStringValue(invoiceNode, "posting_date"));
                        invoice.setDueDate(getStringValue(invoiceNode, "due_date"));
                        invoice.setStatus(getStringValue(invoiceNode, "status"));
                        invoice.setGrandTotal(getStringValue(invoiceNode, "grand_total"));
                        invoice.setOutstandingAmount(getStringValue(invoiceNode, "outstanding_amount"));
                        invoice.setBillNo(getStringValue(invoiceNode, "bill_no"));
                        invoice.setCompany(getStringValue(invoiceNode, "company"));
                        invoices.add(invoice);
                    }
                }

                return invoices;
            }

            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des factures", e);
            return new ArrayList<>();
        }
    }

    private String getStringValue(JsonNode node, String field) {
        return node.has(field) ? node.get(field).asText() : "";
    }
} 