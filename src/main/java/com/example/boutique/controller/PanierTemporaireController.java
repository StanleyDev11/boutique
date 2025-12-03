package com.example.boutique.controller;

import com.example.boutique.dto.PanierTemporaireDto;
import com.example.boutique.model.Utilisateur;
import com.example.boutique.service.PanierTemporaireService;
import com.example.boutique.utils.UserUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/paniers")
public class PanierTemporaireController {

    private final PanierTemporaireService panierTemporaireService;
    private final UserUtils userUtils;

    public PanierTemporaireController(PanierTemporaireService panierTemporaireService, UserUtils userUtils) {
        this.panierTemporaireService = panierTemporaireService;
        this.userUtils = userUtils;
    }

    @GetMapping
    public ResponseEntity<List<PanierTemporaireDto>> getPaniers(Authentication authentication) {
        Utilisateur utilisateur = userUtils.getCurrentUser(authentication);
        List<PanierTemporaireDto> paniers = panierTemporaireService.getPaniersForUser(utilisateur);
        return ResponseEntity.ok(paniers);
    }

    @PostMapping
    public ResponseEntity<PanierTemporaireDto> savePanier(@RequestBody PanierTemporaireDto panierDto, Authentication authentication) {
        Utilisateur utilisateur = userUtils.getCurrentUser(authentication);
        PanierTemporaireDto savedPanier = panierTemporaireService.savePanier(panierDto, utilisateur);
        return ResponseEntity.ok(savedPanier);
    }

    @DeleteMapping("/{tabId}")
    public ResponseEntity<Void> deletePanier(@PathVariable Long tabId, Authentication authentication) {
        Utilisateur utilisateur = userUtils.getCurrentUser(authentication);
        panierTemporaireService.deletePanier(tabId, utilisateur);
        return ResponseEntity.noContent().build();
    }
}
