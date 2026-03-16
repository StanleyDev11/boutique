package com.example.boutique.utils;

import com.example.boutique.model.Utilisateur;
import com.example.boutique.repository.UtilisateurRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserUtils {

    private final UtilisateurRepository utilisateurRepository;

    public UserUtils(UtilisateurRepository utilisateurRepository) {
        this.utilisateurRepository = utilisateurRepository;
    }

    public Utilisateur getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            String username = ((UserDetails) principal).getUsername();
            return utilisateurRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé: " + username));
        }
        return null;
    }

    public static String getCurrentUsername() {
        Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserDetails) {
                return ((UserDetails) principal).getUsername();
            }
            if (principal instanceof String) {
                return (String) principal;
            }
        }
        return null;
    }

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
