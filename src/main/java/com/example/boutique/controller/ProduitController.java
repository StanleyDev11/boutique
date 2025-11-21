package com.example.boutique.controller;

import com.example.boutique.dto.FactureInfoDTO;
import com.example.boutique.dto.ProductBatchDto;
import com.example.boutique.dto.ProduitDto;
import org.springframework.dao.DataIntegrityViolationException;
import com.example.boutique.model.MouvementStock;
import com.example.boutique.model.Produit;
import com.example.boutique.repository.ProduitRepository;
import com.example.boutique.service.ParametreService;
import com.example.boutique.service.ProduitService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.example.boutique.enums.TypeMouvement;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/produits")
public class ProduitController {

    private final ProduitRepository produitRepository;
    private final ProduitService produitService;
    private final com.example.boutique.repository.MouvementStockRepository mouvementStockRepository;
    private final ParametreService parametreService;


    public ProduitController(ProduitRepository produitRepository, ProduitService produitService, com.example.boutique.repository.MouvementStockRepository mouvementStockRepository, ParametreService parametreService) {
        this.produitRepository = produitRepository;
        this.produitService = produitService;
        this.mouvementStockRepository = mouvementStockRepository;
        this.parametreService = parametreService;
    }

    @GetMapping
    public String listProduits(Model model,
                               @RequestParam(defaultValue = "produits") String tab,
                               @RequestParam(required = false) String keyword,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "10") int size,
                               @RequestParam(defaultValue = "nom") String sortField,
                               @RequestParam(defaultValue = "asc") String sortDir,
                               @RequestHeader(value = "X-Requested-With", required = false) String requestedWith) {

        model.addAttribute("activeTab", tab);
        int seuilStockBas = parametreService.getSeuilStockBas();
        model.addAttribute("seuilStockBas", seuilStockBas);

        if ("factures".equals(tab)) {
            List<FactureInfoDTO> factures = produitRepository.findFactureInfos();

            // Search
            if (keyword != null && !keyword.isEmpty()) {
                String keywordLower = keyword.toLowerCase();
                factures = factures.stream()
                        .filter(f -> (f.getNumeroFacture() != null && f.getNumeroFacture().toLowerCase().contains(keywordLower)) ||
                                     (f.getNomFournisseur() != null && f.getNomFournisseur().toLowerCase().contains(keywordLower)))
                        .collect(Collectors.toList());
            }

            // Sorting
            String finalSortField = "factures".equals(tab) && "nom".equals(sortField) ? "dateFacture" : sortField;
            Comparator<FactureInfoDTO> comparator = switch (finalSortField) {
                case "numeroFacture" -> Comparator.comparing(FactureInfoDTO::getNumeroFacture, Comparator.nullsLast(String::compareToIgnoreCase));
                case "nomFournisseur" -> Comparator.comparing(FactureInfoDTO::getNomFournisseur, Comparator.nullsLast(String::compareToIgnoreCase));
                case "montantTotal" -> Comparator.comparing(FactureInfoDTO::getMontantTotal, Comparator.nullsLast(BigDecimal::compareTo));
                default -> Comparator.comparing(FactureInfoDTO::getDateFacture, Comparator.nullsLast(LocalDateTime::compareTo));
            };

            if ("desc".equalsIgnoreCase(sortDir)) {
                comparator = comparator.reversed();
            }
            factures.sort(comparator);

            model.addAttribute("facturesInfos", factures);
            sortField = finalSortField;

        } else {
            Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortField).ascending() :
                    Sort.by(sortField).descending();
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<Produit> pageProduits;

            if (keyword != null && !keyword.isEmpty()) {
                pageProduits = produitRepository.findByNomContainingIgnoreCaseOrCodeBarresContainingOrNumeroFactureContainingIgnoreCase(keyword, keyword, keyword, pageable);
            } else {
                pageProduits = produitRepository.findAll(pageable);
            }

            model.addAttribute("produitsPage", pageProduits);
            model.addAttribute("totalPages", pageProduits.getTotalPages());
        }

        model.addAttribute("keyword", keyword);
        model.addAttribute("currentPage", page);
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", sortDir.equals("asc") ? "desc" : "asc");

        if ("XMLHttpRequest".equals(requestedWith)) {
            if ("factures".equals(tab)) {
                return "produits :: #facture-list-container";
            }
            return "produits :: #product-list-container";
        }

        return "produits";
    }

    @GetMapping("/facture/{numeroFacture}")
    public String viewFactureDetails(@PathVariable String numeroFacture, Model model, RedirectAttributes redirectAttributes) {
        List<Produit> produits = produitRepository.findByNumeroFacture(numeroFacture);
        Optional<FactureInfoDTO> factureInfoOpt = produitRepository.findFactureInfoByNumeroFacture(numeroFacture);

        if (factureInfoOpt.isEmpty() || produits.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Détails de la facture non trouvés pour le numéro : " + numeroFacture);
            return "redirect:/produits?tab=factures";
        }

        List<MouvementStock> mouvements = mouvementStockRepository.findByProduitNumeroFactureAndTypeMouvementOrderByProduitNomAsc(numeroFacture, TypeMouvement.ENTREE);

        model.addAttribute("produits", produits);
        model.addAttribute("factureInfo", factureInfoOpt.get());
        model.addAttribute("mouvements", mouvements);
        model.addAttribute("numeroFacture", numeroFacture);
        return "facture-details";
    }

    @GetMapping("/form")
    public String getFormFragment(@RequestParam(required = false) Long id,
                                  @RequestParam(defaultValue = "false") boolean batch,
                                  Model model) {
        List<String> categories = produitRepository.findDistinctCategories();
        model.addAttribute("categories", categories);

        if (batch) {
            model.addAttribute("productBatchDto", new ProductBatchDto());
            model.addAttribute("pageTitle", "Ajouter des produits par lot");
            return "produit-form :: form-content";
        }

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
    @PostMapping("/save-batch")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveBatch(@Valid @ModelAttribute ProductBatchDto productBatchDto, org.springframework.validation.BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            List<String> errors = bindingResult.getAllErrors().stream()
                    .map(org.springframework.validation.ObjectError::getDefaultMessage)
                    .collect(Collectors.toList());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Erreurs de validation.", "errors", errors));
        }
        try {
            produitService.saveNewProductBatch(productBatchDto);
            return ResponseEntity.ok(Map.of("success", true, "message", "Les produits ont été sauvegardés avec succès !"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            // Log the exception e
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Une erreur inattendue est survenue lors de la sauvegarde des produits."));
        }
    }


    @PostMapping("/save")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveProduit(@Valid @ModelAttribute("produit") Produit produit, org.springframework.validation.BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            List<String> errors = bindingResult.getAllErrors().stream()
                    .map(org.springframework.validation.ObjectError::getDefaultMessage)
                    .collect(Collectors.toList());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Erreurs de validation.", "errors", errors));
        }
        try {
            produitService.saveProduit(produit);
            return ResponseEntity.ok(Map.of("success", true, "message", "Le produit a été sauvegardé avec succès !"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            // Log the exception e
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Une erreur inattendue est survenue lors de la sauvegarde du produit."));
        }
    }



// ... other imports and class definition ...

    @PostMapping("/delete/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteProduit(@PathVariable Long id) {
        try {
            produitRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Le produit a été supprimé avec succès !"));
        } catch (DataIntegrityViolationException e) {
            // This exception typically occurs due to foreign key constraints
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Impossible de supprimer ce produit car il est lié à d'autres enregistrements (ex: mouvements de stock, ventes). Veuillez supprimer les enregistrements associés d'abord."));
        } catch (Exception e) {
            // Log the exception e
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Une erreur inattendue est survenue lors de la suppression du produit."));
        }
    }

    @GetMapping("/etiquettes")
    public String showEtiquettesPage(Model model) {
        List<Produit> produits = produitRepository.findAll(Sort.by("nom"));
        model.addAttribute("produits", produits);
        return "etiquettes";
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
