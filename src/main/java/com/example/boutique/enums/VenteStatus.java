package com.example.boutique.enums;

public enum VenteStatus {
    COMPLETED("Complétée"),
    CANCELLED("Annulée");

    private final String displayName;

    VenteStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
