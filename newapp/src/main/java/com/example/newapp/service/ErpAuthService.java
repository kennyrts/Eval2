package com.example.newapp.service;

import com.example.newapp.dto.LoginRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import lombok.extern.slf4j.Slf4j;
import java.util.List;

@Service
@Slf4j
public class ErpAuthService {

    @Value("${erpnext.api.url}")
    private String erpUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private String sessionId;

    public ErpAuthService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public boolean authenticate(String username, String password) {
        try {
            log.info("Tentative d'authentification pour l'utilisateur: {}", username);
            LoginRequest loginRequest = new LoginRequest(username, password);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<LoginRequest> entity = new HttpEntity<>(loginRequest, headers);
            String url = erpUrl + "/api/method/login";
            log.debug("URL d'authentification: {}", url);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            log.debug("Code de statut de la réponse: {}", response.getStatusCode());

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                String message = jsonResponse.path("message").asText();
                log.debug("Réponse complète: {}", response.getBody());

                // Extraire le cookie de session
                List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
                if (cookies != null) {
                    for (String cookie : cookies) {
                        if (cookie.startsWith("sid=")) {
                            sessionId = cookie.split(";")[0].substring(4);
                            log.debug("Session ID extrait: {}", sessionId);
                            break;
                        }
                    }
                }

                return "Logged In".equals(message);
            }

            return false;
        } catch (Exception e) {
            log.error("Erreur lors de l'authentification", e);
            return false;
        }
    }

    public String getSessionId() {
        return sessionId;
    }
} 