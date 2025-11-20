package com.example.boutique.dto;



import java.time.LocalDate;

import java.sql.Date; // Import java.sql.Date

import java.time.LocalDateTime;



public class MouvementStatDto {

    private LocalDate date;

    private Long count;



    // Default constructor for JPA

    public MouvementStatDto() {

    }



    public MouvementStatDto(LocalDate date, Long count) {

        this.date = date;

        this.count = count;

    }



    // New constructor to handle java.sql.Date from the query

    public MouvementStatDto(Date sqlDate, Long count) {

        this.date = sqlDate.toLocalDate(); // Convert to LocalDate

        this.count = count;

    }



    public MouvementStatDto(LocalDateTime dateTime, Long count) {

        this.date = dateTime.toLocalDate();

        this.count = count;

    }



    // Getters and Setters

    public LocalDate getDate() {

        return date;

    }



    public void setDate(LocalDate date) {

        this.date = date;

    }



    public Long getCount() {

        return count;

    }



    public void setCount(Long count) {

        this.count = count;

    }

}
