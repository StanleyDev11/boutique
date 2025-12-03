package com.example.boutique.service;

import com.example.boutique.dto.ProductBatchDto;
import com.example.boutique.dto.ProduitDto;
import com.example.boutique.model.MouvementStock;
import com.example.boutique.model.Produit;
import com.example.boutique.repository.ProduitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProduitServiceTest {

    @Mock
    private ProduitRepository produitRepository;

    @Mock
    private StockService stockService;

    @InjectMocks
    private ProduitService produitService;

    @BeforeEach
    void setUp() {
        // Initialisation avant chaque test si nécessaire
    }

    @Test
    void saveProduit_shouldSaveNewProductSuccessfully() {
        // Given
        Produit newProduit = new Produit();
        newProduit.setNom("Test Produit");
        newProduit.setCodeBarres("12345");

        when(produitRepository.findByCodeBarres("12345")).thenReturn(Optional.empty());
        when(produitRepository.save(any(Produit.class))).thenReturn(newProduit);

        // When
        Produit savedProduit = produitService.saveProduit(newProduit);

        // Then
        assertNotNull(savedProduit);
        assertEquals("Test Produit", savedProduit.getNom());
        verify(produitRepository, times(1)).findByCodeBarres("12345");
        verify(produitRepository, times(1)).save(newProduit);
    }

    @Test
    void saveProduit_shouldUpdateExistingProductSuccessfully() {
        // Given
        Produit existingProduit = new Produit();
        existingProduit.setId(1L);
        existingProduit.setNom("Old Name");
        existingProduit.setCodeBarres("ABC");

        Produit updatedProduit = new Produit();
        updatedProduit.setId(1L);
        updatedProduit.setNom("New Name");
        updatedProduit.setCodeBarres("ABC"); // Same barcode

        when(produitRepository.findByCodeBarres("ABC")).thenReturn(Optional.of(existingProduit));
        when(produitRepository.save(any(Produit.class))).thenReturn(updatedProduit);

        // When
        Produit result = produitService.saveProduit(updatedProduit);

        // Then
        assertNotNull(result);
        assertEquals("New Name", result.getNom());
        verify(produitRepository, times(1)).findByCodeBarres("ABC");
        verify(produitRepository, times(1)).save(updatedProduit);
    }

    @Test
    void saveProduit_shouldThrowExceptionWhenNewProductBarcodeAlreadyExists() {
        // Given
        Produit existingProduit = new Produit();
        existingProduit.setId(1L);
        existingProduit.setNom("Existing Product");
        existingProduit.setCodeBarres("12345");

        Produit newProduit = new Produit();
        newProduit.setNom("New Product");
        newProduit.setCodeBarres("12345");

        when(produitRepository.findByCodeBarres("12345")).thenReturn(Optional.of(existingProduit));

        // When & Then
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () ->
                produitService.saveProduit(newProduit));
        assertEquals("Le code-barres '12345' est déjà utilisé par le produit 'Existing Product'.", thrown.getMessage());
        verify(produitRepository, times(1)).findByCodeBarres("12345");
        verify(produitRepository, never()).save(any(Produit.class));
    }

    @Test
    void saveProduit_shouldThrowExceptionWhenUpdatedProductBarcodeAlreadyExistsForAnotherProduct() {
        // Given
        Produit productToUpdate = new Produit();
        productToUpdate.setId(1L);
        productToUpdate.setCodeBarres("BARCODE_NEW"); // Trying to change to this

        Produit anotherExistingProduct = new Produit();
        anotherExistingProduct.setId(2L);
        anotherExistingProduct.setNom("Another Product");
        anotherExistingProduct.setCodeBarres("BARCODE_NEW");

        when(produitRepository.findByCodeBarres("BARCODE_NEW")).thenReturn(Optional.of(anotherExistingProduct));

        // When & Then
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () ->
                produitService.saveProduit(productToUpdate));
        assertEquals("Le code-barres 'BARCODE_NEW' est déjà utilisé par le produit 'Another Product'.", thrown.getMessage());
        verify(produitRepository, times(1)).findByCodeBarres("BARCODE_NEW");
        verify(produitRepository, never()).save(any(Produit.class));
    }

    @Test
    void saveNewProductBatch_shouldSaveProductsAndRecordMovementsSuccessfully() {
        // Given
        ProductBatchDto batchDto = new ProductBatchDto();
        batchDto.setNumeroFacture("INV001");
        batchDto.setNomFournisseur("Supplier A");

        ProduitDto dto1 = new ProduitDto();
        dto1.setNom("Batch Product 1");
        dto1.setCodeBarres("BP1");
        dto1.setPrixAchat(BigDecimal.valueOf(10.0));
        dto1.setPrixVenteUnitaire(BigDecimal.valueOf(15.0));
        dto1.setQuantiteEnStock(BigDecimal.valueOf(5));
        dto1.setCategorie("Category A");
        dto1.setDatePeremption(LocalDate.now().plusMonths(6));

        ProduitDto dto2 = new ProduitDto();
        dto2.setNom("Batch Product 2");
        dto2.setCodeBarres("BP2");
        dto2.setPrixAchat(BigDecimal.valueOf(20.0));
        dto2.setPrixVenteUnitaire(BigDecimal.valueOf(25.0));
        dto2.setQuantiteEnStock(BigDecimal.ZERO); // No initial stock movement
        dto2.setCategorie("Category B");
        dto2.setDatePeremption(LocalDate.now().plusMonths(3));

        batchDto.setProduits(List.of(dto1, dto2));

        Produit savedProduit1 = new Produit();
        savedProduit1.setId(10L);
        savedProduit1.setNom("Batch Product 1");

        Produit savedProduit2 = new Produit();
        savedProduit2.setId(11L);
        savedProduit2.setNom("Batch Product 2");

        when(produitRepository.findByCodeBarres("BP1")).thenReturn(Optional.empty());
        when(produitRepository.findByCodeBarres("BP2")).thenReturn(Optional.empty());
        when(produitRepository.saveAll(anyList())).thenReturn(List.of(savedProduit1, savedProduit2));
        doNothing().when(stockService).enregistrerMouvement(any(MouvementStock.class), anyString(), anyString());

        // When
        produitService.saveNewProductBatch(batchDto);

        // Then
        verify(produitRepository, times(1)).findByCodeBarres("BP1");
        verify(produitRepository, times(1)).findByCodeBarres("BP2");
        verify(produitRepository, times(1)).saveAll(anyList());
        verify(stockService, times(1)).enregistrerMouvement(any(MouvementStock.class), anyString(), anyString()); // Only for dto1 (quantiteEnStock > 0)
    }

    @Test
    void saveNewProductBatch_shouldThrowExceptionWhenBatchProductBarcodeAlreadyExists() {
        // Given
        ProductBatchDto batchDto = new ProductBatchDto();
        batchDto.setNumeroFacture("INV002");
        batchDto.setNomFournisseur("Supplier B");

        ProduitDto dto = new ProduitDto();
        dto.setNom("Batch Product Existing");
        dto.setCodeBarres("EXISTING_BARCODE");
        dto.setPrixAchat(BigDecimal.valueOf(10.0));
        dto.setPrixVenteUnitaire(BigDecimal.valueOf(15.0));
        dto.setQuantiteEnStock(BigDecimal.valueOf(5));

        batchDto.setProduits(Collections.singletonList(dto));

        Produit existingProduit = new Produit();
        existingProduit.setId(1L);
        existingProduit.setNom("Existing Product Name");
        existingProduit.setCodeBarres("EXISTING_BARCODE");

        when(produitRepository.findByCodeBarres("EXISTING_BARCODE")).thenReturn(Optional.of(existingProduit));

        // When & Then
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () ->
                produitService.saveNewProductBatch(batchDto));
        assertEquals("Le code-barres 'EXISTING_BARCODE' existe déjà pour le produit 'Existing Product Name'.", thrown.getMessage());
        verify(produitRepository, times(1)).findByCodeBarres("EXISTING_BARCODE");
        verify(produitRepository, never()).saveAll(anyList());
        verify(stockService, never()).enregistrerMouvement(any(MouvementStock.class), anyString(), anyString());
    }
}
