package com.example.boutique.model;

import com.example.boutique.enums.VenteStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@ToString
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
    @ToString.Exclude
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VenteStatus status;

    @ManyToOne
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;

    @ManyToOne
    @JoinColumn(name = "session_caisse_id")
    private SessionCaisse sessionCaisse;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vente vente = (Vente) o;
        return id != null && id.equals(vente.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
