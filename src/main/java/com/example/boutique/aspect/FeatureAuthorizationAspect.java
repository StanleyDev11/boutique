package com.example.boutique.aspect;

import com.example.boutique.exception.FeatureUnavailableException;
import com.example.boutique.model.Feature;
import com.example.boutique.model.Licence;
import com.example.boutique.model.LicenceStatus;
import com.example.boutique.model.Utilisateur;
import com.example.boutique.repository.LicenceRepository;
import com.example.boutique.repository.UtilisateurRepository;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.JoinPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.aspectj.lang.reflect.MethodSignature;


@Aspect
@Component
public class FeatureAuthorizationAspect {

    @Autowired
    private LicenceRepository licenceRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Before("@annotation(com.example.boutique.aspect.RequiresFeature)")
    public void checkFeatureAuthorization(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        RequiresFeature requiresFeature = signature.getMethod().getAnnotation(RequiresFeature.class);
        Feature requiredFeature = requiresFeature.value();

        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof UserDetails) {
            String username = ((UserDetails) principal).getUsername();
            Utilisateur utilisateur = utilisateurRepository.findByUsername(username)
                    .orElseThrow(() -> new FeatureUnavailableException("Utilisateur non trouvé."));

            Licence licence = licenceRepository.findByUtilisateur(utilisateur)
                    .orElseThrow(() -> new FeatureUnavailableException("Aucune licence trouvée pour cet utilisateur."));

            if (licence.getStatut() != LicenceStatus.ACTIVE) {
                throw new FeatureUnavailableException("Votre licence n'est pas active.");
            }

            if (licence.getPlan() == null || !licence.getPlan().getFeatures().contains(requiredFeature)) {
                throw new FeatureUnavailableException("Votre abonnement actuel ne vous donne pas accès à cette fonctionnalité.");
            }
        } else {
             throw new FeatureUnavailableException("Impossible de vérifier l'authentification de l'utilisateur.");
        }
    }
}
