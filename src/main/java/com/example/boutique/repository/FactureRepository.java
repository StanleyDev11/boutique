package com.example.boutique.repository;

import com.example.boutique.model.Facture;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FactureRepository extends JpaRepository<Facture, Long> {
    boolean existsByNumeroFacture(String numeroFacture);
    Optional<Facture> findByNumeroFacture(String numeroFacture);
    Page<Facture> findByNumeroFactureContainingIgnoreCaseOrNomFournisseurContainingIgnoreCase(String numeroFacture, String nomFournisseur, Pageable pageable);
}
