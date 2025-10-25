package com.example.boutique.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface VenteCreditDto {
    Long getId();
    LocalDateTime getDateVente();
    BigDecimal getTotalFinal();
    String getNomClient();
}
