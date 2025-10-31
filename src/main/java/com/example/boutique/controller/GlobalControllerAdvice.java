package com.example.boutique.controller;

import com.example.boutique.model.SessionCaisse;
import com.example.boutique.model.Utilisateur;
import com.example.boutique.repository.SessionCaisseRepository;
import com.example.boutique.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Optional;

@ControllerAdvice
public class GlobalControllerAdvice {

    @Autowired
    private SessionCaisseRepository sessionCaisseRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @ModelAttribute("isSessionActive")
    public boolean isSessionActive() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            Optional<Utilisateur> utilisateurOpt = utilisateurRepository.findByUsername(username);

            if (utilisateurOpt.isPresent()) {
                Utilisateur utilisateur = utilisateurOpt.get();
                if (utilisateur.getRoles().contains("CAISSIER")) {
                    Optional<SessionCaisse> activeSession = sessionCaisseRepository.findFirstByUtilisateurAndDateFermetureIsNullOrderByDateOuvertureDesc(utilisateur);
                    return activeSession.isPresent();
                }
            }
        }
        return false;
    }
}
