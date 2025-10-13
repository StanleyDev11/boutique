package com.example.boutique.model;

import com.example.boutique.enums.TypeMouvement;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "mouvements_stock")
public class MouvementStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "produit_id", nullable = false)
    private Produit produit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeMouvement typeMouvement;

    @Column(nullable = false)
    private int quantite;

    @Column(nullable = false)
    private LocalDateTime dateMouvement;

    private String description;

    @PrePersist
    protected void onCreate() {
        this.dateMouvement = LocalDateTime.now();
    }
}
