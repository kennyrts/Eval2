package com.example.newapp.service;

import com.example.newapp.dto.SupplierDTO;
import com.example.newapp.dto.SupplierQuotationDTO;
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
public class SupplierService {

    @Value("${erpnext.api.url}")
    private String erpUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ErpAuthService erpAuthService;

    public SupplierService(ErpAuthService erpAuthService) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.erpAuthService = erpAuthService;
    }

    public List<SupplierDTO> getAllSuppliers(String sessionCookie) {
        try {
            if (sessionCookie == null) {
                log.error("Cookie de session manquant");
                return new ArrayList<>();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("Cookie", sessionCookie);

            HttpEntity<?> entity = new HttpEntity<>(headers);
            String url = erpUrl + "/api/resource/Supplier?fields=[\"*\"]";
            log.debug("URL de récupération des fournisseurs: {}", url);

            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                List<SupplierDTO> suppliers = new ArrayList<>();

                if (jsonResponse.has("data")) {
                    JsonNode data = jsonResponse.get("data");
                    for (JsonNode supplierNode : data) {
                        SupplierDTO supplier = new SupplierDTO();
                        supplier.setName(getStringValue(supplierNode, "name"));
                        supplier.setSupplierName(getStringValue(supplierNode, "supplier_name"));
                        supplier.setSupplierType(getStringValue(supplierNode, "supplier_type"));
                        supplier.setCountry(getStringValue(supplierNode, "country"));
                        supplier.setSupplierGroup(getStringValue(supplierNode, "supplier_group"));
                        supplier.setStatus(getStringValue(supplierNode, "status"));
                        suppliers.add(supplier);
                    }
                }

                return suppliers;
            }

            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des fournisseurs", e);
            return new ArrayList<>();
        }
    }

    public List<SupplierQuotationDTO> getSupplierQuotations(String sessionCookie, String supplierName) {
        try {
            if (sessionCookie == null) {
                log.error("Cookie de session manquant");
                return new ArrayList<>();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("Cookie", sessionCookie);

            HttpEntity<?> entity = new HttpEntity<>(headers);
            String url = erpUrl + "/api/resource/Supplier Quotation?fields=[\"*\"]&filters=[[\"supplier\",\"=\",\"" + supplierName + "\"]]";
            log.debug("URL de récupération des devis fournisseur: {}", url);

            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                List<SupplierQuotationDTO> quotations = new ArrayList<>();

                if (jsonResponse.has("data")) {
                    JsonNode data = jsonResponse.get("data");
                    for (JsonNode quotationNode : data) {
                        SupplierQuotationDTO quotation = new SupplierQuotationDTO();
                        quotation.setName(getStringValue(quotationNode, "name"));
                        quotation.setSupplier(getStringValue(quotationNode, "supplier"));
                        quotation.setSupplierName(getStringValue(quotationNode, "supplier_name"));
                        quotation.setTransactionDate(getStringValue(quotationNode, "transaction_date"));
                        quotation.setValidTill(getStringValue(quotationNode, "valid_till"));
                        quotation.setGrandTotal(getStringValue(quotationNode, "grand_total"));
                        quotation.setStatus(getStringValue(quotationNode, "status"));
                        quotations.add(quotation);
                    }
                }

                return quotations;
            }

            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des devis fournisseur", e);
            return new ArrayList<>();
        }
    }

    private String getStringValue(JsonNode node, String field) {
        return node.has(field) ? node.get(field).asText() : "";
    }
} 