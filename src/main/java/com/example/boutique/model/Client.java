package com.example.boutique.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "clients")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nom;

    @Column(unique = true)
    private String telephone;

    private String adresse;

    @Column(nullable = false, updatable = false)
    private LocalDate dateInscription;

    @Column(nullable = false)
    private java.math.BigDecimal soldeCredit = java.math.BigDecimal.ZERO;

    @PrePersist
    protected void onCreate() {
        if (dateInscription == null) {
            dateInscription = LocalDate.now();
        }
    }
}
