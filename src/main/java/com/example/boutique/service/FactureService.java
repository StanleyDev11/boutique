package com.example.boutique.service;

import com.example.boutique.dto.MouvementStockBatchDto;
import com.example.boutique.dto.MouvementStockItemDto;
import com.example.boutique.model.Facture;
import com.example.boutique.model.LigneFacture;
import com.example.boutique.model.Produit;
import com.example.boutique.repository.FactureRepository;
import com.example.boutique.repository.LigneFactureRepository;
import com.example.boutique.repository.ProduitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class FactureService {

    private final FactureRepository factureRepository;
    private final LigneFactureRepository ligneFactureRepository;
    private final ProduitRepository produitRepository;

    public FactureService(FactureRepository factureRepository, LigneFactureRepository ligneFactureRepository, ProduitRepository produitRepository) {
        this.factureRepository = factureRepository;
        this.ligneFactureRepository = ligneFactureRepository;
        this.produitRepository = produitRepository;
    }

    @Transactional(rollbackFor = Exception.class)
    public Facture creerFactureAchat(MouvementStockBatchDto batchDto) {
        // 1. Validate invoice number uniqueness
        if (batchDto.getNumeroFacture() != null && !batchDto.getNumeroFacture().isBlank()) {
            if (factureRepository.existsByNumeroFacture(batchDto.getNumeroFacture())) {
                throw new IllegalArgumentException("Le numéro de facture '" + batchDto.getNumeroFacture() + "' existe déjà.");
            }
        } else {
            throw new IllegalArgumentException("Le numéro de facture est obligatoire pour créer une facture.");
        }

        // 2. Create and save the Facture entity
        Facture facture = new Facture();
        facture.setNumeroFacture(batchDto.getNumeroFacture());
        facture.setNomFournisseur(batchDto.getNomFournisseur());
        facture.setDateFacture(LocalDateTime.now());
        
        // Save the facture first to get an ID
        Facture savedFacture = factureRepository.save(facture);
        
        BigDecimal montantTotalFacture = BigDecimal.ZERO;
        List<LigneFacture> lignes = new ArrayList<>();

        // 3. Create LigneFacture entities and update total amount
        for (MouvementStockItemDto itemDto : batchDto.getMouvements()) {
            Produit produit = produitRepository.findById(itemDto.getProduitId())
                    .orElseThrow(() -> new IllegalArgumentException("Produit non trouvé avec l'ID: " + itemDto.getProduitId()));

            LigneFacture ligne = new LigneFacture();
            ligne.setFacture(savedFacture);
            ligne.setProduit(produit);
            ligne.setQuantite(itemDto.getQuantite());
            
            // Use the purchase price from the product for the invoice line
            BigDecimal prixAchat = produit.getPrixAchat();
            if (prixAchat == null) {
                throw new IllegalStateException("Le produit '" + produit.getNom() + "' n'a pas de prix d'achat défini.");
            }
            ligne.setPrixUnitaire(prixAchat);
            
            BigDecimal montantLigne = prixAchat.multiply(itemDto.getQuantite());
            ligne.setMontantTotalLigne(montantLigne);
            
            lignes.add(ligne);
            montantTotalFacture = montantTotalFacture.add(montantLigne);
        }
        
        ligneFactureRepository.saveAll(lignes);

        // 4. Update the total amount on the facture and return it
        savedFacture.setMontantTotal(montantTotalFacture);
        return factureRepository.save(savedFacture);
    }

    @Transactional
    public Facture findOrCreateFacture(String numeroFacture, String nomFournisseur) {
        if (numeroFacture == null || numeroFacture.isBlank()) {
            return null; // No invoice if no number is provided
        }

        return factureRepository.findByNumeroFacture(numeroFacture)
                .orElseGet(() -> {
                    Facture newFacture = new Facture();
                    newFacture.setNumeroFacture(numeroFacture);
                    newFacture.setNomFournisseur(nomFournisseur);
                    newFacture.setDateFacture(LocalDateTime.now());
                    // Total amount is not calculated for single movements, can be updated later if needed.
                    return factureRepository.save(newFacture);
                });
    }
}
