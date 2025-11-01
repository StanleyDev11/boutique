package com.example.boutique.repository;

import com.example.boutique.model.Caisse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.boutique.model.Utilisateur;

import java.util.List;
import java.util.Optional;

@Repository
public interface CaisseRepository extends JpaRepository<Caisse, Long> {
    List<Caisse> findByNomContainingIgnoreCase(String nom);

    Optional<Caisse> findByUtilisateur(Utilisateur utilisateur);
}