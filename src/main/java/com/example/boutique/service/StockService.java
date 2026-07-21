package com.example.boutique.service;

import com.example.boutique.dto.CartItemDto;
import com.example.boutique.dto.MouvementStockBatchDto;
import com.example.boutique.dto.MouvementStockItemDto;
import com.example.boutique.dto.VenteRequestDto;
import com.example.boutique.enums.TypeMouvement;
import com.example.boutique.model.*;


import com.example.boutique.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class StockService {

    private final MouvementStockRepository mouvementStockRepository;
    private final ProduitRepository produitRepository;
    private final VenteRepository venteRepository;
    private final ClientRepository clientRepository;
    private final LigneVenteRepository ligneVenteRepository;
    private final SessionCaisseRepository sessionCaisseRepository;
    private final FactureService factureService;


    public StockService(MouvementStockRepository mouvementStockRepository, ProduitRepository produitRepository, VenteRepository venteRepository, ClientRepository clientRepository, LigneVenteRepository ligneVenteRepository, SessionCaisseRepository sessionCaisseRepository, FactureService factureService) {
        this.mouvementStockRepository = mouvementStockRepository;
        this.produitRepository = produitRepository;
        this.venteRepository = venteRepository;
        this.clientRepository = clientRepository;
        this.ligneVenteRepository = ligneVenteRepository;
        this.sessionCaisseRepository = sessionCaisseRepository;
        this.factureService = factureService;
    }

    @Transactional(rollbackFor = Exception.class)
    public void enregistrerMouvementBatch(MouvementStockBatchDto batchDto) {
        if (batchDto.getMouvements() == null || batchDto.getMouvements().isEmpty()) {
            throw new IllegalArgumentException("La liste des mouvements ne peut pas être vide.");
        }

        // 1. Create Invoice if it's an incoming movement
        Facture facture = null;
        if (batchDto.getTypeMouvement() == TypeMouvement.ENTREE && batchDto.getNumeroFacture() != null && !batchDto.getNumeroFacture().isBlank()) {
            facture = factureService.creerFactureAchat(batchDto);
        }

        // 2. Update stock and create movements
        List<MouvementStock> mouvementsASauvegarder = new ArrayList<>();
        List<Long> produitIds = batchDto.getMouvements().stream()
                .map(MouvementStockItemDto::getProduitId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, Produit> produitsMap = new HashMap<>();
        for (Long produitId : produitIds) {
            Produit produit = produitRepository.findByIdForUpdate(produitId)
                    .orElseThrow(() -> new IllegalArgumentException("Produit non trouvé avec l'ID: " + produitId));
            produitsMap.put(produitId, produit);
        }

        for (MouvementStockItemDto itemDto : batchDto.getMouvements()) {
            Produit produit = produitsMap.get(itemDto.getProduitId());
            BigDecimal quantiteMouvement = itemDto.getQuantite();
            if (quantiteMouvement == null || quantiteMouvement.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("La quantité pour le produit '" + produit.getNom() + "' doit être positive.");
            }

            // Update product's purchase price if it's an incoming movement
            // This assumes the DTO can carry the new purchase price, which it currently does not.
            // For now, we rely on the price already on the product.

            BigDecimal stockActuel = produit.getQuantiteEnStock();
            BigDecimal nouveauStock;

            switch (batchDto.getTypeMouvement()) {
                case ENTREE:
                    nouveauStock = stockActuel.add(quantiteMouvement);
                    break;
                case SORTIE_VENTE:
                case SORTIE_PERTE:
                case PERIME:
                case CASSE_DEFECTUEUX:
                case AVOIR:
                    if (stockActuel.compareTo(quantiteMouvement) < 0) {
                        throw new IllegalStateException("Quantité en stock (" + stockActuel + ") insuffisante pour le produit: " + produit.getNom());
                    }
                    nouveauStock = stockActuel.subtract(quantiteMouvement);
                    break;
                default:
                    throw new IllegalArgumentException("Type de mouvement non supporté: " + batchDto.getTypeMouvement());
            }
            produit.setQuantiteEnStock(nouveauStock);

            MouvementStock mouvement = new MouvementStock();
            mouvement.setProduit(produit);
            mouvement.setQuantite(quantiteMouvement);
            mouvement.setTypeMouvement(batchDto.getTypeMouvement());
            mouvement.setFacture(facture); // Link to the new invoice
            mouvement.setDescription(batchDto.getDescription());
            mouvement.setDateMouvement(LocalDateTime.now());
            mouvementsASauvegarder.add(mouvement);
        }

        produitRepository.saveAll(produitsMap.values());
        mouvementStockRepository.saveAll(mouvementsASauvegarder);
    }

    // Nouvelle méthode simplifiée pour la mise à jour du stock
    public void enregistrerMouvement(MouvementStock mouvement) {
        Produit produit = produitRepository.findByIdForUpdate(mouvement.getProduit().getId())
                .orElseThrow(() -> new IllegalArgumentException("Produit non trouvé pour l'ID: " + mouvement.getProduit().getId()));

        BigDecimal quantiteMouvement = mouvement.getQuantite();
        BigDecimal stockActuel = produit.getQuantiteEnStock();
        BigDecimal nouveauStock;

        switch (mouvement.getTypeMouvement()) {
            case ENTREE:
                nouveauStock = stockActuel.add(quantiteMouvement);
                break;
            case SORTIE_VENTE:
            case SORTIE_PERTE:
            case PERIME:
            case CASSE_DEFECTUEUX:
            case AVOIR:
                if (stockActuel.compareTo(quantiteMouvement) < 0) {
                    throw new IllegalStateException("Quantité en stock (" + stockActuel + ") insuffisante pour le produit: " + produit.getNom());
                }
                nouveauStock = stockActuel.subtract(quantiteMouvement);
                break;
            default:
                throw new IllegalArgumentException("Type de mouvement non supporté: " + mouvement.getTypeMouvement());
        }

        produit.setQuantiteEnStock(nouveauStock);
        produitRepository.save(produit);
        mouvementStockRepository.save(mouvement);
    }

    public void enregistrerMouvement(MouvementStock mouvement, String numeroFacture, String nomFournisseur) {
        Produit produit = produitRepository.findById(mouvement.getProduit().getId())
                .orElseThrow(() -> new IllegalArgumentException("Produit non trouvé"));

        // Find or create the invoice and associate it with the movement
        if (mouvement.getTypeMouvement() == TypeMouvement.ENTREE) {
            Facture facture = factureService.findOrCreateFacture(numeroFacture, nomFournisseur);
            mouvement.setFacture(facture);

            // If a facture was found or created, add a line item to it
            if (facture != null) {
                factureService.addLigneToFacture(facture, produit, mouvement.getQuantite());
            }
        }

        BigDecimal quantiteMouvement = mouvement.getQuantite();
        BigDecimal stockActuel = produit.getQuantiteEnStock();
        BigDecimal nouveauStock;

        switch (mouvement.getTypeMouvement()) {
            case ENTREE:
                nouveauStock = stockActuel.add(quantiteMouvement);
                break;
            case SORTIE_VENTE:
            case SORTIE_PERTE:
            case PERIME:
            case CASSE_DEFECTUEUX:
            case AVOIR:
                if (stockActuel.compareTo(quantiteMouvement) < 0) {
                    throw new IllegalStateException("Quantité en stock insuffisante pour le produit: " + produit.getNom());
                }
                nouveauStock = stockActuel.subtract(quantiteMouvement);
                break;
            default:
                throw new IllegalArgumentException("Type de mouvement non supporté: " + mouvement.getTypeMouvement());
        }

        produit.setQuantiteEnStock(nouveauStock);
        produitRepository.save(produit);
        mouvementStockRepository.save(mouvement);
    }

    public void enregistrerVente(VenteRequestDto venteRequest, Utilisateur utilisateur) {
        // Récupère la seule session de caisse ouverte, peu importe qui l'a ouverte
        SessionCaisse sessionCaisse = sessionCaisseRepository.findFirstByDateFermetureIsNull()
                .orElseThrow(() -> new IllegalStateException("Aucune session de caisse active n'a été trouvée. Veuillez ouvrir une session avant de faire une vente."));

        List<CartItemDto> cartItems = venteRequest.getCart();
        BigDecimal totalBrut = BigDecimal.ZERO;
        Map<Long, Produit> produitsVerrouilles = new HashMap<>();

        // 1. Verrouiller les produits, valider le stock et calculer le total
        for (CartItemDto item : cartItems) {
            Produit produit = produitRepository.findByIdForUpdate(item.getId()) // Utilise la méthode de verrouillage
                    .orElseThrow(() -> new IllegalArgumentException("Produit non trouvé avec l'ID: " + item.getId()));

            if (produit.getQuantiteEnStock().compareTo(item.getQuantity()) < 0) {
                throw new IllegalStateException("Stock insuffisant pour le produit: " + produit.getNom());
            }

            totalBrut = totalBrut.add(produit.getApplicablePrix().multiply(item.getQuantity()));
            produitsVerrouilles.put(item.getId(), produit);
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

        if (venteRequest.getMontantPaye() != null) {
            BigDecimal montantPaye = venteRequest.getMontantPaye();
            BigDecimal reliquat = montantPaye.subtract(totalFinal);
            vente.setMontantPaye(montantPaye);
            vente.setReliquat(reliquat);
        }

        vente.setDateVente(LocalDateTime.now());
        vente.setClient(client);
        vente.setTotal(totalBrut);
        vente.setTotalBrut(totalBrut);
        vente.setRemise(remise);
        vente.setTotalNet(totalFinal);
        vente.setTotalFinal(totalFinal);
        vente.setTypeVente(venteRequest.getSaleType());
        
        // Convert paymentMethod string to Enum
        try {
            String paymentMethodString = venteRequest.getPaymentMethod();
            if (paymentMethodString == null || paymentMethodString.isBlank()) {
                throw new IllegalArgumentException("Le moyen de paiement est requis.");
            }
            vente.setMoyenPaiement(com.example.boutique.enums.MoyenPaiement.valueOf(paymentMethodString.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Moyen de paiement invalide: " + venteRequest.getPaymentMethod());
        }

        vente.setUtilisateur(utilisateur);
        vente.setSessionCaisse(sessionCaisse);
        vente.setStatus(com.example.boutique.enums.VenteStatus.COMPLETED);
        venteRepository.save(vente);

        // Then, update stock and create stock movements
        for (CartItemDto item : cartItems) {
            Produit produit = produitsVerrouilles.get(item.getId()); // Récupère le produit depuis la Map

            LigneVente ligneVente = new LigneVente();
            ligneVente.setVente(vente);
            ligneVente.setProduit(produit);
            ligneVente.setQuantite(item.getQuantity());
            ligneVente.setPrixUnitaire(produit.getApplicablePrix());
            ligneVente.setMontantTotal(produit.getApplicablePrix().multiply(item.getQuantity()));
            ligneVenteRepository.save(ligneVente);

            // Update product stock
            produit.setQuantiteEnStock(produit.getQuantiteEnStock().subtract(item.getQuantity()));
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

    public void annulerVente(Long venteId, String motifAnnulation) {
        Vente vente = venteRepository.findById(venteId)
                .orElseThrow(() -> new IllegalArgumentException("Vente non trouvée avec l'ID: " + venteId));

        if (vente.getStatus() == com.example.boutique.enums.VenteStatus.CANCELLED) {
            throw new IllegalStateException("Cette vente a déjà été annulée.");
        }

        vente.setStatus(com.example.boutique.enums.VenteStatus.CANCELLED);
        vente.setMotifAnnulation(motifAnnulation);
        venteRepository.save(vente);

        List<LigneVente> lignesVente = ligneVenteRepository.findByVente(vente);

        for (LigneVente ligne : lignesVente) {
            Produit produit = produitRepository.findByIdForUpdate(ligne.getProduit().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Produit non trouvé: " + ligne.getProduit().getNom()));

            // Créer le mouvement de stock pour l'annulation (comme une entrée)
            MouvementStock mouvementStock = new MouvementStock();
            mouvementStock.setProduit(produit);
            mouvementStock.setQuantite(ligne.getQuantite());
            mouvementStock.setTypeMouvement(TypeMouvement.ENTREE); // Utiliser ENTREE
            mouvementStock.setDateMouvement(LocalDateTime.now());
            mouvementStock.setDescription("Retour de stock suite à l'annulation de la vente #" + vente.getId());
            mouvementStock.setUtilisateur(vente.getUtilisateur());

            // La méthode enregistrerMouvement gère déjà la logique d'addition au stock pour le type ENTREE
            enregistrerMouvement(mouvementStock, null, null);
        }
    }
}

