package com.example.boutique.repository;

import com.example.boutique.dto.VenteCreditDto;
import com.example.boutique.model.Vente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VenteRepository extends JpaRepository<Vente, Long> {
    List<Vente> findByDateVenteBetween(LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT v.id as id, v.dateVente as dateVente, v.totalFinal as totalFinal, c.nom as nomClient FROM Vente v JOIN v.client c WHERE c.id = :clientId AND v.typeVente = 'CREDIT'")
    List<VenteCreditDto> findCreditSalesByClientId(Long clientId);
}
