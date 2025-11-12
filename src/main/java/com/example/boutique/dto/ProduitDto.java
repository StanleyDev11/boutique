package com.example.boutique.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDate;

@Data
public class ProduitDto {
    private Long id;

    @NotBlank(message = "Le nom du produit ne peut pas être vide.")
    @Size(max = 255, message = "Le nom du produit ne doit pas dépasser 255 caractères.")
    private String nom;

    @Size(max = 100, message = "Le code-barres ne doit pas dépasser 100 caractères.")
    private String codeBarres;

    @PositiveOrZero(message = "Le prix d'achat doit être un nombre positif ou zéro.")
    private double prixAchat;

    @PositiveOrZero(message = "Le prix de vente doit être un nombre positif ou zéro.")
    private double prixVenteUnitaire;

    private Double prixPromotionnel;
    private boolean promotionActive;

    @Size(max = 100, message = "La catégorie ne doit pas dépasser 100 caractères.")
    private String categorie;

    @PositiveOrZero(message = "La quantité en stock doit être un nombre positif ou zéro.")
    private int quantiteEnStock;

    private LocalDate datePeremption;

    // These are inherited from the batch, so they are not part of the individual product DTO validation
    private String nomFournisseur;
    private String numeroFacture;
}
