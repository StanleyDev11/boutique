package com.example.boutique.utils;

import com.example.boutique.model.Produit;

import java.util.List;

public class ProductUtils {

    public static long countOutOfStock(List<Produit> produits) {
        if (produits == null) {
            return 0;
        }
        return produits.stream()
                .filter(produit -> produit.getQuantiteEnStock() == 0)
                .count();
    }

    public static long countLowStock(List<Produit> produits, int seuil) {
        if (produits == null) {
            return 0;
        }
        return produits.stream()
                .filter(produit -> produit.getQuantiteEnStock() > 0 && produit.getQuantiteEnStock() <= seuil)
                .count();
    }
}
