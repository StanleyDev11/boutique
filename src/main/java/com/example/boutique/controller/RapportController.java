package com.example.boutique.controller;

import com.example.boutique.model.Produit;
import com.example.boutique.repository.ProduitRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/rapports")
public class RapportController {

    private final ProduitRepository produitRepository;
    private static final int SEUIL_STOCK_BAS = 10;
    private static final int JOURS_AVANT_PEREMPTION = 30;

    public RapportController(ProduitRepository produitRepository) {
        this.produitRepository = produitRepository;
    }

    @GetMapping("/stock-bas")
    public String rapportStockBas(Model model,
                                  @RequestParam(required = false) String filter,
                                  @RequestParam(defaultValue = "asc") String sort,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "8") int size) {

        // --- Préparation de la Pagination et du Tri ---
        Sort.Direction direction = "desc".equalsIgnoreCase(sort) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, "quantiteEnStock"));

        // --- Récupération des Données Paginées pour l'affichage ---
        Page<Produit> produitsPage;
        if ("rupture".equals(filter)) {
            produitsPage = produitRepository.findAllByQuantiteEnStock(0, pageable);
        } else {
            produitsPage = produitRepository.findAllByQuantiteEnStockLessThanEqual(SEUIL_STOCK_BAS, pageable);
        }

        // --- Calcul des Statistiques Globales (non paginées) ---
        List<Produit> tousLesProduitsEnStockBas = produitRepository.findAllByQuantiteEnStockLessThanEqual(SEUIL_STOCK_BAS, PageRequest.of(0, Integer.MAX_VALUE)).getContent();

        long nombreEnRupture = tousLesProduitsEnStockBas.stream()
                .filter(p -> p.getQuantiteEnStock() == 0)
                .count();

        double valeurStockBas = tousLesProduitsEnStockBas.stream()
                .filter(p -> p.getPrixAchat() != null)
                .map(p -> p.getPrixAchat().multiply(new BigDecimal(p.getQuantiteEnStock())))
                .mapToDouble(BigDecimal::doubleValue)
                .sum();

        // --- Données de Péremption ---
        LocalDate aujourdhui = LocalDate.now();
        LocalDate dateLimite = aujourdhui.plusDays(JOURS_AVANT_PEREMPTION);
        List<Produit> produitsPeremptionProche = produitRepository.findAllByDatePeremptionBetween(aujourdhui, dateLimite);

        // --- Ajout des données au modèle ---
        model.addAttribute("produitsPage", produitsPage);
        model.addAttribute("seuil", SEUIL_STOCK_BAS);
        model.addAttribute("sort", sort);
        model.addAttribute("activeFilter", filter);

        // Ajout des stats au modèle
        model.addAttribute("nombreStockBas", tousLesProduitsEnStockBas.size());
        model.addAttribute("nombreEnRupture", nombreEnRupture);
        model.addAttribute("valeurStockBas", valeurStockBas);
        model.addAttribute("produitsPeremptionProche", produitsPeremptionProche);
        model.addAttribute("joursAvantPeremption", JOURS_AVANT_PEREMPTION);
        model.addAttribute("totalProduits", produitRepository.count());

        return "rapport-stock-bas";
    }
}
