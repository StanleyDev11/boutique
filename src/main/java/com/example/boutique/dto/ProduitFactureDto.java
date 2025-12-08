package com.example.boutique.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ProduitFactureDto {
    private Long id;
    private String nom;
    private BigDecimal prixAchat;
    private BigDecimal prixVenteUnitaire;
    private Double quantite;
    private LocalDate datePeremption;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public BigDecimal getPrixAchat() {
        return prixAchat;
    }

    public void setPrixAchat(BigDecimal prixAchat) {
        this.prixAchat = prixAchat;
    }

    public BigDecimal getPrixVenteUnitaire() {
        return prixVenteUnitaire;
    }

    public void setPrixVenteUnitaire(BigDecimal prixVenteUnitaire) {
        this.prixVenteUnitaire = prixVenteUnitaire;
    }

    public Double getQuantite() {
        return quantite;
    }

    public void setQuantite(Double quantite) {
        this.quantite = quantite;
    }

    public LocalDate getDatePeremption() {
        return datePeremption;
    }

    public void setDatePeremption(LocalDate datePeremption) {
        this.datePeremption = datePeremption;
    }
}
