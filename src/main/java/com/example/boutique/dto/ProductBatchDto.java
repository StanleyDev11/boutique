package com.example.boutique.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class ProductBatchDto {

    @NotBlank(message = "Le nom du fournisseur ne peut pas être vide.")
    @Size(max = 255, message = "Le nom du fournisseur ne doit pas dépasser 255 caractères.")
    private String nomFournisseur;

    @NotBlank(message = "Le numéro de facture ne peut pas être vide.")
    @Size(max = 100, message = "Le numéro de facture ne doit pas dépasser 100 caractères.")
    private String numeroFacture;

    @Valid
    @NotEmpty(message = "La liste de produits ne peut pas être vide.")
    private List<ProduitDto> produits;
}
