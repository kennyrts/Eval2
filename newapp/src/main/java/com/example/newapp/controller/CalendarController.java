package com.example.newapp.controller;

import com.example.newapp.dto.CalendarEventDTO;
import com.example.newapp.service.CalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
@RequestMapping("/calendar")
@RequiredArgsConstructor
public class CalendarController {
    
    private final CalendarService calendarService;
    
    @GetMapping
    public String showCalendar() {
        return "calendar";
    }
    
    @GetMapping("/api/events")
    @ResponseBody
    public List<CalendarEventDTO> getEvents(
            @RequestParam String start,
            @RequestParam String end,
            @RequestParam(defaultValue = "All") String documentType,
            HttpServletRequest request) {
        
        String sessionCookie = request.getHeader("Cookie");
        return calendarService.getCalendarEvents(start, end, documentType, sessionCookie);
    }
} 