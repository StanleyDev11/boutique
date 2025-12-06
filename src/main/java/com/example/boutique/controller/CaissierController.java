package com.example.boutique.controller;

import com.example.boutique.dto.VenteRequestDto;
import com.example.boutique.model.Produit;
import com.example.boutique.model.SessionCaisse;
import com.example.boutique.model.Utilisateur;
import com.example.boutique.repository.ClientRepository;
import com.example.boutique.repository.ProduitRepository;
import com.example.boutique.repository.SessionCaisseRepository;
import com.example.boutique.repository.UtilisateurRepository;
import com.example.boutique.service.StockService;
import com.example.boutique.service.PanierTemporaireService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/caissier")
public class CaissierController {

    private static final Logger logger = LoggerFactory.getLogger(CaissierController.class);

    private final ProduitRepository produitRepository;
    private final StockService stockService;
    private final ClientRepository clientRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final SessionCaisseRepository sessionCaisseRepository;
    private final PanierTemporaireService panierTemporaireService;

    public CaissierController(ProduitRepository produitRepository, StockService stockService, ClientRepository clientRepository, UtilisateurRepository utilisateurRepository, SessionCaisseRepository sessionCaisseRepository, PanierTemporaireService panierTemporaireService) {
        this.produitRepository = produitRepository;
        this.stockService = stockService;
        this.clientRepository = clientRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.sessionCaisseRepository = sessionCaisseRepository;
        this.panierTemporaireService = panierTemporaireService;
    }

    @GetMapping
    public String caissier(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        // Selon la nouvelle logique, on vérifie s'il y a N'IMPORTE QUELLE session ouverte
        Optional<SessionCaisse> sessionOpt = sessionCaisseRepository.findFirstByDateFermetureIsNull();

        if (sessionOpt.isEmpty()) {
             // Rediriger vers la page d'ouverture de caisse si aucune session n'est ouverte
             return "redirect:/caisse/ouvrir";
        }

        model.addAttribute("produits", produitRepository.findAll());
        model.addAttribute("categories", produitRepository.findAll().stream().map(Produit::getCategorie).distinct().collect(Collectors.toList()));
        model.addAttribute("clients", clientRepository.findAll());
        model.addAttribute("user", userDetails);
        return "caissier";
    }

    @PostMapping("/vendre")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> vendre(@RequestBody VenteRequestDto venteRequest) {
        try {
            String code = venteRequest.getCodeCaissier();
            if (code == null || code.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Le code du caissier est requis pour valider la vente."));
            }

            Utilisateur utilisateur = utilisateurRepository.findByCode(code)
                    .orElseThrow(() -> new IllegalArgumentException("Code caissier invalide."));

            if (!utilisateur.getRoles().contains("ROLE_CAISSIER")) {
                throw new IllegalStateException("L'utilisateur n'a pas les droits de caissier.");
            }

            stockService.enregistrerVente(venteRequest, utilisateur);

            // La suppression du panier temporaire est désactivée pour permettre la persistance des onglets.
            // if (venteRequest.getTabId() != null) {
            //     panierTemporaireService.deletePanier(venteRequest.getTabId(), utilisateur);
            // }

            return ResponseEntity.ok(Map.of("success", true, "message", "Vente enregistrée avec succès !"));
        } catch (IllegalArgumentException | IllegalStateException e) {
            logger.warn("Erreur de logique métier lors de la vente : {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Erreur inattendue lors de l'enregistrement de la vente.", e);
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Une erreur technique est survenue. La vente n'a pas pu être enregistrée."));
        }
    }
}
