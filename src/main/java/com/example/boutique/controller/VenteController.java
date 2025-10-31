package com.example.boutique.controller;

import com.example.boutique.model.LigneVente;
import com.example.boutique.model.Vente;
import com.example.boutique.repository.LigneVenteRepository;
import com.example.boutique.repository.VenteRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/ventes")
public class VenteController {

    private final VenteRepository venteRepository;
    private final LigneVenteRepository ligneVenteRepository;

    public VenteController(VenteRepository venteRepository, LigneVenteRepository ligneVenteRepository) {
        this.venteRepository = venteRepository;
        this.ligneVenteRepository = ligneVenteRepository;
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
}