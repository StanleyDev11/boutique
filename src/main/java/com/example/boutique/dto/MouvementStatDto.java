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

    // Constructor to fix the Object mapping issue from the native query
    public MouvementStatDto(Object dateObject, Long count) {
        if (dateObject instanceof java.sql.Date) {
            this.date = ((java.sql.Date) dateObject).toLocalDate();
        } else if (dateObject instanceof java.sql.Timestamp) {
            this.date = ((java.sql.Timestamp) dateObject).toLocalDateTime().toLocalDate();
        } else if (dateObject instanceof LocalDate) {
            this.date = (LocalDate) dateObject;
        }
        this.count = (count != null) ? count : 0L;
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