package com.example.newapp.dto;

import lombok.Data;

@Data
public class CalendarEventDTO {
    private String id;
    private String title;
    private String start;
    private String end;
    private String doctype;
    private String docname;
    private String status;
    private String lastModified;
    private String created;
    private boolean allDay;
} 