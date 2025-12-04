package com.example.boutique.dto;

import java.time.LocalDate;
import java.util.List;

public class FactureDto {
    private String numeroFacture;
    private String nomFournisseur;
    private LocalDate dateFacture;
    private List<ProduitFactureDto> produits;

    // Getters and Setters
    public String getNumeroFacture() {
        return numeroFacture;
    }

    public void setNumeroFacture(String numeroFacture) {
        this.numeroFacture = numeroFacture;
    }

    public String getNomFournisseur() {
        return nomFournisseur;
    }

    public void setNomFournisseur(String nomFournisseur) {
        this.nomFournisseur = nomFournisseur;
    }

    public LocalDate getDateFacture() {
        return dateFacture;
    }

    public void setDateFacture(LocalDate dateFacture) {
        this.dateFacture = dateFacture;
    }

    public List<ProduitFactureDto> getProduits() {
        return produits;
    }

    public void setProduits(List<ProduitFactureDto> produits) {
        this.produits = produits;
    }
}
