package com.example.boutique.repository;

import com.example.boutique.model.Caisse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CaisseRepository extends JpaRepository<Caisse, Long> {
    List<Caisse> findByNomContainingIgnoreCase(String nom);
}