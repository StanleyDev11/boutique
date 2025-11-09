package com.example.boutique.repository;

import com.example.boutique.dto.CategoryProductCount;
import com.example.boutique.dto.FactureInfoDTO;
import com.example.boutique.model.Produit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProduitRepository extends JpaRepository<Produit, Long> {

    // Méthode pour la recherche par nom
    Page<Produit> findByNomContainingIgnoreCase(String nom, Pageable pageable);

    Page<Produit> findByNomContainingIgnoreCaseOrCodeBarresContainingOrNumeroFactureContainingIgnoreCase(String nom, String codeBarres, String numeroFacture, Pageable pageable);

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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Produit p WHERE p.id = :id")
    Optional<Produit> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT DISTINCT p.categorie FROM Produit p WHERE p.categorie IS NOT NULL AND p.categorie != '' ORDER BY p.categorie")
    List<String> findDistinctCategories();

    Optional<Produit> findByCodeBarres(String codeBarres);

    @Query("SELECT DISTINCT p.numeroFacture FROM Produit p WHERE p.numeroFacture IS NOT NULL AND p.numeroFacture != ''")
    List<String> findDistinctNumeroFacture();

    @Query("SELECT new com.example.boutique.dto.FactureInfoDTO(" +
            "p.numeroFacture, " +
            "p.nomFournisseur, " +
            "MIN(m.dateMouvement), " +
            "COUNT(DISTINCT p.id), " +
            "SUM(p.prixAchat * m.quantite)) " +
            "FROM MouvementStock m JOIN m.produit p " +
            "WHERE p.numeroFacture IS NOT NULL AND p.numeroFacture <> '' " +
            "AND m.typeMouvement = com.example.boutique.enums.TypeMouvement.ENTREE " +
            "GROUP BY p.numeroFacture, p.nomFournisseur " +
            "ORDER BY MIN(m.dateMouvement) DESC")
    List<FactureInfoDTO> findFactureInfos();

    @Query("SELECT new com.example.boutique.dto.FactureInfoDTO(" +
            "p.numeroFacture, " +
            "p.nomFournisseur, " +
            "MIN(m.dateMouvement), " +
            "COUNT(DISTINCT p.id), " +
            "SUM(p.prixAchat * m.quantite)) " +
            "FROM MouvementStock m JOIN m.produit p " +
            "WHERE p.numeroFacture = :numeroFacture " +
            "AND m.typeMouvement = com.example.boutique.enums.TypeMouvement.ENTREE " +
            "GROUP BY p.numeroFacture, p.nomFournisseur")
    Optional<FactureInfoDTO> findFactureInfoByNumeroFacture(@Param("numeroFacture") String numeroFacture);

    List<Produit> findByNumeroFacture(String numeroFacture);
}