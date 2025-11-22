package com.example.boutique.repository;

import com.example.boutique.model.SessionCaisse;
import com.example.boutique.model.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SessionCaisseRepository extends JpaRepository<SessionCaisse, Long> {

    @Query("SELECT sc FROM SessionCaisse sc JOIN FETCH sc.utilisateur u WHERE u = :utilisateur AND sc.dateFermeture IS NULL ORDER BY sc.dateOuverture DESC")
    Optional<SessionCaisse> findOpenSessionWithUser(@Param("utilisateur") Utilisateur utilisateur);

    Optional<SessionCaisse> findFirstByDateFermetureIsNull();

    Optional<SessionCaisse> findFirstByUtilisateurAndDateFermetureIsNullOrderByDateOuvertureDesc(Utilisateur utilisateur);

    List<SessionCaisse> findByUtilisateur(Utilisateur utilisateur);

    Page<SessionCaisse> findByDateFermetureIsNull(Pageable pageable);

    Page<SessionCaisse> findByDateFermetureIsNotNull(Pageable pageable);

    Page<SessionCaisse> findByDateFermetureIsNullAndUtilisateurUsernameContainingIgnoreCase(String keyword, Pageable pageable);

    Page<SessionCaisse> findByDateFermetureIsNotNullAndUtilisateurUsernameContainingIgnoreCase(String keyword, Pageable pageable);

    @Query("SELECT sc FROM SessionCaisse sc WHERE sc.dateFermeture IS NULL " +
           "AND (:keyword IS NULL OR :keyword = '' OR LOWER(sc.utilisateur.username) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<SessionCaisse> findOpenSessions(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT sc FROM SessionCaisse sc WHERE sc.dateFermeture IS NOT NULL " +
           "AND (:keyword IS NULL OR :keyword = '' OR LOWER(sc.utilisateur.username) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (CAST(:startDate AS timestamp) IS NULL OR sc.dateFermeture >= :startDate) " +
           "AND (CAST(:endDate AS timestamp) IS NULL OR sc.dateFermeture <= :endDate)")
    Page<SessionCaisse> findClosedSessions(@Param("keyword") String keyword,
                                           @Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate,
                                           Pageable pageable);

}