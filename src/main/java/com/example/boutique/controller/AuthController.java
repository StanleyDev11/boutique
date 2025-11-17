package com.example.boutique.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class AuthController {

    @GetMapping("/login")
    public String login() {
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

    @GetMapping("/api/keep-alive")
    @ResponseBody
    public ResponseEntity<Void> keepAlive() {
        return ResponseEntity.ok().build();
    }
}