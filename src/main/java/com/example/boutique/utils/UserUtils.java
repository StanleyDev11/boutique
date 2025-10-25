package com.example.boutique.utils;

import com.example.boutique.model.Utilisateur;

import java.util.List;

public class UserUtils {

    public static long countAdmins(List<Utilisateur> utilisateurs) {
        if (utilisateurs == null) {
            return 0;
        }
        return utilisateurs.stream()
                .filter(utilisateur -> utilisateur.getRoles() != null && utilisateur.getRoles().contains("ROLE_ADMIN"))
                .count();
    }

    public static long countUsers(List<Utilisateur> utilisateurs) {
        if (utilisateurs == null) {
            return 0;
        }
        return utilisateurs.stream()
                .filter(utilisateur -> utilisateur.getRoles() != null && utilisateur.getRoles().contains("ROLE_USER"))
                .count();
    }

    public static long countCaissiers(List<Utilisateur> utilisateurs) {
        if (utilisateurs == null) {
            return 0;
        }
        return utilisateurs.stream()
                .filter(utilisateur -> utilisateur.getRoles() != null && utilisateur.getRoles().contains("ROLE_CAISSIER"))
                .count();
    }

    public static long countGestionnaires(List<Utilisateur> utilisateurs) {
        if (utilisateurs == null) {
            return 0;
        }
        return utilisateurs.stream()
                .filter(utilisateur -> utilisateur.getRoles() != null && utilisateur.getRoles().contains("ROLE_GESTIONNAIRE"))
                .count();
    }
}
