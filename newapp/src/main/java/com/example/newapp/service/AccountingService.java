package com.example.newapp.service;

import com.example.newapp.dto.PurchaseInvoiceDTO;
import com.example.newapp.dto.PaymentEntryDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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

    private String getDefaultCashAccount(String sessionCookie) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("Cookie", sessionCookie);

            HttpEntity<?> entity = new HttpEntity<>(headers);
            String url = erpUrl + "/api/resource/Company/TEST/default_cash_account";

            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                if (jsonResponse.has("data")) {
                    return jsonResponse.get("data").asText();
                }
            }
            return "Cash - TEST"; // Fallback
        } catch (Exception e) {
            log.error("Erreur lors de la récupération du compte de caisse par défaut", e);
            return "Cash - TEST"; // Fallback
        }
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

    public boolean createPaymentEntry(String sessionCookie, PaymentEntryDTO paymentDTO) {
        try {
            if (sessionCookie == null) {
                log.error("Cookie de session manquant");
                return false;
            }
    
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("Cookie", sessionCookie);
    
            ObjectNode requestBody = objectMapper.createObjectNode();
            // Ajout des champs exactement comme dans le JSON qui fonctionne
            requestBody.put("mode_of_payment", "Cash");
            requestBody.put("posting_date", paymentDTO.getPaymentDate());
            requestBody.put("target_exchange_rate", 1.0);
            requestBody.put("party_type", "Supplier");
            requestBody.put("party", paymentDTO.getSupplier());
            requestBody.put("paid_to", "Creditors - ITU");
            requestBody.put("paid_to_account_currency", "EUR");
            requestBody.put("paid_amount", paymentDTO.getPaymentAmount());
            requestBody.put("received_amount", paymentDTO.getPaymentAmount());
            requestBody.put("paid_from", "Creditors - ITU");
            requestBody.put("paid_from_account_currency", "EUR");
            requestBody.put("payment_type", "Pay");
            requestBody.put("company", "ITUniversity");
    
            // Création du tableau references
            ObjectNode reference = objectMapper.createObjectNode();
            reference.put("reference_doctype", "Purchase Invoice");
            reference.put("reference_name", paymentDTO.getInvoiceId());
            reference.put("total_amount", paymentDTO.getPaymentAmount());
            reference.put("outstanding_amount", paymentDTO.getPaymentAmount());
            reference.put("allocated_amount", paymentDTO.getPaymentAmount());
            
            requestBody.putArray("references").add(reference);
    
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);
            String url = erpUrl + "/api/resource/Payment Entry";
    
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                String.class
            );
    
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                if (jsonResponse.has("data")) {
                    String paymentEntryName = jsonResponse.get("data").get("name").asText();
                    return submitPaymentEntry(sessionCookie, paymentEntryName);
                }
            }
    
            return false;
        } catch (Exception e) {
            log.error("Erreur lors de la création du paiement", e);
            return false;
        }
    }

    private boolean submitPaymentEntry(String sessionCookie, String paymentEntryName) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("Cookie", sessionCookie);

            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("docstatus", 1);

            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);
            String url = erpUrl + "/api/resource/Payment Entry/" + paymentEntryName + "/submit";

            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                String.class
            );

            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            log.error("Erreur lors de la soumission du paiement {}", paymentEntryName, e);
            return false;
        }
    }

    public byte[] downloadPurchaseInvoicePdf(String sessionCookie, String invoiceName) {
        String url = erpUrl + "/api/method/frappe.utils.print_format.download_pdf";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Cookie", sessionCookie);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("doctype", "Purchase Invoice");
        map.add("name", invoiceName);
        map.add("format", "Standard");
        map.add("no_letterhead", "0");
        
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
        
        ResponseEntity<byte[]> response = restTemplate.exchange(
            url,
            HttpMethod.POST,
            request,
            byte[].class
        );
        
        return response.getBody();
    }
} 