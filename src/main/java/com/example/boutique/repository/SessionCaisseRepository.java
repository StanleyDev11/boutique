package com.example.boutique.repository;

import com.example.boutique.model.SessionCaisse;
import com.example.boutique.model.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SessionCaisseRepository extends JpaRepository<SessionCaisse, Long> {

    @Query("SELECT sc FROM SessionCaisse sc JOIN FETCH sc.utilisateur u WHERE u = :utilisateur AND sc.dateFermeture IS NULL ORDER BY sc.dateOuverture DESC")
    Optional<SessionCaisse> findOpenSessionWithUser(@Param("utilisateur") Utilisateur utilisateur);

    Optional<SessionCaisse> findFirstByUtilisateurAndDateFermetureIsNullOrderByDateOuvertureDesc(Utilisateur utilisateur);

    List<SessionCaisse> findByUtilisateur(Utilisateur utilisateur);
}
