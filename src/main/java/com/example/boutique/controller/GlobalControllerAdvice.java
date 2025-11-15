package com.example.boutique.controller;

import com.example.boutique.model.SessionCaisse;
import com.example.boutique.model.Utilisateur;
import com.example.boutique.repository.SessionCaisseRepository;
import com.example.boutique.repository.UtilisateurRepository;
import com.example.boutique.service.ParametreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

import java.util.Optional;

@ControllerAdvice
public class GlobalControllerAdvice {

    private static final Logger logger = LoggerFactory.getLogger(GlobalControllerAdvice.class);

    @Autowired
    private SessionCaisseRepository sessionCaisseRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private ParametreService parametreService;

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

    @ModelAttribute("delaiInactivite")
    public int delaiInactivite() {
        return parametreService.getDelaiInactivite();
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ModelAndView handleAllExceptions(Exception ex) {
        // Log the exception for debugging purposes
        logger.error("Une erreur inattendue est survenue: ", ex);

        // Return a user-friendly error page
        ModelAndView modelAndView = new ModelAndView("error"); // 'error' is the name of the view
        modelAndView.addObject("errorMessage", "Une erreur inattendue est survenue. Veuillez réessayer plus tard.");
        return modelAndView;
    }
}
