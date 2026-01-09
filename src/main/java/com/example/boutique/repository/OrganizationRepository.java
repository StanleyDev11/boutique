package com.example.boutique.repository;

import com.example.boutique.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    Optional<Organization> findByClientId(String clientId);
    Optional<Organization> findByNom(String nom);
}
