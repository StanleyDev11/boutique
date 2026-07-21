package com.example.boutique.dto;

import com.example.boutique.model.Produit;

import java.math.BigDecimal;

public class ProduitVenteStatsDto {
    private Produit produit;
    private BigDecimal totalQuantiteVendue;
    private BigDecimal totalRevenu;

    public ProduitVenteStatsDto(Produit produit, BigDecimal totalQuantiteVendue, BigDecimal totalRevenu) {
        this.produit = produit;
        this.totalQuantiteVendue = totalQuantiteVendue;
        this.totalRevenu = totalRevenu;
    }

    // Getters and Setters
    public Produit getProduit() {
        return produit;
    }

    public void setProduit(Produit produit) {
        this.produit = produit;
    }

    public BigDecimal getTotalQuantiteVendue() {
        return totalQuantiteVendue;
    }

    public void setTotalQuantiteVendue(BigDecimal totalQuantiteVendue) {
        this.totalQuantiteVendue = totalQuantiteVendue;
    }

    public BigDecimal getTotalRevenu() {
        return totalRevenu;
    }

    public void setTotalRevenu(BigDecimal totalRevenu) {
        this.totalRevenu = totalRevenu;
    }
}
