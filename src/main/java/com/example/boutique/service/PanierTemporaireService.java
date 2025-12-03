package com.example.boutique.service;

import com.example.boutique.dto.PanierTemporaireDto;
import com.example.boutique.model.PanierTemporaire;
import com.example.boutique.model.Utilisateur;
import com.example.boutique.repository.PanierTemporaireRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PanierTemporaireService {

    private final PanierTemporaireRepository panierTemporaireRepository;

    public PanierTemporaireService(PanierTemporaireRepository panierTemporaireRepository) {
        this.panierTemporaireRepository = panierTemporaireRepository;
    }

    @Transactional(readOnly = true)
    public List<PanierTemporaireDto> getPaniersForUser(Utilisateur utilisateur) {
        return panierTemporaireRepository.findByUtilisateurId(utilisateur.getId()).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public PanierTemporaireDto savePanier(PanierTemporaireDto panierDto, Utilisateur utilisateur) {
        PanierTemporaire panier = panierTemporaireRepository
                .findByTabIdAndUtilisateurId(panierDto.getTabId(), utilisateur.getId())
                .orElse(new PanierTemporaire());

        panier.setUtilisateur(utilisateur);
        panier.setTabId(panierDto.getTabId());
        panier.setCartData(panierDto.getCartData());

        PanierTemporaire savedPanier = panierTemporaireRepository.save(panier);
        return convertToDto(savedPanier);
    }

    @Transactional
    public void deletePanier(Long tabId, Utilisateur utilisateur) {
        panierTemporaireRepository.deleteByTabIdAndUtilisateurId(tabId, utilisateur.getId());
    }

    private PanierTemporaireDto convertToDto(PanierTemporaire panier) {
        return new PanierTemporaireDto(panier.getId(), panier.getTabId(), panier.getCartData());
    }
}
