package com.example.boutique.repository;

import com.example.boutique.dto.MouvementStatDto;
import com.example.boutique.dto.ProduitVenteDto;
import com.example.boutique.enums.TypeMouvement;
import com.example.boutique.model.MouvementStock;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MouvementStockRepository extends JpaRepository<MouvementStock, Long> {

    List<MouvementStock> findByProduitIdOrderByDateMouvementDesc(Long produitId);

    List<MouvementStock> findTop5ByOrderByDateMouvementDesc();

    @Query("SELECT new com.example.boutique.dto.MouvementStatDto(CAST(m.dateMouvement AS date), COUNT(m)) " +
           "FROM MouvementStock m WHERE m.dateMouvement >= :startDate GROUP BY FUNCTION('DATE', m.dateMouvement)")
    List<MouvementStatDto> countMouvementsByDay(@Param("startDate") LocalDateTime startDate);

    // Pour les KPIs de vente
    List<MouvementStock> findByTypeMouvementAndDateMouvementBetween(TypeMouvement type, LocalDateTime start, LocalDateTime end);

    // Pour les produits les plus vendus
    @Query("SELECT new com.example.boutique.dto.ProduitVenteDto(m.produit, SUM(m.quantite)) " +
           "FROM MouvementStock m WHERE m.typeMouvement = com.example.boutique.enums.TypeMouvement.SORTIE_VENTE " +
           "GROUP BY m.produit ORDER BY SUM(m.quantite) DESC")
    List<ProduitVenteDto> findTopSellingProducts(Pageable pageable);

    // Pour le graphique
    @Query("SELECT new com.example.boutique.dto.MouvementStatDto(CAST(m.dateMouvement AS date), COUNT(m)) " +
           "FROM MouvementStock m WHERE m.dateMouvement >= :startDate AND m.typeMouvement = :type " +
           "GROUP BY FUNCTION('DATE', m.dateMouvement)")
    List<MouvementStatDto> countMouvementsByDayAndType(@Param("startDate") LocalDateTime startDate, @Param("type") TypeMouvement type);

    List<MouvementStock> findByProduitNumeroFactureAndTypeMouvementOrderByProduitNomAsc(String numeroFacture, TypeMouvement type);

}
