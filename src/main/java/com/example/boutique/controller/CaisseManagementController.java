package com.example.boutique.controller;

import com.example.boutique.model.Caisse;
import com.example.boutique.model.SessionCaisse;
import com.example.boutique.model.Vente;
import com.example.boutique.service.CaisseService;
import com.example.boutique.repository.SessionCaisseRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/gestion-caisses")
public class CaisseManagementController {

    private final CaisseService caisseService;
    private final SessionCaisseRepository sessionCaisseRepository;

    public CaisseManagementController(CaisseService caisseService, SessionCaisseRepository sessionCaisseRepository) {
        this.caisseService = caisseService;
        this.sessionCaisseRepository = sessionCaisseRepository;
    }

    @GetMapping
    public String listCaisses(Model model,
                              @RequestParam(required = false) String caisseKeyword,
                              @RequestParam(required = false) String openKeyword,
                              @RequestParam(required = false) String closedKeyword,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                              @RequestParam(defaultValue = "0") int openPage,
                              @RequestParam(defaultValue = "10") int openSize,
                              @RequestParam(defaultValue = "dateOuverture") String openSortField,
                              @RequestParam(defaultValue = "desc") String openSortDir,
                              @RequestParam(defaultValue = "0") int closedPage,
                              @RequestParam(defaultValue = "10") int closedSize,
                              @RequestParam(defaultValue = "dateFermeture") String closedSortField,
                              @RequestParam(defaultValue = "desc") String closedSortDir) {

        List<Caisse> caisses = caisseService.searchCaisses(caisseKeyword);
        model.addAttribute("caisses", caisses);
        model.addAttribute("caisseKeyword", caisseKeyword);

        // Open sessions pagination
        Sort openSort = openSortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(openSortField).ascending() : Sort.by(openSortField).descending();
        Pageable openPageable = PageRequest.of(openPage, openSize, openSort);
        Page<SessionCaisse> openSessionsPage = caisseService.getOpenSessions(openKeyword, openPageable);
        model.addAttribute("openSessionsPage", openSessionsPage);
        model.addAttribute("openKeyword", openKeyword);
        model.addAttribute("openSortField", openSortField);
        model.addAttribute("openSortDir", openSortDir);
        model.addAttribute("openReverseSortDir", openSortDir.equals("asc") ? "desc" : "asc");

        // Closed sessions pagination
        Sort closedSort = closedSortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(closedSortField).ascending() : Sort.by(closedSortField).descending();
        Pageable closedPageable = PageRequest.of(closedPage, closedSize, closedSort);
        Page<SessionCaisse> closedSessionsPage = caisseService.getClosedSessions(closedKeyword, startDate, endDate, closedPageable);
        model.addAttribute("closedSessionsPage", closedSessionsPage);
        model.addAttribute("closedKeyword", closedKeyword);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("closedSortField", closedSortField);
        model.addAttribute("closedSortDir", closedSortDir);
        model.addAttribute("closedReverseSortDir", closedSortDir.equals("asc") ? "desc" : "asc");

        return "gestion-caisses";
    }

    @GetMapping("/form")
    public String showCaisseForm(@RequestParam(required = false) Long id, Model model) {
        Caisse caisse = id != null ? caisseService.getCaisseById(id).orElse(new Caisse()) : new Caisse();
        model.addAttribute("caisse", caisse);
        return "fragments/caisse-form :: caisse-form";
    }

    @PostMapping("/save")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveCaisse(@ModelAttribute Caisse caisse) {
        try {
            Caisse savedCaisse = caisseService.createCaisse(caisse);
            String successMessage = "La caisse '" + savedCaisse.getNom() + "' a été enregistrée avec succès.";
            return ResponseEntity.ok(Map.of("success", true, "message", successMessage));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Une caisse avec ce nom existe déjà."));
        }
        catch (Exception e) {
            String errorMessage = "Erreur lors de l'enregistrement de la caisse : " + e.getMessage();
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", errorMessage));
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteCaisse(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            caisseService.deleteCaisse(id);
            redirectAttributes.addFlashAttribute("successMessage", "Caisse supprimée avec succès !");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/gestion-caisses?tab=caisses";
    }

    @PostMapping("/activate/{id}")
    public String activateCaisse(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        caisseService.activateCaisse(id);
        redirectAttributes.addFlashAttribute("successMessage", "Caisse activée avec succès !");
        return "redirect:/gestion-caisses?tab=caisses";
    }

    @PostMapping("/deactivate/{id}")
    public String deactivateCaisse(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            caisseService.deactivateCaisse(id);
            redirectAttributes.addFlashAttribute("successMessage", "Caisse désactivée avec succès !");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/gestion-caisses?tab=caisses";
    }

    @GetMapping("/session-details/{id}")
    public String sessionDetails(@PathVariable Long id, Model model) {
        SessionCaisse sessionCaisse = sessionCaisseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Session de caisse non trouvée avec l'id : " + id));
        List<Vente> ventes = caisseService.getVentesBySessionCaisse(sessionCaisse);

        double totalVentes = ventes.stream().map(Vente::getTotalFinal).mapToDouble(java.math.BigDecimal::doubleValue).sum();
        int nombreVentes = ventes.size();
        double venteMoyenne = (nombreVentes > 0) ? totalVentes / nombreVentes : 0.0;

        model.addAttribute("sessionCaisse", sessionCaisse);
        model.addAttribute("ventes", ventes);
        model.addAttribute("totalVentes", totalVentes);
        model.addAttribute("nombreVentes", nombreVentes);
        model.addAttribute("venteMoyenne", venteMoyenne);

        return "session-details";
    }
}