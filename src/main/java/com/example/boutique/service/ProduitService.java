package com.example.boutique.service;

import com.example.boutique.dto.ProductBatchDto;
import com.example.boutique.dto.ProduitDto;
import com.example.boutique.model.MouvementStock;
import com.example.boutique.model.Produit;
import com.example.boutique.repository.ProduitRepository;
import com.example.boutique.enums.TypeMouvement;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ProduitService {

    private final ProduitRepository produitRepository;
    private final StockService stockService;

    public ProduitService(ProduitRepository produitRepository, StockService stockService) {
        this.produitRepository = produitRepository;
        this.stockService = stockService;
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveNewProductBatch(ProductBatchDto productBatchDto) {
        // Barcode validation
        for (ProduitDto dto : productBatchDto.getProduits()) {
            if (dto.getCodeBarres() != null && !dto.getCodeBarres().isEmpty()) {
                Optional<Produit> existing = produitRepository.findByCodeBarres(dto.getCodeBarres());
                if (existing.isPresent()) {
                    throw new IllegalArgumentException("Le code-barres '" + dto.getCodeBarres() + "' existe déjà pour le produit '" + existing.get().getNom() + "'.");
                }
            }
        }

        List<Produit> produitsToSave = new ArrayList<>();
        for (ProduitDto dto : productBatchDto.getProduits()) {
            Produit produit = new Produit();
            produit.setNom(dto.getNom());
            produit.setCodeBarres(dto.getCodeBarres());
            produit.setPrixAchat(BigDecimal.valueOf(dto.getPrixAchat()));
            produit.setPrixVenteUnitaire(BigDecimal.valueOf(dto.getPrixVenteUnitaire()));
            if (dto.getPrixPromotionnel() != null) {
                produit.setPrixPromotionnel(BigDecimal.valueOf(dto.getPrixPromotionnel()));
            }
            produit.setPromotionActive(dto.isPromotionActive());
            produit.setCategorie(dto.getCategorie());
            produit.setQuantiteEnStock(0); // Initial stock is 0 before movement
            produit.setDatePeremption(dto.getDatePeremption());
            produit.setNomFournisseur(productBatchDto.getNomFournisseur());
            produit.setNumeroFacture(productBatchDto.getNumeroFacture());
            produitsToSave.add(produit);
        }

        List<Produit> savedProduits = produitRepository.saveAll(produitsToSave);

        for (int i = 0; i < savedProduits.size(); i++) {
            Produit produit = savedProduits.get(i);
            ProduitDto dto = productBatchDto.getProduits().get(i);

            if (dto.getQuantiteEnStock() > 0) {
                MouvementStock mouvement = new MouvementStock();
                mouvement.setProduit(produit);
                mouvement.setQuantite(dto.getQuantiteEnStock());
                mouvement.setTypeMouvement(TypeMouvement.ENTREE);
                mouvement.setDateMouvement(LocalDateTime.now());
                mouvement.setDescription("Stock initial");
                stockService.enregistrerMouvement(mouvement);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Produit saveProduit(Produit produit) {
        // Check for duplicate barcode before saving
        if (produit.getCodeBarres() != null && !produit.getCodeBarres().isEmpty()) {
            Optional<Produit> existing = produitRepository.findByCodeBarres(produit.getCodeBarres());
            // If a product with this barcode exists, and it's not the same product we are editing
            if (existing.isPresent() && !existing.get().getId().equals(produit.getId())) {
                throw new IllegalArgumentException("Le code-barres '" + produit.getCodeBarres() + "' est déjà utilisé par le produit '" + existing.get().getNom() + "'.");
            }
        }
        return produitRepository.save(produit);
    }
}
