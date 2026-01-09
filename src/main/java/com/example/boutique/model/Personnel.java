package com.example.boutique.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.time.LocalDate;

@Data
@Entity
@Table(name = "personnel")
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "clientId", type = String.class))
@Filter(name = "tenantFilter", condition = "clientId = :clientId")
public class Personnel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    // Nouveau champ pour l'identifiant du client (tenant)
    private String clientId;

    private String poste;

    private LocalDate dateEmbauche;

    private String contact; // Email ou numéro de téléphone
}
