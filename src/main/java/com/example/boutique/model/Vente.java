package com.example.boutique.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List; // Added

@Data
@Entity
@Table(name = "ventes")
public class Vente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private Client client;

    @Column(nullable = false)
    private LocalDateTime dateVente;

    @OneToMany(mappedBy = "vente", cascade = CascadeType.ALL, orphanRemoval = true) // Added
    private List<LigneVente> ligneVentes; // Added

    @Column(name = "total", nullable = false)
    private BigDecimal total;

    @Column(name = "total_brut", nullable = false)
    private BigDecimal totalBrut;

    private BigDecimal remise;

    @Column(name = "total_net", nullable = false)
    private BigDecimal totalNet;

    @Column(name = "total_final", nullable = false)
    private BigDecimal totalFinal;

    @Column(nullable = false)
    private String typeVente; // "Payé" ou "A crédit"

    @Column(nullable = false)
    private String moyenPaiement; // "Espèces", "Carte", ou "Credit"
}
