package com.example.newapp.service;

import com.example.newapp.dto.LoginRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ErpAuthService {

    @Value("${erpnext.api.url}")
    private String erpUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public ErpAuthService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public boolean authenticate(String username, String password) {
        try {
            log.info("Tentative d'authentification pour l'utilisateur: {}", username);
            
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setUsr(username);
            loginRequest.setPwd(password);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<LoginRequest> request = new HttpEntity<>(loginRequest, headers);
            
            String loginUrl = erpUrl + "/api/method/login";
            log.debug("URL d'authentification: {}", loginUrl);

            ResponseEntity<String> response = restTemplate.postForEntity(
                loginUrl,
                request,
                String.class
            );

            log.debug("Code de statut de la réponse: {}", response.getStatusCode());
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                log.debug("Réponse complète: {}", jsonResponse);
                
                // Vérifier si la réponse contient un message de succès
                if (jsonResponse.has("message") && "Logged In".equals(jsonResponse.get("message").asText())) {
                    log.info("Authentification réussie pour l'utilisateur: {}", username);
                    return true;
                }
                
                // Si le message n'est pas "Logged In", c'est une erreur
                if (jsonResponse.has("message")) {
                    String message = jsonResponse.get("message").asText();
                    log.error("Erreur d'authentification: {}", message);
                }
                
                return false;
            }
            
            return false;
        } catch (Exception e) {
            log.error("Erreur lors de l'authentification", e);
            return false;
        }
    }
} 