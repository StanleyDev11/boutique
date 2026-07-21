package com.example.boutique.controller;

import com.example.boutique.service.LicenseService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

    private final LicenseService licenseService;

    public AuthController(LicenseService licenseService) {
        this.licenseService = licenseService;
    }

    @GetMapping("/login")
    public String login() {
        // À la première ouverture de la page de connexion, on démarre l'essai
        // (enregistre install_id + install_date de façon persistante).
        licenseService.ensureInitialized();
        return "login";
    }

    @GetMapping("/")
    public String redirectToAppropriatePage() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            if (AuthorityUtils.authorityListToSet(authentication.getAuthorities()).contains("ROLE_ADMIN")) {
                return "redirect:/dashboard";
            } else if (AuthorityUtils.authorityListToSet(authentication.getAuthorities()).contains("ROLE_GESTIONNAIRE")) {
                return "redirect:/produits";
            }
        }
        return "redirect:/login";
    }

}