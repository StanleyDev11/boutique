package com.example.boutique.config;

import com.example.boutique.model.SessionCaisse;
import com.example.boutique.model.Utilisateur;
import com.example.boutique.repository.SessionCaisseRepository;
import com.example.boutique.repository.UtilisateurRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private SessionCaisseRepository sessionCaisseRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        Set<String> roles = AuthorityUtils.authorityListToSet(authentication.getAuthorities());

        if (roles.contains("ROLE_ADMIN")) {
            response.sendRedirect("/dashboard");
        } else if (roles.contains("ROLE_GESTIONNAIRE")) {
            response.sendRedirect("/produits");
        } else if (roles.contains("ROLE_CAISSIER")) {
            // Vérifier si une session de caisse est déjà ouverte globalement
            Optional<SessionCaisse> sessionOpt = sessionCaisseRepository.findFirstByDateFermetureIsNull();

            if (sessionOpt.isEmpty()) {
                // Aucune session ouverte dans le magasin, rediriger vers la page d'ouverture
                response.sendRedirect("/caisse/ouvrir");
            } else {
                // Une session est déjà ouverte, rediriger directement vers l'interface de vente
                response.sendRedirect("/caissier");
            }
        } else {
            response.sendRedirect("/"); // Fallback
        }
    }
}
