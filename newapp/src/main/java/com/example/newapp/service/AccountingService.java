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
import com.fasterxml.jackson.databind.node.ArrayNode;

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
    
            // Construction du corps de la requête avec la structure correcte compatible avec la soumission
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("doctype", "Payment Entry");
            requestBody.put("payment_type", "Pay");
            requestBody.put("posting_date", paymentDTO.getPaymentDate());
            requestBody.put("company", "ITUniversity");
            requestBody.put("mode_of_payment", paymentDTO.getPaymentMode() != null ? paymentDTO.getPaymentMode() : "Cash");
            requestBody.put("party_type", "Supplier");
            requestBody.put("party", paymentDTO.getSupplier());
            
            // Utiliser des comptes corrects
            requestBody.put("paid_from", "Cash - ITU");
            requestBody.put("paid_from_account_currency", "EUR");
            requestBody.put("paid_to", "Creditors - ITU");
            requestBody.put("paid_to_account_currency", "EUR");
            
            // Montants et taux de change
            requestBody.put("received_amount", paymentDTO.getPaymentAmount());
            requestBody.put("paid_amount", paymentDTO.getPaymentAmount());
            requestBody.put("source_exchange_rate", 1.0);
            requestBody.put("target_exchange_rate", 1.0);
            
            // Référence à la facture
            ArrayNode referencesArray = objectMapper.createArrayNode();
            ObjectNode reference = objectMapper.createObjectNode();
            reference.put("reference_doctype", "Purchase Invoice");
            reference.put("reference_name", paymentDTO.getInvoiceId());
            reference.put("allocated_amount", paymentDTO.getPaymentAmount());
            referencesArray.add(reference);
            requestBody.set("references", referencesArray);
            
            // Enregistrer le numéro de référence (numéro de chèque, etc.) s'il est fourni
            if (paymentDTO.getReference() != null && !paymentDTO.getReference().isEmpty()) {
                requestBody.put("reference_no", paymentDTO.getReference());
                requestBody.put("reference_date", paymentDTO.getPaymentDate());
            }
    
            log.debug("Corps de la requête de création de paiement: {}", objectMapper.writeValueAsString(requestBody));
            
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);
            String url = erpUrl + "/api/resource/Payment Entry";
    
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                String.class
            );
    
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                if (jsonResponse.has("data") && jsonResponse.get("data").has("name")) {
                    String paymentEntryName = jsonResponse.get("data").get("name").asText();
                    log.info("Payment Entry créé avec succès: {}", paymentEntryName);
                    return submitPaymentEntry(sessionCookie, paymentEntryName);
                }
            }
    
            log.error("Erreur lors de la création du paiement: {}", response.getStatusCode());
            return false;
        } catch (Exception e) {
            log.error("Erreur lors de la création du paiement", e);
            return false;
        }
    }

    private boolean submitPaymentEntry(String sessionCookie, String paymentEntryName) {
        try {
            log.debug("Soumission du Payment Entry: {}", paymentEntryName);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("Cookie", sessionCookie);

            // Créer un corps de requête avec docstatus = 1 pour la soumission
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("docstatus", 1);
            
            // Créer l'entité HTTP avec le corps de la requête
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);
            
            // URL directe pour mettre à jour le document avec PUT
            String url = erpUrl + "/api/resource/Payment Entry/" + paymentEntryName;
            
            log.debug("Appel de l'API de soumission: {}", url);
            
            // Utiliser PUT pour mettre à jour le document avec docstatus=1
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.PUT,
                entity,
                String.class
            );
            
            log.debug("Réponse de soumission: {}", response.getStatusCode());
            if (response.getBody() != null) {
                log.debug("Corps de réponse: {}", response.getBody());
            }

            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("Erreur lors de la soumission du paiement {}: {}", paymentEntryName, e.getMessage());
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