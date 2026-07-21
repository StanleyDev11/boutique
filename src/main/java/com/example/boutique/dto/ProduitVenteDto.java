package com.example.boutique.dto;

import com.example.boutique.model.Produit;

import java.math.BigDecimal;

public class ProduitVenteDto {
    private Produit produit;
    private BigDecimal totalVendu;

    public ProduitVenteDto(Produit produit, BigDecimal totalVendu) {
        this.produit = produit;
        this.totalVendu = totalVendu;
    }

    public Produit getProduit() {
        return produit;
    }

    public void setProduit(Produit produit) {
        this.produit = produit;
    }

    public BigDecimal getTotalVendu() {
        return totalVendu;
    }

    public void setTotalVendu(BigDecimal totalVendu) {
        this.totalVendu = totalVendu;
    }
}
