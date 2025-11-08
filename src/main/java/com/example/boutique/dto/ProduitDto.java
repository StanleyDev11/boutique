package com.example.boutique.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ProduitDto {
    private Long id;
    private String nom;
    private String codeBarres;
    private double prixAchat;
    private double prixVenteUnitaire;
    private String categorie;
    private int quantiteEnStock;
    private LocalDate datePeremption;
    private String nomFournisseur;
    private String numeroFacture;
}
