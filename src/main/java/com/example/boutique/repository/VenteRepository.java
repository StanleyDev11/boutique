package com.example.boutique.repository;

import com.example.boutique.model.Vente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VenteRepository extends JpaRepository<Vente, Long> {
    List<Vente> findByDateVenteBetween(LocalDateTime startDate, LocalDateTime endDate);
}
