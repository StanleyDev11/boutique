package com.example.boutique.repository;

import com.example.boutique.dto.CategorySales;
import com.example.boutique.dto.ProduitVenteDto;
import com.example.boutique.model.LigneVente;
import com.example.boutique.model.Vente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LigneVenteRepository extends JpaRepository<LigneVente, Long> {

    

    List<LigneVente> findByVenteId(Long venteId);

    List<LigneVente> findByVente(Vente vente);

    @Query("SELECT p.categorie as category, sum(lv.prixUnitaire * lv.quantite) as totalSales FROM LigneVente lv JOIN lv.produit p GROUP BY p.categorie")
    List<CategorySales> findTotalSalesByCategory();

    @Query("SELECT lv FROM LigneVente lv WHERE LOWER(lv.produit.nom) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<LigneVente> findByProduitNomContainingIgnoreCase(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT lv FROM LigneVente lv WHERE lv.vente.dateVente BETWEEN :start AND :end")
    Page<LigneVente> findByVenteDateVenteBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, Pageable pageable);

    @Query("SELECT lv.produit, sum(lv.quantite) FROM LigneVente lv GROUP BY lv.produit ORDER BY sum(lv.quantite) DESC")
    List<Object[]> findMostSoldProducts();

    @Query(value = "SELECT lv FROM LigneVente lv JOIN FETCH lv.vente", countQuery = "SELECT count(lv) FROM LigneVente lv")
    Page<LigneVente> findAllWithVente(Pageable pageable);

}