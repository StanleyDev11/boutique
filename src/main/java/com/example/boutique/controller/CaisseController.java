package com.example.boutique.controller;

import com.example.boutique.model.Caisse;
import com.example.boutique.model.SessionCaisse;
import com.example.boutique.model.Utilisateur;
import com.example.boutique.repository.CaisseRepository;
import com.example.boutique.repository.SessionCaisseRepository;
import com.example.boutique.repository.UtilisateurRepository;
import com.example.boutique.repository.VenteRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/caisse")
public class CaisseController {

    @Autowired
    private SessionCaisseRepository sessionCaisseRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private VenteRepository venteRepository;

    @Autowired
    private CaisseRepository caisseRepository;

    @GetMapping("/ouvrir")
    public String showOuvertureForm(Model model) {
        return "ouverture-caisse";
    }

    @PostMapping("/ouvrir")
    public String ouvrirCaisse(@RequestParam("montantInitial") BigDecimal montantInitial,
                               @RequestParam("codeCaissier") String codeCaissier,
                               RedirectAttributes redirectAttributes, Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        Optional<Utilisateur> utilisateurOpt = utilisateurRepository.findByUsername(username);

        if (utilisateurOpt.isEmpty()) {
            model.addAttribute("error", "Utilisateur non trouvé.");
            return "ouverture-caisse";
        }

        Utilisateur utilisateur = utilisateurOpt.get();

        // Vérifier si le code fourni correspond au code de l'utilisateur
        if (utilisateur.getCode() == null || !utilisateur.getCode().equals(codeCaissier)) {
            model.addAttribute("error", "Le code personnel est incorrect.");
            return "ouverture-caisse";
        }

        // Vérifier si une session est déjà ouverte dans tout le système
        Optional<SessionCaisse> anyOpenSession = sessionCaisseRepository.findFirstByDateFermetureIsNull();
        if (anyOpenSession.isPresent()) {
            model.addAttribute("error", "Une session de caisse est déjà ouverte. Veuillez la fermer avant d'en ouvrir une nouvelle.");
            return "ouverture-caisse";
        }

        if (montantInitial.compareTo(BigDecimal.ZERO) < 0) {
            model.addAttribute("error", "Le montant initial ne peut pas être négatif.");
            return "ouverture-caisse";
        }

        SessionCaisse sessionCaisse = new SessionCaisse();
        sessionCaisse.setUtilisateur(utilisateur);
        sessionCaisse.setMontantInitial(montantInitial);

        sessionCaisseRepository.save(sessionCaisse);

        return "redirect:/caissier";
    }

    @GetMapping("/fermer")
    public String showFermetureForm(Model model, RedirectAttributes redirectAttributes) {
        Optional<SessionCaisse> sessionOpt = sessionCaisseRepository.findFirstByDateFermetureIsNull();
        if (sessionOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Aucune session de caisse n'est actuellement ouverte.");
            return "redirect:/caisse/ouvrir";
        }

        SessionCaisse session = sessionOpt.get();

        BigDecimal ventesCalculees = venteRepository.sumTotalForSession(session);
        if (ventesCalculees == null) {
            ventesCalculees = BigDecimal.ZERO;
        }

        model.addAttribute("session", session);
        model.addAttribute("ventesCalculees", ventesCalculees);
        model.addAttribute("montantInitialFromController", session.getMontantInitial());

        return "fermeture-caisse";
    }

    @PostMapping("/fermer")
    public String fermerCaisse(@RequestParam("montantFinal") BigDecimal montantFinal,
                               @RequestParam("codeCaissier") String codeCaissier,
                               HttpServletRequest request,
                               HttpServletResponse response,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Optional<Utilisateur> utilisateurOpt = utilisateurRepository.findByUsername(authentication.getName());

        if (utilisateurOpt.isEmpty()) {
            return "redirect:/login";
        }
        Utilisateur utilisateur = utilisateurOpt.get();

        Optional<SessionCaisse> sessionOpt = sessionCaisseRepository.findFirstByDateFermetureIsNull();
        if (sessionOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Aucune session de caisse n'est actuellement ouverte.");
            return "redirect:/caisse/ouvrir";
        }
        SessionCaisse session = sessionOpt.get();

        // Valider le code du caissier qui ferme la caisse
        if (utilisateur.getCode() == null || !utilisateur.getCode().equals(codeCaissier)) {
            model.addAttribute("error", "Le code personnel est incorrect.");
            model.addAttribute("montantFinal", montantFinal);

            // Re-populate model attributes for the view
            BigDecimal ventesCalculees = venteRepository.sumTotalForSession(session);
            if (ventesCalculees == null) {
                ventesCalculees = BigDecimal.ZERO;
            }
            model.addAttribute("session", session);
            model.addAttribute("ventesCalculees", ventesCalculees);
            model.addAttribute("montantInitialFromController", session.getMontantInitial());

            return "fermeture-caisse";
        }

        BigDecimal ventesCalculees = venteRepository.sumTotalForSession(session);
        if (ventesCalculees == null) {
            ventesCalculees = BigDecimal.ZERO;
        }

        BigDecimal totalAttendu = session.getMontantInitial().add(ventesCalculees);
        BigDecimal ecart = montantFinal.subtract(totalAttendu);

        session.setMontantFinal(montantFinal);
        session.setVentesCalculees(ventesCalculees);
        session.setEcart(ecart);
        session.setDateFermeture(LocalDateTime.now());
        session.setFermeParUtilisateur(utilisateur);

        sessionCaisseRepository.save(session);

        // Logout user
        new SecurityContextLogoutHandler().logout(request, response, authentication);

        return "redirect:/login?logout";
    }

    @GetMapping("/historique")
    public String showSessionHistorique(Model model, RedirectAttributes redirectAttributes) {
        // Récupérer la session globale ouverte, peu importe qui l'a ouverte
        Optional<SessionCaisse> sessionOpt = sessionCaisseRepository.findFirstByDateFermetureIsNull();
        if (sessionOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Aucune session de caisse n'est actuellement ouverte.");
            return "redirect:/caisse/ouvrir";
        }

        SessionCaisse session = sessionOpt.get();
        // Note: The original logic fetched sales for a specific user.
        // The new logic should probably fetch ALL sales for the session.
        List<com.example.boutique.model.Vente> ventes = venteRepository.findBySessionCaisse(session, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "dateVente"));

        BigDecimal totalSalesAmount = ventes.stream()
                .filter(v -> v.getStatus() == com.example.boutique.enums.VenteStatus.COMPLETED && !"credit".equals(v.getTypeVente()))
                .map(com.example.boutique.model.Vente::getTotalFinal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long numberOfSales = ventes.stream()
                .filter(v -> v.getStatus() == com.example.boutique.enums.VenteStatus.COMPLETED && !"credit".equals(v.getTypeVente()))
                .count();

        long numberOfCreditSales = ventes.stream()
                .filter(v -> "credit".equals(v.getTypeVente()))
                .count();

        model.addAttribute("session", session);
        model.addAttribute("ventes", ventes);
        model.addAttribute("totalSalesAmount", totalSalesAmount);
        model.addAttribute("numberOfSales", numberOfSales);
        model.addAttribute("numberOfCreditSales", numberOfCreditSales);

        return "session-historique";
    }
}
