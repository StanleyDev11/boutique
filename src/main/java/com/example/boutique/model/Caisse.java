package com.example.boutique.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "caisses")
public class Caisse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nom;

    @Column(nullable = false)
    private boolean active = true;
}
