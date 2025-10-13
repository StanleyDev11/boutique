package com.example.boutique.repository;

import com.example.boutique.model.Produit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProduitRepository extends JpaRepository<Produit, Long> {

    // Méthode pour la recherche par nom
    List<Produit> findByNomContainingIgnoreCase(String nom);

    // Méthode pour le rapport de stock bas
    List<Produit> findAllByQuantiteEnStockLessThanEqualOrderByQuantiteEnStockAsc(int seuil);
}
