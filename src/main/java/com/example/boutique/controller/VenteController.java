package com.example.boutique.controller;

import com.example.boutique.model.LigneVente;
import com.example.boutique.model.Vente;
import com.example.boutique.repository.LigneVenteRepository;
import com.example.boutique.repository.VenteRepository;
import com.example.boutique.service.StockService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/ventes")
public class VenteController {

    private final VenteRepository venteRepository;
    private final LigneVenteRepository ligneVenteRepository;
    private final StockService stockService;

    public VenteController(VenteRepository venteRepository, LigneVenteRepository ligneVenteRepository, StockService stockService) {
        this.venteRepository = venteRepository;
        this.ligneVenteRepository = ligneVenteRepository;
        this.stockService = stockService;
    }

    @GetMapping("/{id}")
    public String getVenteDetails(@PathVariable Long id, Model model) {
        model.addAttribute("vente", venteRepository.findById(id).orElseThrow());
        return "vente-detail";
    }

    @GetMapping("/recu/{id}")
    public String showRecu(@PathVariable Long id, Model model) {
        Vente vente = venteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vente non trouvée avec l'id : " + id));
        List<LigneVente> ligneVentes = ligneVenteRepository.findByVenteId(id);

        model.addAttribute("vente", vente);
        model.addAttribute("ligneVentes", ligneVentes);

        return "recu-vente";
    }

    @PostMapping("/{id}/annuler")
    public ResponseEntity<?> annulerVente(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            String motif = body.get("motif");
            if (motif == null || motif.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Le motif d'annulation est obligatoire."));
            }
            stockService.annulerVente(id, motif);
            return ResponseEntity.ok(Map.of("success", true, "message", "Vente annulée avec succès."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
