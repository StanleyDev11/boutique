package com.example.boutique.dto;

import com.example.boutique.enums.TypeMouvement;

import java.util.List;

public class MouvementStockBatchDto {

    private String numeroFacture;
    private String nomFournisseur;
    private TypeMouvement typeMouvement;
    private String description;
    private List<MouvementStockItemDto> mouvements;

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

    public TypeMouvement getTypeMouvement() {
        return typeMouvement;
    }

    public void setTypeMouvement(TypeMouvement typeMouvement) {
        this.typeMouvement = typeMouvement;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<MouvementStockItemDto> getMouvements() {
        return mouvements;
    }

    public void setMouvements(List<MouvementStockItemDto> mouvements) {
        this.mouvements = mouvements;
    }
}
