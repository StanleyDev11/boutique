package com.example.boutique.service;

import com.example.boutique.dto.FactureDto;
import com.example.boutique.dto.ProductBatchDto;
import com.example.boutique.dto.ProduitDto;
import com.example.boutique.dto.ProduitFactureDto;
import com.example.boutique.enums.TypeMouvement;
import com.example.boutique.model.Facture;
import com.example.boutique.model.LigneFacture;
import com.example.boutique.model.MouvementStock;
import com.example.boutique.model.Produit;
import com.example.boutique.repository.FactureRepository;
import com.example.boutique.repository.MouvementStockRepository;
import com.example.boutique.repository.ProduitRepository;
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
    private final MouvementStockRepository mouvementStockRepository;
    private final FactureRepository factureRepository;


    public ProduitService(ProduitRepository produitRepository, StockService stockService, MouvementStockRepository mouvementStockRepository, FactureRepository factureRepository) {
        this.produitRepository = produitRepository;
        this.stockService = stockService;
        this.mouvementStockRepository = mouvementStockRepository;
        this.factureRepository = factureRepository;
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveNewProductBatch(ProductBatchDto productBatchDto) {
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
            produit.setPrixAchat(dto.getPrixAchat());
            produit.setPrixVenteUnitaire(dto.getPrixVenteUnitaire());
            if (dto.getPrixPromotionnel() != null) {
                produit.setPrixPromotionnel(dto.getPrixPromotionnel());
            }
            produit.setPromotionActive(dto.isPromotionActive());
            produit.setCategorie(dto.getCategorie());
            produit.setQuantiteEnStock(BigDecimal.ZERO);
            produit.setUniteDeVente(dto.getUniteDeVente());
            produit.setDatePeremption(dto.getDatePeremption());
            produit.setNomFournisseur(productBatchDto.getNomFournisseur());
            produit.setNumeroFacture(productBatchDto.getNumeroFacture());
            produitsToSave.add(produit);
        }

        List<Produit> savedProduits = produitRepository.saveAll(produitsToSave);

        for (int i = 0; i < savedProduits.size(); i++) {
            Produit produit = savedProduits.get(i);
            ProduitDto dto = productBatchDto.getProduits().get(i);

            if (dto.getQuantiteEnStock() != null && dto.getQuantiteEnStock().compareTo(BigDecimal.ZERO) > 0) {
                MouvementStock mouvement = new MouvementStock();
                mouvement.setProduit(produit);
                mouvement.setQuantite(dto.getQuantiteEnStock());
                mouvement.setTypeMouvement(TypeMouvement.ENTREE);
                mouvement.setDateMouvement(LocalDateTime.now());
                mouvement.setDescription("Achat facture: " + productBatchDto.getNumeroFacture());
                stockService.enregistrerMouvement(mouvement, productBatchDto.getNumeroFacture(), productBatchDto.getNomFournisseur());
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Produit saveProduit(Produit produit) {
        if (produit.getCodeBarres() != null && !produit.getCodeBarres().isEmpty()) {
            Optional<Produit> existing = produitRepository.findByCodeBarres(produit.getCodeBarres());
            if (existing.isPresent() && !existing.get().getId().equals(produit.getId())) {
                throw new IllegalArgumentException("Le code-barres '" + produit.getCodeBarres() + "' est déjà utilisé par le produit '" + existing.get().getNom() + "'.");
            }
        }
        return produitRepository.save(produit);
    }

    @Transactional(rollbackFor = Exception.class)
    public void createFacture(FactureDto factureDto) {
        Facture facture = new Facture();
        facture.setNumeroFacture(factureDto.getNumeroFacture());
        facture.setNomFournisseur(factureDto.getNomFournisseur());
        facture.setDateFacture(factureDto.getDateFacture() != null ? factureDto.getDateFacture().atStartOfDay() : LocalDateTime.now());
        facture.setMontantTotal(BigDecimal.ZERO);
        Facture savedFacture = factureRepository.save(facture);

        BigDecimal montantTotalFacture = BigDecimal.ZERO;
        List<LigneFacture> lignesFacture = new ArrayList<>();

        for (ProduitFactureDto produitDto : factureDto.getProduits()) {
            if (produitDto.getId() == null || produitDto.getQuantite() <= 0) continue;

            Produit produit = produitRepository.findById(produitDto.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Produit non trouvé pour l'ID: " + produitDto.getId()));

            BigDecimal quantite = BigDecimal.valueOf(produitDto.getQuantite());
            BigDecimal prixAchat = produitDto.getPrixAchat();

            if (prixAchat == null) {
                throw new IllegalArgumentException("Le prix d'achat est requis pour le produit: " + produit.getNom());
            }

            BigDecimal montantLigne = prixAchat.multiply(quantite);

            LigneFacture ligne = new LigneFacture();
            ligne.setFacture(savedFacture);
            ligne.setProduit(produit);
            ligne.setQuantite(quantite);
            ligne.setPrixUnitaire(prixAchat);
            ligne.setMontantTotalLigne(montantLigne);
            lignesFacture.add(ligne);

            montantTotalFacture = montantTotalFacture.add(montantLigne);

            produit.setPrixAchat(prixAchat);
            if (produitDto.getPrixVenteUnitaire() != null) {
                produit.setPrixVenteUnitaire(produitDto.getPrixVenteUnitaire());
            }
            produit.setDatePeremption(produitDto.getDatePeremption());

            BigDecimal stockActuel = produit.getQuantiteEnStock();
            BigDecimal nouveauStock = stockActuel.add(quantite);
            produit.setQuantiteEnStock(nouveauStock);

            produitRepository.save(produit);

            MouvementStock mouvement = new MouvementStock();
            mouvement.setProduit(produit);
            mouvement.setQuantite(quantite);
            mouvement.setTypeMouvement(TypeMouvement.ENTREE);
            mouvement.setDateMouvement(savedFacture.getDateFacture());
            mouvement.setDescription("Achat facture: " + savedFacture.getNumeroFacture());
            mouvement.setFacture(savedFacture); 

            mouvementStockRepository.save(mouvement);
        }

        savedFacture.setLignes(lignesFacture);
        savedFacture.setMontantTotal(montantTotalFacture);
        factureRepository.save(savedFacture);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteFacture(Long id) {
        Facture facture = factureRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Facture non trouvée avec l'ID: " + id));

        for (LigneFacture ligne : facture.getLignes()) {
            Produit produit = ligne.getProduit();
            BigDecimal quantite = ligne.getQuantite();

            MouvementStock mouvementAnnulation = new MouvementStock();
            mouvementAnnulation.setProduit(produit);
            mouvementAnnulation.setQuantite(quantite);
            mouvementAnnulation.setTypeMouvement(TypeMouvement.SORTIE_PERTE);
            mouvementAnnulation.setDateMouvement(LocalDateTime.now());
            mouvementAnnulation.setDescription("Annulation facture N°: " + facture.getNumeroFacture());
            
            stockService.enregistrerMouvement(mouvementAnnulation);
        }

        List<MouvementStock> mouvementsLies = mouvementStockRepository.findByFacture(facture);
        for (MouvementStock mv : mouvementsLies) {
            mv.setFacture(null);
            mouvementStockRepository.save(mv);
        }

        factureRepository.deleteById(id);
    }
}