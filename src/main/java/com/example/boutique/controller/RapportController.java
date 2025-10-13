package com.example.boutique.controller;

import com.example.boutique.repository.ProduitRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/rapports")
public class RapportController {

    private final ProduitRepository produitRepository;
    private static final int SEUIL_STOCK_BAS = 10;

    public RapportController(ProduitRepository produitRepository) {
        this.produitRepository = produitRepository;
    }

    @GetMapping("/stock-bas")
    public String rapportStockBas(Model model) {
        model.addAttribute("produits", produitRepository.findAllByQuantiteEnStockLessThanEqualOrderByQuantiteEnStockAsc(SEUIL_STOCK_BAS));
        model.addAttribute("seuil", SEUIL_STOCK_BAS);
        return "rapport-stock-bas";
    }
}
