package com.example.boutique.controller;

import com.example.boutique.enums.TypeMouvement;
import com.example.boutique.model.MouvementStock;
import com.example.boutique.model.Produit;
import com.example.boutique.repository.MouvementStockRepository;
import com.example.boutique.repository.ProduitRepository;
import com.example.boutique.service.StockService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/stock")
public class StockController {

    private final StockService stockService;
    private final ProduitRepository produitRepository;
    private final MouvementStockRepository mouvementStockRepository;

    public StockController(StockService stockService, ProduitRepository produitRepository, MouvementStockRepository mouvementStockRepository) {
        this.stockService = stockService;
        this.produitRepository = produitRepository;
        this.mouvementStockRepository = mouvementStockRepository;
    }

    @GetMapping("/new")
    public String showStockForm(@RequestParam(required = false) Long produitId, Model model) {
        MouvementStock mouvement = new MouvementStock();
        if (produitId != null) {
            produitRepository.findById(produitId).ifPresent(mouvement::setProduit);
        }

        List<Produit> produits = produitRepository.findAll();
        List<Map<String, Object>> productMaps = new ArrayList<>();
        for (Produit p : produits) {
            Map<String, Object> productMap = new HashMap<>();
            productMap.put("id", p.getId());
            productMap.put("name", p.getNom());
            productMap.put("barcode", p.getCodeBarres());
            productMap.put("stock", p.getQuantiteEnStock());
            productMaps.add(productMap);
        }

        ObjectMapper mapper = new ObjectMapper();
        String produitsJson = "[]";
        try {
            produitsJson = mapper.writeValueAsString(productMaps);
        } catch (JsonProcessingException e) {
            // Gérer l'erreur de sérialisation, par exemple en loggant
        }

        model.addAttribute("mouvement", mouvement);
        model.addAttribute("produitsJson", produitsJson);
        model.addAttribute("produits", produits); // On peut le garder pour d'autres usages si nécessaire
        model.addAttribute("typesMouvement", TypeMouvement.values());
        return "stock-form";
    }

    @PostMapping("/save")
    public String saveMouvement(@ModelAttribute("mouvement") MouvementStock mouvement, RedirectAttributes redirectAttributes) {
        try {
            stockService.enregistrerMouvement(mouvement);
            redirectAttributes.addFlashAttribute("successMessage", "Mouvement de stock enregistré avec succès !");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erreur : " + e.getMessage());
        }
        return "redirect:/produits";
    }

    @GetMapping("/history/{produitId}")
    public String showHistory(@PathVariable Long produitId, Model model, RedirectAttributes redirectAttributes) {
        try {
            Produit produit = produitRepository.findById(produitId)
                    .orElseThrow(() -> new IllegalArgumentException("Produit non trouvé"));
            model.addAttribute("produit", produit);
            model.addAttribute("historique", mouvementStockRepository.findByProduitIdOrderByDateMouvementDesc(produitId));
            return "stock-history";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/produits";
        }
    }
}