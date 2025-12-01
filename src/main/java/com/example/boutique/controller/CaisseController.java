package com.example.boutique.controller;

import com.example.boutique.enums.MoyenPaiement;
import com.example.boutique.model.*;
import com.example.boutique.repository.CaisseRepository;
import com.example.boutique.repository.SessionCaisseRepository;
import com.example.boutique.repository.UtilisateurRepository;
import com.example.boutique.repository.VenteRepository;
import com.example.boutique.service.PdfGenerationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Sort;
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

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/caisse")
public class CaisseController {

    private static final Logger logger = LoggerFactory.getLogger(CaisseController.class);

    private final SessionCaisseRepository sessionCaisseRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final VenteRepository venteRepository;
    private final CaisseRepository caisseRepository;
    private final PdfGenerationService pdfGenerationService;

    @Autowired
    public CaisseController(SessionCaisseRepository sessionCaisseRepository, UtilisateurRepository utilisateurRepository, VenteRepository venteRepository, CaisseRepository caisseRepository, PdfGenerationService pdfGenerationService) {
        this.sessionCaisseRepository = sessionCaisseRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.venteRepository = venteRepository;
        this.caisseRepository = caisseRepository;
        this.pdfGenerationService = pdfGenerationService;
    }

    @GetMapping("/ouvrir")
    public String showOuvertureForm(Model model) {
        logger.info("Accès au formulaire d'ouverture de caisse.");
        Optional<Caisse> caisseOpt = caisseRepository.findFirstByActive(true);
        if (caisseOpt.isEmpty()) {
            logger.warn("Aucune caisse active trouvée. Redirection vers la page d'erreur.");
            model.addAttribute("error", "Aucune caisse n'est active. Veuillez activer une caisse avant d'ouvrir une session.");
            return "error"; // Or a more specific error page
        }
        logger.info("Caisse active trouvée, affichage du formulaire.");
        return "ouverture-caisse";
    }

