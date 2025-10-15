package com.example.boutique.repository;

import com.example.boutique.dto.MouvementStatDto;
import com.example.boutique.model.MouvementStock;
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

    @Query("SELECT new com.example.boutique.dto.MouvementStatDto(FUNCTION('DATE', m.dateMouvement), COUNT(m)) " +
           "FROM MouvementStock m WHERE m.dateMouvement >= :startDate GROUP BY FUNCTION('DATE', m.dateMouvement)")
    List<MouvementStatDto> countMouvementsByDay(@Param("startDate") LocalDateTime startDate);

}