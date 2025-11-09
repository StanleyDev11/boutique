package com.example.boutique.repository;

import com.example.boutique.dto.VenteCreditDto;
import com.example.boutique.enums.VenteStatus;
import com.example.boutique.model.SessionCaisse;
import com.example.boutique.model.Utilisateur;
import com.example.boutique.model.Vente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VenteRepository extends JpaRepository<Vente, Long> {
    Page<Vente> findByDateVenteBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    List<Vente> findByDateVenteBetween(LocalDateTime startDate, LocalDateTime endDate);


    @Query("SELECT v.id as id, v.dateVente as dateVente, v.totalFinal as totalFinal, c.nom as nomClient FROM Vente v JOIN v.client c WHERE c.id = :clientId AND v.typeVente = 'CREDIT'")
    List<VenteCreditDto> findCreditSalesByClientId(Long clientId);

    @Query("SELECT SUM(v.totalFinal) FROM Vente v WHERE v.utilisateur = :utilisateur AND v.dateVente >= :startDate AND v.typeVente != 'credit'")
    BigDecimal sumTotalForUserSince(@Param("utilisateur") Utilisateur utilisateur, @Param("startDate") LocalDateTime startDate);

    @Query("SELECT SUM(v.totalFinal) FROM Vente v WHERE v.sessionCaisse = :sessionCaisse AND v.typeVente != 'credit'")
    BigDecimal sumTotalForSession(@Param("sessionCaisse") SessionCaisse sessionCaisse);

    List<Vente> findByUtilisateurAndDateVenteAfter(Utilisateur utilisateur, LocalDateTime startDate, org.springframework.data.domain.Sort sort);

    @EntityGraph(attributePaths = {"ligneVentes", "ligneVentes.produit", "utilisateur"})
    List<Vente> findBySessionCaisse(SessionCaisse sessionCaisse, org.springframework.data.domain.Sort sort);

    List<Vente> findByUtilisateurAndDateVenteBetween(Utilisateur utilisateur, LocalDateTime startDate, LocalDateTime endDate);

    long countByStatus(VenteStatus status);

    List<Vente> findAllByDateVenteAfter(LocalDateTime date);

    @Query("SELECT DISTINCT v FROM Vente v LEFT JOIN FETCH v.ligneVentes lv LEFT JOIN FETCH lv.produit LEFT JOIN FETCH v.utilisateur LEFT JOIN FETCH v.client WHERE v.dateVente BETWEEN :startDateTime AND :endDateTime")
    List<Vente> findAllWithDetailsByDateVenteBetween(@Param("startDateTime") LocalDateTime startDateTime, @Param("endDateTime") LocalDateTime endDateTime, org.springframework.data.domain.Sort sort);
}