    @PostMapping("/ouvrir")
    public String ouvrirCaisse(@RequestParam("montantInitial") BigDecimal montantInitial,
                               @RequestParam("codeCaissier") String codeCaissier,
                               RedirectAttributes redirectAttributes, Model model) {
        logger.info("Tentative d'ouverture de caisse avec le code caissier.");
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            logger.info("Utilisateur authentifié: {}", username);

            Optional<Utilisateur> utilisateurOpt = utilisateurRepository.findByUsername(username);

            if (utilisateurOpt.isEmpty()) {
                logger.warn("Utilisateur non trouvé pour le nom d'utilisateur: {}", username);
                model.addAttribute("error", "Utilisateur non trouvé.");
                return "ouverture-caisse";
            }

            Utilisateur utilisateur = utilisateurOpt.get();
            logger.info("Utilisateur trouvé: {}", utilisateur.getUsername());

            if (utilisateur.getCode() == null || !utilisateur.getCode().equals(codeCaissier)) {
                logger.warn("Code personnel incorrect pour l'utilisateur: {}", username);
                model.addAttribute("error", "Le code personnel est incorrect.");
                return "ouverture-caisse";
            }

            Optional<SessionCaisse> anyOpenSession = sessionCaisseRepository.findFirstByDateFermetureIsNull();
            if (anyOpenSession.isPresent()) {
                logger.warn("Tentative d'ouverture d'une nouvelle session alors qu'une session est déjà ouverte.");
                model.addAttribute("error", "Une session de caisse est déjà ouverte. Veuillez la fermer avant d'en ouvrir une nouvelle.");
                return "ouverture-caisse";
            }

            Optional<Caisse> caisseOpt = caisseRepository.findFirstByActive(true);
            if (caisseOpt.isEmpty()) {
                logger.warn("Aucune caisse active trouvée pour l'ouverture de la session.");
                model.addAttribute("error", "Aucune caisse active n'est disponible pour démarrer une session.");
                return "ouverture-caisse";
            }

            if (montantInitial.compareTo(BigDecimal.ZERO) < 0) {
                logger.warn("Montant initial négatif fourni: {}", montantInitial);
                model.addAttribute("error", "Le montant initial ne peut pas être négatif.");
                return "ouverture-caisse";
            }

            SessionCaisse sessionCaisse = new SessionCaisse();
            sessionCaisse.setUtilisateur(utilisateur);
            sessionCaisse.setMontantInitial(montantInitial);
            sessionCaisse.setCaisse(caisseOpt.get()); // Link the active caisse

            sessionCaisseRepository.save(sessionCaisse);
            logger.info("Nouvelle session de caisse créée avec succès pour l'utilisateur: {}", username);

            return "redirect:/caissier";
        } catch (Exception e) {
            logger.error("Erreur inattendue lors de l'ouverture de la caisse.", e);
            model.addAttribute("error", "Une erreur technique est survenue. Impossible d'ouvrir la session.");
            return "ouverture-caisse";
        }
    }

    @GetMapping("/fermer")
    public String showFermetureForm(Model model, RedirectAttributes redirectAttributes) {
        Optional<SessionCaisse> sessionOpt = sessionCaisseRepository.findFirstByDateFermetureIsNull();
        if (sessionOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Aucune session de caisse n'est actuellement ouverte.");
            return "redirect:/dashboard";
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
    public Object fermerCaisse(@RequestParam("montantFinal") BigDecimal montantFinal,
                               @RequestParam("codeCaissier") String codeCaissier,
                               HttpServletRequest request,
                               HttpServletResponse response,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        Optional<SessionCaisse> sessionOpt = sessionCaisseRepository.findFirstByDateFermetureIsNull();
        if (sessionOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Aucune session de caisse n'est actuellement ouverte.");
            return "redirect:/dashboard";
        }
        SessionCaisse session = sessionOpt.get();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        try {
            Optional<Utilisateur> utilisateurOpt = utilisateurRepository.findByUsername(authentication.getName());

            if (utilisateurOpt.isEmpty()) {
                return "redirect:/login";
            }
            Utilisateur utilisateur = utilisateurOpt.get();

            if (utilisateur.getCode() == null || !utilisateur.getCode().equals(codeCaissier)) {
                model.addAttribute("error", "Le code personnel est incorrect.");
                BigDecimal ventesCalculees = venteRepository.sumTotalForSession(session);
                model.addAttribute("session", session);
                model.addAttribute("ventesCalculees", ventesCalculees != null ? ventesCalculees : BigDecimal.ZERO);
                model.addAttribute("montantInitialFromController", session.getMontantInitial());
                return "fermeture-caisse";
            }

            List<Vente> ventes = venteRepository.findBySessionCaisse(session, Sort.by("dateVente"));
            BigDecimal totalVentesEspeces = calculateTotalByPaymentMethod(ventes, MoyenPaiement.ESPECES);

            BigDecimal totalAttendu = session.getMontantInitial().add(totalVentesEspeces);
            BigDecimal ecart = montantFinal.subtract(totalAttendu);

            session.setMontantFinal(montantFinal);
            session.setVentesCalculees(totalVentesEspeces);
            session.setEcart(ecart);
            session.setDateFermeture(LocalDateTime.now());
            session.setFermeParUtilisateur(utilisateur);

            SessionCaisse savedSession = sessionCaisseRepository.save(session);

            // --- PDF Generation ---
            BigDecimal totalVentesCarte = calculateTotalByPaymentMethod(ventes, MoyenPaiement.CARTE);
            BigDecimal totalVentesMobile = calculateTotalByPaymentMethod(ventes, MoyenPaiement.MOBILE);
            BigDecimal totalVentesCredit = calculateTotalByPaymentMethod(ventes, MoyenPaiement.CREDIT);
            BigDecimal grandTotalVentes = totalVentesEspeces.add(totalVentesCarte).add(totalVentesMobile).add(totalVentesCredit);
            BigDecimal totalVentesAutres = totalVentesCarte.add(totalVentesMobile).add(totalVentesCredit);

            Map<String, Object> data = new HashMap<>();
            data.put("session", savedSession);
            data.put("totalVentesEspeces", totalVentesEspeces);
            data.put("totalVentesCarte", totalVentesCarte);
            data.put("totalVentesMobile", totalVentesMobile);
            data.put("totalVentesCredit", totalVentesCredit);
            data.put("totalVentesAutres", totalVentesAutres);
            data.put("grandTotalVentes", grandTotalVentes);
            data.put("totalTheorique", totalAttendu);
            data.put("nombreVentes", ventes.size());
            data.put("now", LocalDateTime.now());

            byte[] pdfBytes = pdfGenerationService.generatePdfFromHtml("recu-fermeture-caisse", data);

            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            String filename = "rapport-caisse-" + savedSession.getId() + ".pdf";
            headers.add("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (IOException e) {
            logger.error("Erreur lors de la génération du PDF pour la session ID: {}", session.getId(), e);
            redirectAttributes.addFlashAttribute("error", "Erreur PDF. Session fermée, mais rapport non généré.");
            return "redirect:/gestion-caisses/session-details/" + session.getId();
        } catch (Exception e) {
            logger.error("Erreur inattendue lors de la fermeture de la caisse.", e);
            BigDecimal ventesCalculees = venteRepository.sumTotalForSession(session);
            model.addAttribute("session", session);
            model.addAttribute("ventesCalculees", ventesCalculees != null ? ventesCalculees : BigDecimal.ZERO);
            model.addAttribute("montantInitialFromController", session.getMontantInitial());
            model.addAttribute("error", "Erreur technique. Impossible de fermer la session.");
            return "fermeture-caisse";
        }
    }

    private BigDecimal calculateTotalByPaymentMethod(List<Vente> ventes, MoyenPaiement moyen) {
        return ventes.stream()
            .filter(v -> v.getStatus() != com.example.boutique.enums.VenteStatus.CANCELLED && v.getMoyenPaiement() == moyen)
            .map(Vente::getTotalFinal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @GetMapping("/historique")
    public String showSessionHistorique(Model model, RedirectAttributes redirectAttributes) {
        Optional<SessionCaisse> sessionOpt = sessionCaisseRepository.findFirstByDateFermetureIsNull();
        if (sessionOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Aucune session de caisse n'est actuellement ouverte.");
            return "redirect:/dashboard";
        }

        SessionCaisse session = sessionOpt.get();
        List<Vente> ventes = venteRepository.findBySessionCaisse(session, Sort.by(Sort.Direction.DESC, "dateVente"));

        BigDecimal totalSalesAmount = ventes.stream()
                .filter(v -> v.getStatus() == com.example.boutique.enums.VenteStatus.COMPLETED && !"credit".equals(v.getTypeVente()))
                .map(Vente::getTotalFinal)
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
