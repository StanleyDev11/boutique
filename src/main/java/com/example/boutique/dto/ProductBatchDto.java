package com.example.boutique.dto;

import lombok.Data;
import java.util.List;

@Data
public class ProductBatchDto {
    private String nomFournisseur;
    private String numeroFacture;
    private List<ProduitDto> produits;
}
