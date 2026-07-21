package com.example.boutique.repository;

import com.example.boutique.model.Utilisateur;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    Optional<Utilisateur> findByUsername(String username);

    Optional<Utilisateur> findByCode(String code);

    List<Utilisateur> findByRolesContaining(String role);

    /** Liste paginée en excluant certains comptes (super admin, compte démo). */
    Page<Utilisateur> findByUsernameNotIn(Collection<String> usernames, Pageable pageable);

}
