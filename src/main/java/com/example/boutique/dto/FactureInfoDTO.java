package com.example.boutique.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FactureInfoDTO {
    private String numeroFacture;
    private String nomFournisseur;
    private LocalDateTime dateFacture;
    private Long nombreProduits;
    private BigDecimal montantTotal;
}
