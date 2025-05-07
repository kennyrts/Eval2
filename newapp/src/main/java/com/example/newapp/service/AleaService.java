package com.example.newapp.service;

import com.example.newapp.dto.SupplierDTO;
import com.example.newapp.dto.SupplierQuotationDTO;
import com.example.newapp.dto.SupplierQuotationItemDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AleaService {

    @Value("${erpnext.api.url}")
    private String erpUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public AleaService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
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

    public List<Map<String, String>> getAllItems(String sessionCookie) {
        try {
            if (sessionCookie == null) {
                log.error("Cookie de session manquant");
                return new ArrayList<>();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("Cookie", sessionCookie);

            HttpEntity<?> entity = new HttpEntity<>(headers);
            String url = erpUrl + "/api/resource/Item?fields=[\"name\", \"item_name\", \"description\"]";
            log.debug("URL de récupération des items: {}", url);

            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                List<Map<String, String>> items = new ArrayList<>();

                if (jsonResponse.has("data")) {
                    JsonNode data = jsonResponse.get("data");
                    for (JsonNode itemNode : data) {
                        Map<String, String> item = new HashMap<>();
                        item.put("code", getStringValue(itemNode, "name"));
                        item.put("name", getStringValue(itemNode, "item_name"));
                        item.put("description", getStringValue(itemNode, "description"));
                        items.add(item);
                    }
                }

                return items;
            }

            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des items", e);
            return new ArrayList<>();
        }
    }

    public List<String> getAllItemNames(String sessionCookie) {
        List<Map<String, String>> items = getAllItems(sessionCookie);
        List<String> itemNames = new ArrayList<>();
        for (Map<String, String> item : items) {
            itemNames.add(item.get("name"));
        }
        return itemNames;
    }

    public List<Map<String, String>> getAllWarehouses(String sessionCookie) {
        try {
            if (sessionCookie == null) {
                log.error("Cookie de session manquant");
                return new ArrayList<>();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("Cookie", sessionCookie);

            HttpEntity<?> entity = new HttpEntity<>(headers);
            String url = erpUrl + "/api/resource/Warehouse?fields=[\"name\", \"warehouse_name\"]";
            log.debug("URL de récupération des entrepôts: {}", url);

            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                List<Map<String, String>> warehouses = new ArrayList<>();

                if (jsonResponse.has("data")) {
                    JsonNode data = jsonResponse.get("data");
                    for (JsonNode warehouseNode : data) {
                        Map<String, String> warehouse = new HashMap<>();
                        warehouse.put("name", getStringValue(warehouseNode, "name"));
                        warehouse.put("warehouseName", getStringValue(warehouseNode, "warehouse_name"));
                        warehouses.add(warehouse);
                    }
                }

                return warehouses;
            }

            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des entrepôts", e);
            return new ArrayList<>();
        }
    }

    public List<String> getAllWarehouseNames(String sessionCookie) {
        List<Map<String, String>> warehouses = getAllWarehouses(sessionCookie);
        List<String> warehouseNames = new ArrayList<>();
        for (Map<String, String> warehouse : warehouses) {
            warehouseNames.add(warehouse.get("name"));
        }
        return warehouseNames;
    }

    public String createSupplierQuotation(String sessionCookie, SupplierQuotationDTO quotation) {
        try {
            if (sessionCookie == null) {
                log.error("Cookie de session manquant");
                return null;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("Cookie", sessionCookie);

            // Construire le corps de la requête
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("doctype", "Supplier Quotation");
            requestBody.put("supplier", quotation.getSupplier());
            requestBody.put("transaction_date", quotation.getTransactionDate());
            requestBody.put("valid_till", quotation.getValidTill());
            
            // Ajouter les items
            ArrayNode itemsArray = objectMapper.createArrayNode();
            for (SupplierQuotationItemDTO item : quotation.getItems()) {
                ObjectNode itemNode = objectMapper.createObjectNode();
                itemNode.put("doctype", "Supplier Quotation Item");
                itemNode.put("item_code", item.getItemName());
                itemNode.put("qty", item.getQty());
                itemNode.put("warehouse", item.getWarehouse());                
                itemsArray.add(itemNode);
            }
            requestBody.set("items", itemsArray);

            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);
            String url = erpUrl + "/api/resource/Supplier Quotation";
            log.debug("URL de création de devis: {}", url);
            log.debug("Corps de la requête: {}", objectMapper.writeValueAsString(requestBody));

            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                if (jsonResponse.has("data") && jsonResponse.get("data").has("name")) {
                    String quotationName = jsonResponse.get("data").get("name").asText();
                    log.info("Devis fournisseur créé avec succès: {}", quotationName);
                    return quotationName;
                }
            }

            log.error("Erreur lors de la création du devis fournisseur: {}", response.getStatusCode());
            return null;
        } catch (Exception e) {
            log.error("Erreur lors de la création du devis fournisseur", e);
            return null;
        }
    }

    private String getStringValue(JsonNode node, String field) {
        return node.has(field) ? node.get(field).asText() : "";
    }
} 