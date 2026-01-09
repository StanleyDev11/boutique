package com.example.boutique.multitenancy;

import com.example.boutique.model.Utilisateur;
import com.example.boutique.repository.UtilisateurRepository;
import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class TenantFilter extends OncePerRequestFilter {

    private final UtilisateurRepository utilisateurRepository;
    private final EntityManagerFactory entityManagerFactory;

    public TenantFilter(UtilisateurRepository utilisateurRepository, EntityManagerFactory entityManagerFactory) {
        this.utilisateurRepository = utilisateurRepository;
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String clientId = null;

        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof UserDetails) {
            String username = ((UserDetails) authentication.getPrincipal()).getUsername();
            Utilisateur utilisateur = utilisateurRepository.findByUsername(username).orElse(null);

            if (utilisateur != null && utilisateur.getOrganization() != null) {
                clientId = utilisateur.getOrganization().getClientId();
                TenantContext.setCurrentTenant(clientId);
            }
        }

        Session session = entityManagerFactory.unwrap(Session.class);
        if (clientId != null) {
            Filter filter = session.enableFilter("tenantFilter");
            filter.setParameter("clientId", clientId);
        } else {
            // For requests without an authenticated user or without a clientId,
            // we might want to disable the filter or set a 'default' client ID
            // For now, let's assume no filter is applied if no client ID is present.
            // Or, for 'anonymous' users accessing public resources, we can set a specific "public" client ID.
            // For the purpose of data isolation, it's safer to not enable the filter
            // unless a specific clientId is present.
            session.disableFilter("tenantFilter");
        }


        try {
            filterChain.doFilter(request, response);
        } finally {
            if (clientId != null) {
                session.disableFilter("tenantFilter");
            }
            TenantContext.clear();
        }
    }
}
