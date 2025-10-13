package com.example.boutique.service;

import com.example.boutique.model.MouvementStock;
import com.example.boutique.model.Produit;
import com.example.boutique.repository.MouvementStockRepository;
import com.example.boutique.repository.ProduitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class StockService {

    private final MouvementStockRepository mouvementStockRepository;
    private final ProduitRepository produitRepository;

    public StockService(MouvementStockRepository mouvementStockRepository, ProduitRepository produitRepository) {
        this.mouvementStockRepository = mouvementStockRepository;
        this.produitRepository = produitRepository;
    }

    public void enregistrerMouvement(MouvementStock mouvement) {
        // 1. Récupérer le produit concerné pour s'assurer qu'il existe
        Produit produit = produitRepository.findById(mouvement.getProduit().getId())
                .orElseThrow(() -> new IllegalArgumentException("Produit non trouvé"));

        int quantiteMouvement = mouvement.getQuantite();
        int stockActuel = produit.getQuantiteEnStock();
        int nouveauStock;

        // 2. Mettre à jour la quantité en stock en fonction du type de mouvement
        switch (mouvement.getTypeMouvement()) {
            case ENTREE:
                nouveauStock = stockActuel + quantiteMouvement;
                break;
            case SORTIE_VENTE:
            case SORTIE_PERTE:
                if (stockActuel < quantiteMouvement) {
                    throw new IllegalStateException("Quantité en stock insuffisante pour le produit: " + produit.getNom());
                }
                nouveauStock = stockActuel - quantiteMouvement;
                break;
            default:
                throw new IllegalArgumentException("Type de mouvement non supporté");
        }

        produit.setQuantiteEnStock(nouveauStock);

        // 3. Sauvegarder le produit mis à jour et le nouveau mouvement
        produitRepository.save(produit);
        mouvementStockRepository.save(mouvement);
    }
}
