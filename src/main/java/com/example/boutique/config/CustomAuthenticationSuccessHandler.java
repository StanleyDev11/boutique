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
            String username = authentication.getName();
            Optional<Utilisateur> userOpt = utilisateurRepository.findByUsername(username);

            if (userOpt.isPresent()) {
                Utilisateur utilisateur = userOpt.get();
                Optional<SessionCaisse> sessionOpt = sessionCaisseRepository.findFirstByUtilisateurAndDateFermetureIsNullOrderByDateOuvertureDesc(utilisateur);

                if (sessionOpt.isEmpty()) {
                    // No open session, redirect to open cash register page
                    response.sendRedirect("/caisse/ouvrir");
                } else {
                    // Session already open, proceed to cashier page
                    response.sendRedirect("/caissier");
                }
            } else {
                // Should not happen if authentication is successful
                response.sendRedirect("/login?error");
            }
        } else {
            response.sendRedirect("/"); // Fallback
        }
    }
}
