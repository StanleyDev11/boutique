package com.example.boutique.repository;

import com.example.boutique.model.LigneVente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // Added
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime; // Added
import java.util.List; // Added

@Repository
public interface LigneVenteRepository extends JpaRepository<LigneVente, Long> {
    Page<LigneVente> findByProduitNomContainingIgnoreCase(String nomProduit, Pageable pageable);
    Page<LigneVente> findByVenteDateVenteBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    @Query("SELECT lv.produit, SUM(lv.quantite) AS totalQuantiteVendue " +
           "FROM LigneVente lv GROUP BY lv.produit ORDER BY totalQuantiteVendue DESC")
    List<Object[]> findMostSoldProducts();
}