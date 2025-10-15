package com.example.boutique.controller;

import com.example.boutique.enums.TypeMouvement;
import com.example.boutique.model.MouvementStock;
import com.example.boutique.model.Produit;
import com.example.boutique.repository.MouvementStockRepository;
import com.example.boutique.repository.ProduitRepository;
import com.example.boutique.service.StockService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

        model.addAttribute("mouvement", mouvement);
        model.addAttribute("produits", produitRepository.findAll());
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