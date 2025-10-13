package com.example.boutique.enums;

public enum TypeMouvement {
    ENTREE("Entrée"),
    SORTIE_VENTE("Sortie (Vente)"),
    SORTIE_PERTE("Sortie (Perte)");

    private final String libelle;

    TypeMouvement(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
