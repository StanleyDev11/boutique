package com.example.boutique.dto;

import java.time.LocalDate;
import java.sql.Date; // Import java.sql.Date

public class MouvementStatDto {
    private LocalDate date;
    private long count;

    // Default constructor for JPA
    public MouvementStatDto() {
    }

    public MouvementStatDto(LocalDate date, long count) {
        this.date = date;
        this.count = count;
    }

    // New constructor to handle java.sql.Date from the query
    public MouvementStatDto(Date sqlDate, long count) {
        this.date = sqlDate.toLocalDate(); // Convert to LocalDate
        this.count = count;
    }

    // Fallback constructor to handle cases where Hibernate passes generic Object/Number types
    public MouvementStatDto(Object dateObj, Number countObj) {
        if (dateObj instanceof java.sql.Date) {
            this.date = ((java.sql.Date) dateObj).toLocalDate();
        } else if (dateObj instanceof java.time.LocalDate) {
            this.date = (java.time.LocalDate) dateObj;
        } else if (dateObj instanceof java.time.LocalDateTime) {
            this.date = ((java.time.LocalDateTime) dateObj).toLocalDate();
        } else if (dateObj != null) {
            // Try parsing from string as last resort
            this.date = java.time.LocalDate.parse(dateObj.toString());
        }

        if (countObj != null) {
            this.count = countObj.longValue();
        } else {
            this.count = 0L;
        }
    }

    // Getters and Setters
    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}