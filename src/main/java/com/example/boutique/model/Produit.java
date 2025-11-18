package com.example.boutique.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "produits")
public class Produit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le nom du produit ne peut pas être vide.")
    @Size(max = 255, message = "Le nom du produit ne doit pas dépasser 255 caractères.")
    @Column(nullable = false)
    private String nom;

    @Size(max = 100, message = "Le code-barres ne doit pas dépasser 100 caractères.")
    @Column(unique = true)
    private String codeBarres;

    public void setCodeBarres(String codeBarres) {
        this.codeBarres = (codeBarres == null || codeBarres.trim().isEmpty()) ? null : codeBarres;
    }


    private BigDecimal prixAchat;

    @Column(nullable = false)
    private BigDecimal prixVenteUnitaire;

    private BigDecimal prixPromotionnel;

    private boolean promotionActive;

    @Size(max = 100, message = "La catégorie ne doit pas dépasser 100 caractères.")
    private String categorie;

    @Column(nullable = false)
    private int quantiteEnStock;

    private LocalDate datePeremption;

    @Size(max = 255, message = "Le nom du fournisseur ne doit pas dépasser 255 caractères.")
    private String nomFournisseur;

    @Size(max = 100, message = "Le numéro de facture ne doit pas dépasser 100 caractères.")
    private String numeroFacture;

    public BigDecimal getMarge() {
        if (prixAchat == null || prixVenteUnitaire == null || prixVenteUnitaire.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal difference = prixVenteUnitaire.subtract(prixAchat);
        return difference.divide(prixVenteUnitaire, 4, RoundingMode.HALF_UP);
    }
}
