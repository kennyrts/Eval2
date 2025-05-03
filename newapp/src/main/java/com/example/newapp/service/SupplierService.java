package com.example.newapp.service;

import com.example.newapp.dto.SupplierDTO;
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

    private String getStringValue(JsonNode node, String fieldName) {
        return node.has(fieldName) ? node.get(fieldName).asText() : "";
    }
} 