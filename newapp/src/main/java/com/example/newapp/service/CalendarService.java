package com.example.newapp.service;

import com.example.newapp.dto.CalendarEventDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class CalendarService {
    
    @Value("${erpnext.api.url}")
    private String erpNextUrl;
    
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ErpAuthService erpAuthService;
    
    public CalendarService(ErpAuthService erpAuthService) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.erpAuthService = erpAuthService;
    }
    
    public List<CalendarEventDTO> getCalendarEvents(LocalDate start, LocalDate end, String documentType) {
        String url = String.format("%s/api/method/erpnext.calendar.api.get_purchase_calendar_events" +
                "?start=%s&end=%s&document_type=%s",
                erpNextUrl, start, end, documentType);
        
        var response = restTemplate.getForEntity(url, String.class);
        List<CalendarEventDTO> events = new ArrayList<>();
        
        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode eventsNode = root.path("message").path("events");
            
            if (eventsNode.isArray()) {
                for (JsonNode eventNode : eventsNode) {
                    CalendarEventDTO event = objectMapper.treeToValue(eventNode, CalendarEventDTO.class);
                    events.add(event);
                }
            }
        } catch (Exception e) {
            log.error("Error parsing calendar events", e);
        }
        
        return events;
    }
} 