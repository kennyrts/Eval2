package com.example.newapp.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FileService {

    @Value("${erpnext.api.url}")
    private String erpUrl;

    private final RestTemplate restTemplate;

    public FileService() {
        this.restTemplate = new RestTemplate();
    }

    public String uploadFile(MultipartFile file, String sessionCookie) {
        try {
            if (sessionCookie == null) {
                log.error("Cookie de session manquant");
                return "Error: Cookie de session manquant";
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.add("Cookie", sessionCookie);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            });

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            String url = erpUrl + "/api/method/erpnext.fichier.page.fichier.fichier.process_file";
            log.debug("Uploading file to: {}", url);

            ResponseEntity<String> response = restTemplate.postForEntity(
                url,
                requestEntity,
                String.class
            );

            return response.getBody();
        } catch (Exception e) {
            log.error("Error uploading file", e);
            return "Error: " + e.getMessage();
        }
    }
} 