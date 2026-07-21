package com.example.boutique.repository;

import com.example.boutique.model.Parametre;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ParametreRepository extends JpaRepository<Parametre, String> {
    Optional<Parametre> findByCle(String cle);
}
