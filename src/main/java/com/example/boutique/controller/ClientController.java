package com.example.boutique.controller;

import com.example.boutique.model.Client;
import com.example.boutique.repository.ClientRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clients")
public class ClientController {

    private final ClientRepository clientRepository;

    public ClientController(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @GetMapping("/search")
    public List<Client> searchClients(@RequestParam String query) {
        return clientRepository.findByNomContainingIgnoreCase(query);
    }

    @PostMapping
    public ResponseEntity<Client> createClient(@RequestBody Client client) {
        try {
            Client savedClient = clientRepository.save(client);
            return ResponseEntity.ok(savedClient);
        } catch (Exception e) {
            // Handle potential exceptions, e.g., unique constraint violation
            return ResponseEntity.badRequest().build();
        }
    }
}
