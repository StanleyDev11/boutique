package com.example.boutique.repository;

import com.example.boutique.model.PanierTemporaire;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PanierTemporaireRepository extends JpaRepository<PanierTemporaire, Long> {

    List<PanierTemporaire> findByUtilisateurId(Long utilisateurId);

    Optional<PanierTemporaire> findByTabIdAndUtilisateurId(Long tabId, Long utilisateurId);
    
    void deleteByTabIdAndUtilisateurId(Long tabId, Long utilisateurId);
}
