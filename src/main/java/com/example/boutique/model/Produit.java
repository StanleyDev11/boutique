package com.example.boutique.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "produits")
public class Produit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(unique = true)
    private String codeBarres;

    private BigDecimal prixAchat;

    @Column(nullable = false)
    private BigDecimal prixVenteUnitaire;

    private String categorie;

    @Column(nullable = false)
    private int quantiteEnStock;

    private LocalDate datePeremption;
}
