package com.example.boutique.controller;

import com.example.boutique.repository.PersonnelRepository;
import com.example.boutique.repository.ProduitRepository;
import com.example.boutique.repository.UtilisateurRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private final ProduitRepository produitRepository;
    private final PersonnelRepository personnelRepository;
    private final UtilisateurRepository utilisateurRepository;

    private static final int SEUIL_STOCK_BAS = 10;

    public DashboardController(ProduitRepository produitRepository, PersonnelRepository personnelRepository, UtilisateurRepository utilisateurRepository) {
        this.produitRepository = produitRepository;
        this.personnelRepository = personnelRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    @GetMapping
    public String showDashboard(Model model) {
        model.addAttribute("totalProduits", produitRepository.count());
        model.addAttribute("produitsStockBas", produitRepository.findAllByQuantiteEnStockLessThanEqualOrderByQuantiteEnStockAsc(SEUIL_STOCK_BAS).size());
        model.addAttribute("totalPersonnel", personnelRepository.count());
        model.addAttribute("totalUtilisateurs", utilisateurRepository.count());

        // Vous pouvez ajouter d'autres statistiques ici

        return "dashboard";
    }
}
