package com.example.boutique.controller;

import com.example.boutique.model.Produit;
import com.example.boutique.repository.ProduitRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/produits")
public class ProduitController {

    private final ProduitRepository produitRepository;

    public ProduitController(ProduitRepository produitRepository) {
        this.produitRepository = produitRepository;
    }

    @GetMapping
    public String listProduits(Model model, @RequestParam(required = false) String keyword) {
        if (keyword != null && !keyword.isEmpty()) {
            model.addAttribute("produits", produitRepository.findByNomContainingIgnoreCase(keyword));
        } else {
            model.addAttribute("produits", produitRepository.findAll());
        }
        model.addAttribute("keyword", keyword);
        return "produits";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("produit", new Produit());
        model.addAttribute("pageTitle", "Ajouter un nouveau produit");
        return "produit-form";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Produit produit = produitRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Produit non trouvé pour l'ID: " + id));
            model.addAttribute("produit", produit);
            model.addAttribute("pageTitle", "Modifier le produit");
            return "produit-form";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/produits";
        }
    }

    @PostMapping("/save")
    public String saveProduit(@ModelAttribute("produit") Produit produit, RedirectAttributes redirectAttributes) {
        try {
            produitRepository.save(produit);
            redirectAttributes.addFlashAttribute("successMessage", "Le produit a été sauvegardé avec succès !");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erreur lors de la sauvegarde du produit.");
        }
        return "redirect:/produits";
    }

    @PostMapping("/delete/{id}")
    public String deleteProduit(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            produitRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Le produit a été supprimé avec succès !");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erreur lors de la suppression du produit.");
        }
        return "redirect:/produits";
    }
}
