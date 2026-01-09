package com.example.boutique.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

@Data
@Entity
@Table(name = "caisses")
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "clientId", type = String.class))
@Filter(name = "tenantFilter", condition = "clientId = :clientId")
public class Caisse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nom;

    // Nouveau champ pour l'identifiant du client (tenant)
    private String clientId;

    @Column(nullable = false)
    private boolean active = true;
}
