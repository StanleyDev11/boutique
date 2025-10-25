package com.example.boutique.controller;

import com.example.boutique.repository.VenteRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/ventes")
public class VenteController {

    private final VenteRepository venteRepository;

    public VenteController(VenteRepository venteRepository) {
        this.venteRepository = venteRepository;
    }

    @GetMapping("/{id}")
    public String getVenteDetails(@PathVariable Long id, Model model) {
        model.addAttribute("vente", venteRepository.findById(id).orElseThrow());
        return "vente-detail";
    }
}
