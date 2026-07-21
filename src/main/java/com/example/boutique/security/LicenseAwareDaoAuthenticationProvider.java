package com.example.boutique.security;

import com.example.boutique.service.LicenseService;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Provider d'authentification pour les comptes de la BASE DE DONNÉES.
 *
 * Après une authentification réussie (username/password valides), applique les
 * règles de licence/essai : pendant l'essai seul le compte démo passe,
 * admin.boutika et les autres comptes attendent l'activation, et à l'expiration
 * le compte démo est bloqué (redirection vers la page d'expiration).
 *
 * Le super admin ne passe jamais par ce provider (voir
 * SuperAdminAuthenticationProvider), il n'est donc jamais soumis à ces règles.
 */
@Component
public class LicenseAwareDaoAuthenticationProvider extends DaoAuthenticationProvider {

    private final LicenseService licenseService;

    public LicenseAwareDaoAuthenticationProvider(UserDetailsService userDetailsService,
                                                 PasswordEncoder passwordEncoder,
                                                 LicenseService licenseService) {
        this.licenseService = licenseService;
        setUserDetailsService(userDetailsService);
        setPasswordEncoder(passwordEncoder);
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Authentication result = super.authenticate(authentication);
        // Vérifie l'état de licence/essai selon le compte authentifié.
        licenseService.enforceLoginRules(result);
        return result;
    }
}
