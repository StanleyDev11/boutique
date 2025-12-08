package com.example.boutique.controller;

import com.example.boutique.dto.FactureDto;
import com.example.boutique.dto.ProductBatchDto;
import com.example.boutique.dto.ProduitFactureDto;
import com.example.boutique.enums.TypeMouvement;
import com.example.boutique.model.Facture;
import com.example.boutique.model.LigneFacture;
import com.example.boutique.repository.FactureRepository;
import com.example.boutique.repository.LigneFactureRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import com.example.boutique.model.Produit;
import com.example.boutique.repository.ProduitRepository;
import com.example.boutique.service.ParametreService;
import com.example.boutique.service.ProduitService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/produits")
public class ProduitController {

    private static final Logger logger = LoggerFactory.getLogger(ProduitController.class);

    private final ProduitRepository produitRepository;
    private final ProduitService produitService;
    private final com.example.boutique.repository.MouvementStockRepository mouvementStockRepository;
    private final ParametreService parametreService;
    private final FactureRepository factureRepository;
    private final LigneFactureRepository ligneFactureRepository;


    public ProduitController(ProduitRepository produitRepository, ProduitService produitService, com.example.boutique.repository.MouvementStockRepository mouvementStockRepository, ParametreService parametreService, FactureRepository factureRepository, LigneFactureRepository ligneFactureRepository) {
        this.produitRepository = produitRepository;
        this.produitService = produitService;
        this.mouvementStockRepository = mouvementStockRepository;
        this.parametreService = parametreService;
        this.factureRepository = factureRepository;
        this.ligneFactureRepository = ligneFactureRepository;
    }

