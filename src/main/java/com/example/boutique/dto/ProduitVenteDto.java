package com.example.boutique.dto;

import com.example.boutique.model.Produit;

public class ProduitVenteDto {
    private Produit produit;
    private Long totalVendu;

    public ProduitVenteDto(Produit produit, Long totalVendu) {
        this.produit = produit;
        this.totalVendu = totalVendu;
    }

    public Produit getProduit() {
        return produit;
    }

    public void setProduit(Produit produit) {
        this.produit = produit;
    }

    public Long getTotalVendu() {
        return totalVendu;
    }

    public void setTotalVendu(Long totalVendu) {
        this.totalVendu = totalVendu;
    }
}
