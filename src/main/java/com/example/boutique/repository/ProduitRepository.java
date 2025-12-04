package com.example.boutique.repository;

import com.example.boutique.dto.CategoryProductCount;

import com.example.boutique.model.Produit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProduitRepository extends JpaRepository<Produit, Long> {

    // Méthode pour la recherche par nom
    Page<Produit> findByNomContainingIgnoreCase(String nom, Pageable pageable);

    Page<Produit> findByNomContainingIgnoreCaseOrCodeBarresContainingOrNumeroFactureContainingIgnoreCase(String nom, String codeBarres, String numeroFacture, Pageable pageable);

    Page<Produit> findByNomContainingIgnoreCaseOrCodeBarresContaining(String nom, String codeBarres, Pageable pageable);

    // Méthode paginée pour le rapport de stock bas
    Page<Produit> findAllByQuantiteEnStockLessThanEqual(BigDecimal seuil, Pageable pageable);

    // Méthode paginée pour trouver les produits avec un stock exact (pour le filtre "en rupture")
    Page<Produit> findAllByQuantiteEnStock(BigDecimal stock, Pageable pageable);

    // Méthodes pour la recherche par nom et quantité en stock
    Page<Produit> findByNomContainingIgnoreCaseAndQuantiteEnStock(String nom, BigDecimal stock, Pageable pageable);
    Page<Produit> findByNomContainingIgnoreCaseAndQuantiteEnStockLessThanEqual(String nom, BigDecimal seuil, Pageable pageable);

    // Méthode pour trouver les produits dont la date de péremption est dans un intervalle donné et le stock est > 0
    List<Produit> findAllByDatePeremptionBetweenAndQuantiteEnStockGreaterThan(LocalDate startDate, LocalDate endDate, BigDecimal quantite);

    // Méthode pour compter les produits en stock bas
    long countByQuantiteEnStockLessThanEqual(BigDecimal seuil);

    @Query("SELECT p.categorie as category, COUNT(p) as productCount FROM Produit p GROUP BY p.categorie")
    List<CategoryProductCount> countProductsByCategory();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Produit p WHERE p.id = :id")
    Optional<Produit> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT DISTINCT p.categorie FROM Produit p WHERE p.categorie IS NOT NULL AND p.categorie != '' ORDER BY p.categorie")
    List<String> findDistinctCategories();

    Optional<Produit> findByCodeBarres(String codeBarres);

    @Query("SELECT DISTINCT p.numeroFacture FROM Produit p WHERE p.numeroFacture IS NOT NULL AND p.numeroFacture != ''")
    List<String> findDistinctNumeroFacture();



    @Query("SELECT p FROM Produit p WHERE p.quantiteEnStock <= :seuil ORDER BY p.quantiteEnStock ASC")
    List<Produit> findTopNByQuantiteEnStockLessThanEqualOrderByQuantiteEnStockAsc(@Param("seuil") BigDecimal seuil, Pageable pageable);
}