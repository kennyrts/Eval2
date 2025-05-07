package com.example.newapp.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDate;

@Data
public class CalendarEventDTO {
    private String id;
    private String title;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate start;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate end;
    
    private String doctype;
    private String docname;
    private String status;
    private String lastModified;
    private String created;
    private boolean allDay;
} 