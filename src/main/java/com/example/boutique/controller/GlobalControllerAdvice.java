package com.example.boutique.controller;

import com.example.boutique.repository.SessionCaisseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

    @Autowired
    private SessionCaisseRepository sessionCaisseRepository;


    @ModelAttribute("isSessionActive")
    public boolean isSessionActive() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof UserDetails) {
            // Vérifier si l'utilisateur a le rôle de caissier
            boolean isCashier = authentication.getAuthorities().stream()
                    .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_CAISSIER"));

            if (isCashier) {
                // Si c'est un caissier, vérifier si N'IMPORTE QUELLE session est ouverte
                return sessionCaisseRepository.findFirstByDateFermetureIsNull().isPresent();
            }
        }
        return false;
    }
}
