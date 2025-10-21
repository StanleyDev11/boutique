package com.example.boutique.controller;

import com.example.boutique.dto.VenteRequestDto;
import com.example.boutique.model.Produit;
import com.example.boutique.repository.ClientRepository;
import com.example.boutique.repository.ProduitRepository;
import com.example.boutique.service.StockService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/caissier")
public class CaissierController {

    private final ProduitRepository produitRepository;
    private final StockService stockService;
    private final ClientRepository clientRepository;

    public CaissierController(ProduitRepository produitRepository, StockService stockService, ClientRepository clientRepository) {
        this.produitRepository = produitRepository;
        this.stockService = stockService;
        this.clientRepository = clientRepository;
    }

    @GetMapping
    public String caissier(Model model) {
        model.addAttribute("produits", produitRepository.findAll());
        model.addAttribute("categories", produitRepository.findAll().stream().map(Produit::getCategorie).distinct().collect(Collectors.toList()));
        model.addAttribute("clients", clientRepository.findAll());
        return "caissier";
    }

    @PostMapping("/vendre")
    public ResponseEntity<Map<String, Object>> vendre(@RequestBody VenteRequestDto venteRequest) {
        try {
            stockService.enregistrerVente(venteRequest);
            return ResponseEntity.ok(Map.of("success", true, "message", "Vente enregistrée avec succès !"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
