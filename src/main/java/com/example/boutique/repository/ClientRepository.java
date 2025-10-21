package com.example.boutique.repository;

import com.example.boutique.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
    // Method to search for clients by name, ignoring case
    List<Client> findByNomContainingIgnoreCase(String nom);
}
