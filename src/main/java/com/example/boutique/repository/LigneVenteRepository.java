package com.example.boutique.repository;

import com.example.boutique.dto.CategorySales;
import com.example.boutique.model.LigneVente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LigneVenteRepository extends JpaRepository<LigneVente, Long> {
    @Query("SELECT lv FROM LigneVente lv JOIN lv.vente v JOIN lv.produit p WHERE p.nom LIKE %:nomProduit%")
    Page<LigneVente> findByProduitNomContainingIgnoreCase(String nomProduit, Pageable pageable);

    @Query("SELECT lv FROM LigneVente lv JOIN lv.vente v WHERE v.dateVente BETWEEN :startDate AND :endDate")
    Page<LigneVente> findByVenteDateVenteBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    @Query("SELECT lv FROM LigneVente lv JOIN lv.vente")
    Page<LigneVente> findAllWithVente(Pageable pageable);

    @Query("SELECT lv.produit, SUM(lv.quantite) AS totalQuantiteVendue " +
           "FROM LigneVente lv JOIN lv.vente GROUP BY lv.produit ORDER BY totalQuantiteVendue DESC")
    List<Object[]> findMostSoldProducts();

    @Query("SELECT p.categorie as category, SUM(lv.montantTotal) as totalSales FROM LigneVente lv JOIN lv.produit p JOIN lv.vente GROUP BY p.categorie")
    List<CategorySales> findTotalSalesByCategory();
}