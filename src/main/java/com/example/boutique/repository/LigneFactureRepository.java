package com.example.boutique.repository;

import com.example.boutique.model.LigneFacture;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LigneFactureRepository extends JpaRepository<LigneFacture, Long> {
}
