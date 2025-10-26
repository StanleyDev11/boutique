package com.example.boutique.controller;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.example.boutique.dto.VenteRequestDto;
import com.example.boutique.model.Produit;
import com.example.boutique.repository.ClientRepository;
import com.example.boutique.repository.ProduitRepository;
import com.example.boutique.repository.UtilisateurRepository;
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
    private final UtilisateurRepository utilisateurRepository;

    public CaissierController(ProduitRepository produitRepository, StockService stockService, ClientRepository clientRepository, UtilisateurRepository utilisateurRepository) {
        this.produitRepository = produitRepository;
        this.stockService = stockService;
        this.clientRepository = clientRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    @GetMapping
    public String caissier(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        model.addAttribute("produits", produitRepository.findAll());
        model.addAttribute("categories", produitRepository.findAll().stream().map(Produit::getCategorie).distinct().collect(Collectors.toList()));
        model.addAttribute("clients", clientRepository.findAll());
        model.addAttribute("user", userDetails);
        return "caissier";
    }

    @PostMapping("/vendre")
    public ResponseEntity<Map<String, Object>> vendre(@RequestBody VenteRequestDto venteRequest, @AuthenticationPrincipal UserDetails userDetails) {
        try {
            var utilisateur = utilisateurRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new IllegalStateException("Utilisateur non trouvé"));
            stockService.enregistrerVente(venteRequest, utilisateur);
            return ResponseEntity.ok(Map.of("success", true, "message", "Vente enregistrée avec succès !"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
