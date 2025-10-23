package com.example.boutique.service;

import com.example.boutique.dto.CartItemDto;
import com.example.boutique.dto.VenteRequestDto;
import com.example.boutique.enums.TypeMouvement;
import com.example.boutique.model.Client;
import com.example.boutique.model.MouvementStock;
import com.example.boutique.model.Produit;
import com.example.boutique.model.LigneVente;
import com.example.boutique.model.Vente;


import com.example.boutique.repository.ClientRepository;
import com.example.boutique.repository.MouvementStockRepository;
import com.example.boutique.repository.ProduitRepository;
import com.example.boutique.repository.VenteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class StockService {

    private final MouvementStockRepository mouvementStockRepository;
    private final ProduitRepository produitRepository;
    private final VenteRepository venteRepository;
    private final ClientRepository clientRepository;
    private final com.example.boutique.repository.LigneVenteRepository ligneVenteRepository;

    public StockService(MouvementStockRepository mouvementStockRepository, ProduitRepository produitRepository, VenteRepository venteRepository, ClientRepository clientRepository, com.example.boutique.repository.LigneVenteRepository ligneVenteRepository) {
        this.mouvementStockRepository = mouvementStockRepository;
        this.produitRepository = produitRepository;
        this.venteRepository = venteRepository;
        this.clientRepository = clientRepository;
        this.ligneVenteRepository = ligneVenteRepository;
    }

    public void enregistrerMouvement(MouvementStock mouvement) {
        Produit produit = produitRepository.findById(mouvement.getProduit().getId())
                .orElseThrow(() -> new IllegalArgumentException("Produit non trouvé"));

        int quantiteMouvement = mouvement.getQuantite();
        int stockActuel = produit.getQuantiteEnStock();
        int nouveauStock;

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
        produitRepository.save(produit);
        mouvementStockRepository.save(mouvement);
    }

    public void enregistrerVente(VenteRequestDto venteRequest) {
        List<CartItemDto> cartItems = venteRequest.getCart();
        BigDecimal totalBrut = BigDecimal.ZERO;

        // First, validate stock and calculate total from backend data for security
        for (CartItemDto item : cartItems) {
            Produit produit = produitRepository.findById(item.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Produit non trouvé avec l'ID: " + item.getId()));
            if (produit.getQuantiteEnStock() < item.getQuantity()) {
                throw new IllegalStateException("Stock insuffisant pour le produit: " + produit.getNom());
            }
            totalBrut = totalBrut.add(produit.getPrixVenteUnitaire().multiply(new BigDecimal(item.getQuantity())));
        }

        // Handle discount
        BigDecimal remise = venteRequest.getDiscountAmount() != null ? venteRequest.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal totalFinal = totalBrut.subtract(remise);
        if (totalFinal.compareTo(BigDecimal.ZERO) < 0) {
            totalFinal = BigDecimal.ZERO; // Ensure final total is not negative
        }

        // Handle client
        Client client = null;
        if (venteRequest.getClientId() != null) {
            client = clientRepository.findById(venteRequest.getClientId())
                    .orElse(null); // Or throw an exception if client MUST exist
        }

        // Create and save the Vente entity
        Vente vente = new Vente();
        vente.setDateVente(LocalDateTime.now());
        vente.setClient(client);
        vente.setTotal(totalBrut);
        vente.setTotalBrut(totalBrut);
        vente.setRemise(remise);
        vente.setTotalNet(totalFinal);
        vente.setTotalFinal(totalFinal);
        vente.setTypeVente(venteRequest.getSaleType());
        vente.setMoyenPaiement(venteRequest.getPaymentMethod());
        venteRepository.save(vente);

        // Then, update stock and create stock movements
        for (CartItemDto item : cartItems) {
            Produit produit = produitRepository.findById(item.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Produit non trouvé avec l'ID: " + item.getId())); // Should not happen as we checked before

            LigneVente ligneVente = new LigneVente();
            ligneVente.setVente(vente);
            ligneVente.setProduit(produit);
            ligneVente.setQuantite(item.getQuantity());
            ligneVente.setPrixUnitaire(produit.getPrixVenteUnitaire());
            ligneVente.setMontantTotal(produit.getPrixVenteUnitaire().multiply(new BigDecimal(item.getQuantity())));
            ligneVenteRepository.save(ligneVente);

            // Update product stock
            produit.setQuantiteEnStock(produit.getQuantiteEnStock() - item.getQuantity());
            produitRepository.save(produit);

            // Create stock movement record
            MouvementStock mouvementStock = new MouvementStock();
            mouvementStock.setProduit(produit);
            mouvementStock.setQuantite(item.getQuantity());
            mouvementStock.setTypeMouvement(TypeMouvement.SORTIE_VENTE);
            mouvementStock.setDateMouvement(LocalDateTime.now());
            mouvementStock.setDescription("Vente de " + item.getQuantity() + " unités. Réf Vente ID: " + vente.getId());
            mouvementStockRepository.save(mouvementStock);
        }
    }
}
