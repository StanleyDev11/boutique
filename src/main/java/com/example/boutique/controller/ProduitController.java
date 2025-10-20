package com.example.boutique.controller;

import com.example.boutique.model.Produit;
import com.example.boutique.repository.ProduitRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/produits")
public class ProduitController {

    private final ProduitRepository produitRepository;

    public ProduitController(ProduitRepository produitRepository) {
        this.produitRepository = produitRepository;
    }

    @GetMapping
    public String listProduits(Model model,
                               @RequestParam(required = false) String keyword,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "10") int size,
                               @RequestParam(defaultValue = "nom") String sortField,
                               @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortField).ascending() :
                Sort.by(sortField).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Produit> pageProduits;

        if (keyword != null && !keyword.isEmpty()) {
            pageProduits = produitRepository.findByNomContainingIgnoreCase(keyword, pageable);
        } else {
            pageProduits = produitRepository.findAll(pageable);
        }

        model.addAttribute("produitsPage", pageProduits);
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", pageProduits.getTotalPages());
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc" : "asc");

        return "produits";
    }

    @GetMapping("/form")
    public String getFormFragment(@RequestParam(required = false) Long id, Model model) {
        Produit produit;
        String pageTitle;
        if (id != null) {
            produit = produitRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Produit non trouvé pour l'ID: " + id));
            pageTitle = "Modifier le produit";
        } else {
            produit = new Produit();
            pageTitle = "Ajouter un nouveau produit";
        }
        model.addAttribute("produit", produit);
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("view", "fragment"); // Pour le rendu conditionnel dans le template
        return "produit-form :: form-content";
    }

    @PostMapping("/save")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveProduit(@ModelAttribute("produit") Produit produit) {
        try {
            produitRepository.save(produit);
            return ResponseEntity.ok(Map.of("success", true, "message", "Le produit a été sauvegardé avec succès !"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Erreur lors de la sauvegarde du produit."));
        }
    }

    @PostMapping("/delete/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteProduit(@PathVariable Long id) {
        try {
            produitRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Le produit a été supprimé avec succès !"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Erreur lors de la suppression du produit."));
        }
    }

    // --- Anciennes méthodes (peuvent être gardées pour la navigation sans JS) ---
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
}
