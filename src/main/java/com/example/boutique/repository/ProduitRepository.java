package com.example.boutique.repository;

import com.example.boutique.dto.CategoryProductCount;
import com.example.boutique.model.Produit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ProduitRepository extends JpaRepository<Produit, Long> {

    // Méthode pour la recherche par nom
    Page<Produit> findByNomContainingIgnoreCase(String nom, Pageable pageable);

    // Méthode paginée pour le rapport de stock bas
    Page<Produit> findAllByQuantiteEnStockLessThanEqual(int seuil, Pageable pageable);

    // Méthode paginée pour trouver les produits avec un stock exact (pour le filtre "en rupture")
    Page<Produit> findAllByQuantiteEnStock(int stock, Pageable pageable);

    // Méthode pour trouver les produits dont la date de péremption est dans un intervalle donné
    List<Produit> findAllByDatePeremptionBetween(LocalDate startDate, LocalDate endDate);

    // Méthode pour compter les produits en stock bas
    long countByQuantiteEnStockLessThanEqual(int seuil);

    @Query("SELECT p.categorie as category, COUNT(p) as productCount FROM Produit p GROUP BY p.categorie")
    List<CategoryProductCount> countProductsByCategory();
}