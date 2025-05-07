package com.example.newapp.controller;

import com.example.newapp.dto.CalendarEventDTO;
import com.example.newapp.service.CalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
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
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(defaultValue = "All") String documentType) {
        return calendarService.getCalendarEvents(start, end, documentType);
    }
} 