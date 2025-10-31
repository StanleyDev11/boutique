package com.example.boutique.controller;

import com.example.boutique.model.SessionCaisse;
import com.example.boutique.model.Utilisateur;
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

    @GetMapping("/ouvrir")
    public String showOuvertureForm(Model model) {
        return "ouverture-caisse";
    }

    @PostMapping("/ouvrir")
    public String ouvrirCaisse(@RequestParam("montantInitial") BigDecimal montantInitial, RedirectAttributes redirectAttributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        Optional<Utilisateur> utilisateurOpt = utilisateurRepository.findByUsername(username);

        if (utilisateurOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Utilisateur non trouvé.");
            return "redirect:/login";
        }

        Utilisateur utilisateur = utilisateurOpt.get();

        Optional<SessionCaisse> openSession = sessionCaisseRepository.findFirstByUtilisateurAndDateFermetureIsNullOrderByDateOuvertureDesc(utilisateur);
        if (openSession.isPresent()) {
            return "redirect:/caissier";
        }

        if (montantInitial.compareTo(BigDecimal.ZERO) < 0) {
            redirectAttributes.addFlashAttribute("error", "Le montant initial ne peut pas être négatif.");
            return "redirect:/caisse/ouvrir";
        }

        SessionCaisse sessionCaisse = new SessionCaisse();
        sessionCaisse.setUtilisateur(utilisateur);
        sessionCaisse.setMontantInitial(montantInitial);

        sessionCaisseRepository.save(sessionCaisse);

        return "redirect:/caissier";
    }

    @GetMapping("/fermer")
    public String showFermetureForm(Model model, RedirectAttributes redirectAttributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Optional<Utilisateur> utilisateurOpt = utilisateurRepository.findByUsername(authentication.getName());

        if (utilisateurOpt.isEmpty()) {
            return "redirect:/login";
        }
        Utilisateur utilisateur = utilisateurOpt.get();

        Optional<SessionCaisse> sessionOpt = sessionCaisseRepository.findOpenSessionWithUser(utilisateur);
        if (sessionOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Aucune session de caisse n'est actuellement ouverte.");
            return "redirect:/caisse/ouvrir";
        }

        SessionCaisse session = sessionOpt.get();

        BigDecimal ventesCalculees = venteRepository.sumTotalForUserSince(utilisateur, session.getDateOuverture());
        if (ventesCalculees == null) {
            ventesCalculees = BigDecimal.ZERO;
        }

        BigDecimal initialAmount = session.getMontantInitial();
        System.out.println("Montant Initial from DB (explicitly added): " + initialAmount);
        model.addAttribute("session", session);
        model.addAttribute("ventesCalculees", ventesCalculees);
        model.addAttribute("montantInitialFromController", initialAmount);

        return "fermeture-caisse";
    }

    @PostMapping("/fermer")
    public String fermerCaisse(@RequestParam("montantFinal") BigDecimal montantFinal, HttpServletRequest request, HttpServletResponse response, RedirectAttributes redirectAttributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Optional<Utilisateur> utilisateurOpt = utilisateurRepository.findByUsername(authentication.getName());

        if (utilisateurOpt.isEmpty()) {
            return "redirect:/login";
        }
        Utilisateur utilisateur = utilisateurOpt.get();

        Optional<SessionCaisse> sessionOpt = sessionCaisseRepository.findOpenSessionWithUser(utilisateur);
        if (sessionOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Aucune session de caisse n'est actuellement ouverte.");
            return "redirect:/caisse/ouvrir";
        }

        SessionCaisse session = sessionOpt.get();

        BigDecimal ventesCalculees = venteRepository.sumTotalForUserSince(utilisateur, session.getDateOuverture());
        if (ventesCalculees == null) {
            ventesCalculees = BigDecimal.ZERO;
        }

        BigDecimal totalAttendu = session.getMontantInitial().add(ventesCalculees);
        BigDecimal ecart = montantFinal.subtract(totalAttendu);

        session.setMontantFinal(montantFinal);
        session.setVentesCalculees(ventesCalculees);
        session.setEcart(ecart);
        session.setDateFermeture(LocalDateTime.now());

        sessionCaisseRepository.save(session);

        // Logout user
        new SecurityContextLogoutHandler().logout(request, response, authentication);

        return "redirect:/login?logout";
    }

    @GetMapping("/historique")
    public String showSessionHistorique(Model model, RedirectAttributes redirectAttributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Optional<Utilisateur> utilisateurOpt = utilisateurRepository.findByUsername(authentication.getName());

        if (utilisateurOpt.isEmpty()) {
            return "redirect:/login";
        }
        Utilisateur utilisateur = utilisateurOpt.get();

        Optional<SessionCaisse> sessionOpt = sessionCaisseRepository.findOpenSessionWithUser(utilisateur);
        if (sessionOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Aucune session de caisse n'est actuellement ouverte.");
            return "redirect:/caisse/ouvrir";
        }

        SessionCaisse session = sessionOpt.get();
        List<com.example.boutique.model.Vente> ventes = venteRepository.findByUtilisateurAndDateVenteAfter(utilisateur, session.getDateOuverture(), org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "dateVente"));

        BigDecimal totalSalesAmount = ventes.stream()
                .map(com.example.boutique.model.Vente::getTotalFinal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int numberOfSales = ventes.size();

        model.addAttribute("session", session);
        model.addAttribute("ventes", ventes);
        model.addAttribute("totalSalesAmount", totalSalesAmount);
        model.addAttribute("numberOfSales", numberOfSales);

        return "session-historique";
    }
}