    @GetMapping
    public String listProduits(Model model,
                               @RequestParam(defaultValue = "produits") String tab,
                               @RequestParam(required = false) String keyword,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "50") int size,
                               @RequestParam(required = false) String sortField,
                               @RequestParam(defaultValue = "desc") String sortDir,
                               @RequestHeader(value = "X-Requested-With", required = false) String requestedWith) {

        model.addAttribute("activeTab", tab);
        int seuilStockBas = parametreService.getSeuilStockBas();
        model.addAttribute("seuilStockBas", seuilStockBas);

        if (sortField == null || sortField.isEmpty()) {
            if ("factures".equals(tab)) {
                sortField = "dateFacture";
            } else {
                sortField = "nom";
            }
        }

        if ("factures".equals(tab)) {
            Sort sortFactures = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ?
                    Sort.by(sortField).ascending() : Sort.by(sortField).descending();

            Pageable pageable = PageRequest.of(page, size, sortFactures);
            Page<Facture> facturesPage;

            if (keyword != null && !keyword.isEmpty()) {
                facturesPage = factureRepository.findByNumeroFactureContainingIgnoreCaseOrNomFournisseurContainingIgnoreCase(keyword, keyword, pageable);
            } else {
                facturesPage = factureRepository.findAll(pageable);
            }

            model.addAttribute("facturesPage", facturesPage);
            model.addAttribute("totalPagesFactures", facturesPage.getTotalPages());

        } else {
            // La logique pour l'onglet produits reste la même, mais je la nettoie un peu
            Sort sortProduits = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortField).ascending() :
                    Sort.by(sortField).descending();
            Pageable pageable = PageRequest.of(page, size, sortProduits);
            Page<Produit> pageProduits;

            if (keyword != null && !keyword.isEmpty()) {
                pageProduits = produitRepository.findByNomContainingIgnoreCaseOrCodeBarresContaining(keyword, keyword, pageable);
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

    @GetMapping("/facture/{id}")
    public String viewFactureDetails(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Facture> factureOpt = factureRepository.findById(id);

        if (factureOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Facture non trouvée pour l'ID : " + id);
            return "redirect:/produits?tab=factures";
        }

        Facture facture = factureOpt.get();
        // L'accès à facture.getLignes() chargera les lignes car la session est ouverte.
        
        if(facture.getLignes().isEmpty()){
            // On peut quand même afficher la facture vide si on veut
            model.addAttribute("warningMessage", "Cette facture ne contient aucune ligne de produit.");
        }

        model.addAttribute("facture", facture);
        // Les lignes sont accessibles via l'objet facture directement dans le template
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
            model.addAttribute("produits", produitRepository.findAll(Sort.by("nom")));
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

    @GetMapping({"/facture-form", "/facture-form/{id}"})
    public String getFactureFormFragment(@PathVariable(required = false) Long id, Model model) {
        // Préparation du JSON de tous les produits pour la recherche côté client
        List<Produit> produits = produitRepository.findAll(Sort.by("nom"));
        List<java.util.Map<String, Object>> productMaps = new java.util.ArrayList<>();
        for (Produit p : produits) {
            java.util.Map<String, Object> productMap = new java.util.HashMap<>();
            productMap.put("id", p.getId());
            productMap.put("nom", p.getNom());
            productMap.put("codeBarres", p.getCodeBarres());
            productMap.put("prixAchat", p.getPrixAchat());
            productMap.put("prixVenteUnitaire", p.getPrixVenteUnitaire());
            productMap.put("datePeremption", p.getDatePeremption());
            productMaps.add(productMap);
        }

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        String produitsJson = "[]";
        try {
            produitsJson = mapper.writeValueAsString(productMaps);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            logger.error("Erreur lors de la sérialisation des produits en JSON pour le formulaire de facture.", e);
        }
        model.addAttribute("produitsJson", produitsJson);

        // Si un ID est fourni (mode édition), charger les données de la facture existante
        if (id != null) {
            Facture facture = factureRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Facture non trouvée pour l'ID: " + id));

            FactureDto dto = new FactureDto();
            dto.setId(facture.getId());
            dto.setNumeroFacture(facture.getNumeroFacture());
            dto.setNomFournisseur(facture.getNomFournisseur());
            dto.setDateFacture(facture.getDateFacture().toLocalDate());

            List<ProduitFactureDto> produitsDto = facture.getLignes().stream().map(ligne -> {
                ProduitFactureDto pDto = new ProduitFactureDto();
                pDto.setId(ligne.getProduit().getId());
                pDto.setNom(ligne.getProduit().getNom());
                pDto.setPrixAchat(ligne.getPrixUnitaire());
                pDto.setPrixVenteUnitaire(ligne.getProduit().getPrixVenteUnitaire());
                pDto.setQuantite(ligne.getQuantite().doubleValue());
                pDto.setDatePeremption(ligne.getProduit().getDatePeremption());
                return pDto;
            }).collect(Collectors.toList());
            dto.setProduits(produitsDto);

            model.addAttribute("factureDto", dto);
            model.addAttribute("pageTitle", "Modifier la Facture d'Achat");
        } else {
            model.addAttribute("factureDto", new FactureDto());
            model.addAttribute("pageTitle", "Nouvelle Facture d'Achat");
        }

        return "fragments/facture-form :: facture-form";
    }

    @GetMapping("/facture/details/{id}")
    @ResponseBody
    public ResponseEntity<?> getFactureDetails(@PathVariable Long id) {
        try {
            Facture facture = factureRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Facture non trouvée pour l'ID: " + id));

            // Manual conversion from Entity to DTO
            FactureDto dto = new FactureDto();
            dto.setId(facture.getId());
            dto.setNumeroFacture(facture.getNumeroFacture());
            dto.setNomFournisseur(facture.getNomFournisseur());
            dto.setDateFacture(facture.getDateFacture().toLocalDate());

            List<ProduitFactureDto> produitsDto = facture.getLignes().stream().map(ligne -> {
                ProduitFactureDto pDto = new ProduitFactureDto();
                pDto.setId(ligne.getProduit().getId());
                pDto.setNom(ligne.getProduit().getNom());
                pDto.setPrixAchat(ligne.getPrixUnitaire());
                pDto.setPrixVenteUnitaire(ligne.getProduit().getPrixVenteUnitaire());
                pDto.setQuantite(ligne.getQuantite().doubleValue());
                pDto.setDatePeremption(ligne.getProduit().getDatePeremption());
                return pDto;
            }).collect(Collectors.toList());

            dto.setProduits(produitsDto);

            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/save-facture")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveFacture(@ModelAttribute FactureDto factureDto) {
        try {
            // Si un ID est présent, c'est une mise à jour. On supprime l'ancienne version d'abord.
            if (factureDto.getId() != null) {
                produitService.deleteFacture(factureDto.getId());
            }
            // Ensuite, on crée la nouvelle facture (ou la facture modifiée)
            produitService.createFacture(factureDto);
            
            return ResponseEntity.ok(Map.of("success", true, "message", "La facture a été sauvegardée avec succès !"));
        } catch (IllegalArgumentException e) {
            // Erreurs métier prévues (ex: produit non trouvé, prix manquant)
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (DataIntegrityViolationException e) {
            // Erreurs liées à la base de données (ex: contrainte de non-nullité, valeur unique)
            logger.warn("Erreur d'intégrité des données lors de la sauvegarde de la facture : {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Erreur de données. Il est possible qu'un champ obligatoire soit manquant ou qu'une valeur dupliquée soit interdite. Veuillez vérifier les informations saisies."));
        } catch (Exception e) {
            // Toutes les autres erreurs inattendues
            logger.error("Erreur inattendue lors de la sauvegarde de la facture.", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Une erreur inattendue est survenue lors de la sauvegarde de la facture."));
        }
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
            logger.error("Erreur inattendue lors de la sauvegarde des produits en lot.", e);
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
            logger.error("Erreur inattendue lors de la sauvegarde du produit : " + produit.getId(), e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Une erreur inattendue est survenue lors de la sauvegarde du produit."));
        }
    }

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
            logger.error("Erreur inattendue lors de la suppression du produit avec l'ID : " + id, e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Une erreur inattendue est survenue lors de la suppression du produit."));
        }
    }

    @PostMapping("/facture/delete/{id}")
    @ResponseBody
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteFacture(@PathVariable Long id) {
        try {
            produitService.deleteFacture(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Facture supprimée avec succès. Le stock a été mis à jour."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Erreur inattendue lors de la suppression de la facture avec l'ID : " + id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Map.of("success", false, "message", "Une erreur inattendue est survenue lors de la suppression de la facture."));
        }
    }

    @GetMapping("/etiquettes")
    public String showEtiquettesPage(Model model) {
        List<Produit> produits = produitRepository.findAll(Sort.by("nom")).stream()
                .filter(produit -> produit.getQuantiteEnStock().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());
        model.addAttribute("produits", produits);
        return "etiquettes";
    }

    @GetMapping("/print")
    public String printProduits(Model model,
                                @RequestParam(required = false) String keyword,
                                @RequestParam(defaultValue = "nom") String sortField,
                                @RequestParam(defaultValue = "asc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortField).ascending() : Sort.by(sortField).descending();
        Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE, sort); // Pour tout avoir sur une page
        List<Produit> produits;

        if (keyword != null && !keyword.isEmpty()) {
            produits = produitRepository.findByNomContainingIgnoreCaseOrCodeBarresContaining(keyword, keyword, pageable).getContent();
        } else {
            produits = produitRepository.findAll(pageable).getContent();
        }

        model.addAttribute("produits", produits);
        model.addAttribute("printDate", LocalDateTime.now());
        model.addAttribute("seuilStockBas", parametreService.getSeuilStockBas());

        return "produits-print";
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
