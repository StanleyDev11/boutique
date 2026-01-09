package com.example.boutique.repository;

import com.example.boutique.model.Licence;
import com.example.boutique.model.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LicenceRepository extends JpaRepository<Licence, Long> {
    Optional<Licence> findByUtilisateur(Utilisateur utilisateur);
}
