package com.example.boutique.dto;

import java.math.BigDecimal;

public class MouvementStockItemDto {

    private Long produitId;
    private BigDecimal quantite;

    // Getters and Setters
    public Long getProduitId() {
        return produitId;
    }

    public void setProduitId(Long produitId) {
        this.produitId = produitId;
    }

    public BigDecimal getQuantite() {
        return quantite;
    }

    public void setQuantite(BigDecimal quantite) {
        this.quantite = quantite;
    }
}
