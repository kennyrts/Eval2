package com.example.newapp.service;

import com.example.newapp.dto.SupplierDTO;
import com.example.newapp.dto.SupplierQuotationDTO;
import com.example.newapp.dto.SupplierQuotationItemDTO;
import com.example.newapp.dto.PurchaseOrderDTO;
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

    public SupplierQuotationDTO getQuotationDetails(String sessionCookie, String quotationName) {
        try {
            if (sessionCookie == null) {
                log.error("Cookie de session manquant");
                return null;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("Cookie", sessionCookie);

            HttpEntity<?> entity = new HttpEntity<>(headers);
            
            // URL directe vers le devis
            String url = erpUrl + "/api/resource/Supplier Quotation/" + quotationName;
            log.debug("URL de récupération du devis: {}", url);

            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                
                if (jsonResponse.has("data")) {
                    JsonNode quotationData = jsonResponse.get("data");
                    SupplierQuotationDTO quotation = new SupplierQuotationDTO();
                    quotation.setName(getStringValue(quotationData, "name"));
                    quotation.setSupplier(getStringValue(quotationData, "supplier"));
                    quotation.setSupplierName(getStringValue(quotationData, "supplier_name"));
                    quotation.setTransactionDate(getStringValue(quotationData, "transaction_date"));
                    quotation.setValidTill(getStringValue(quotationData, "valid_till"));
                    quotation.setGrandTotal(getStringValue(quotationData, "grand_total"));
                    quotation.setStatus(getStringValue(quotationData, "status"));
                    quotation.setTerms(getStringValue(quotationData, "terms"));
                    quotation.setTotalTaxesAndCharges(getStringValue(quotationData, "total_taxes_and_charges"));
                    quotation.setNetTotal(getStringValue(quotationData, "net_total"));

                    // Les items sont directement dans la réponse
                    List<SupplierQuotationItemDTO> items = new ArrayList<>();
                    if (quotationData.has("items")) {
                        for (JsonNode itemNode : quotationData.get("items")) {
                            SupplierQuotationItemDTO item = new SupplierQuotationItemDTO();
                            item.setItemCode(getStringValue(itemNode, "item_code"));
                            item.setItemName(getStringValue(itemNode, "item_name"));
                            item.setDescription(getStringValue(itemNode, "description"));
                            item.setQty(getDoubleValue(itemNode, "qty"));
                            item.setUom(getStringValue(itemNode, "uom"));
                            item.setRate(getDoubleValue(itemNode, "rate"));
                            item.setAmount(getDoubleValue(itemNode, "amount"));
                            item.setWarehouse(getStringValue(itemNode, "warehouse"));
                            item.setLeadTimeDays(getDoubleValue(itemNode, "lead_time_days"));
                            items.add(item);
                        }
                    }
                    quotation.setItems(items);

                    return quotation;
                }
            }

            return null;
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des détails du devis", e);
            return null;
        }
    }

    private String getStringValue(JsonNode node, String field) {
        return node.has(field) ? node.get(field).asText() : "";
    }

    private Double getDoubleValue(JsonNode node, String field) {
        return node.has(field) ? node.get(field).asDouble() : 0.0;
    }

    public boolean updateItemRate(String sessionCookie, String quotationName, String itemCode, Double newRate) {
        try {
            if (sessionCookie == null) {
                log.error("Cookie de session manquant");
                return false;
            }

            // D'abord, récupérer les détails actuels du devis pour avoir la quantité
            SupplierQuotationDTO quotation = getQuotationDetails(sessionCookie, quotationName);
            if (quotation == null || quotation.getItems() == null) {
                log.error("Impossible de récupérer les détails du devis");
                return false;
            }

            // Trouver l'item correspondant pour obtenir sa quantité
            Double qty = quotation.getItems().stream()
                .filter(item -> itemCode.equals(item.getItemCode()))
                .findFirst()
                .map(SupplierQuotationItemDTO::getQty)
                .orElse(null);

            if (qty == null) {
                log.error("Item {} non trouvé dans le devis {}", itemCode, quotationName);
                return false;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("Cookie", sessionCookie);

            // Construire le corps de la requête avec un objet JSON via ObjectMapper
            ObjectNode requestBody = objectMapper.createObjectNode();
            ArrayNode itemsArray = objectMapper.createArrayNode();
            ObjectNode itemNode = objectMapper.createObjectNode();
            itemNode.put("item_code", itemCode);
            itemNode.put("rate", newRate);
            itemNode.put("qty", qty);  // Inclure la quantité existante
            itemsArray.add(itemNode);
            requestBody.set("items", itemsArray);

            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);
            String url = erpUrl + "/api/resource/Supplier Quotation/" + quotationName;
            log.debug("URL de mise à jour du devis: {}", url);
            log.debug("Corps de la requête: {}", objectMapper.writeValueAsString(requestBody));

            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.PUT,
                entity,
                String.class
            );

            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            log.error("Erreur lors de la mise à jour du prix unitaire", e);
            return false;
        }
    }

    public List<PurchaseOrderDTO> getSupplierPurchaseOrders(String sessionCookie, String supplierName) {
        try {
            if (sessionCookie == null) {
                log.error("Cookie de session manquant");
                return new ArrayList<>();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("Cookie", sessionCookie);

            HttpEntity<?> entity = new HttpEntity<>(headers);
            String url = erpUrl + "/api/resource/Purchase Order?fields=[\"*\"]&filters=[[\"supplier\",\"=\",\"" + supplierName + "\"]]";
            log.debug("URL de récupération des commandes: {}", url);

            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                List<PurchaseOrderDTO> orders = new ArrayList<>();

                if (jsonResponse.has("data")) {
                    JsonNode data = jsonResponse.get("data");
                    for (JsonNode orderNode : data) {
                        PurchaseOrderDTO order = new PurchaseOrderDTO();
                        order.setName(getStringValue(orderNode, "name"));
                        order.setSupplier(getStringValue(orderNode, "supplier"));
                        order.setSupplierName(getStringValue(orderNode, "supplier_name"));
                        order.setTransactionDate(getStringValue(orderNode, "transaction_date"));
                        order.setStatus(getStringValue(orderNode, "status"));
                        order.setGrandTotal(getStringValue(orderNode, "grand_total"));
                        order.setCompany(getStringValue(orderNode, "company"));
                        order.setRequiredBy(getStringValue(orderNode, "schedule_date"));
                        orders.add(order);
                    }
                }

                return orders;
            }

            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des commandes", e);
            return new ArrayList<>();
        }
    }
} 