package com.example.boutique.controller;

import com.example.boutique.model.Caisse;
import com.example.boutique.model.Utilisateur;
import com.example.boutique.service.CaisseService;
import com.example.boutique.repository.UtilisateurRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/gestion-caisses")
public class CaisseManagementController {

    private final CaisseService caisseService;
    private final UtilisateurRepository utilisateurRepository;

    public CaisseManagementController(CaisseService caisseService, UtilisateurRepository utilisateurRepository) {
        this.caisseService = caisseService;
        this.utilisateurRepository = utilisateurRepository;
    }

    @GetMapping
    public String listCaisses(Model model) {
        List<Caisse> caisses = caisseService.getAllCaisses();
        model.addAttribute("caisses", caisses);
        return "gestion-caisses";
    }

    @GetMapping("/form")
    public String showCaisseForm(@RequestParam(required = false) Long id, Model model) {
        Caisse caisse = id != null ? caisseService.getCaisseById(id).orElse(new Caisse()) : new Caisse();
        List<Utilisateur> caissiers = utilisateurRepository.findByRolesContaining("ROLE_CAISSIER");
        model.addAttribute("caisse", caisse);
        model.addAttribute("caissiers", caissiers);
        return "fragments/caisse-form :: caisse-form";
    }

    @PostMapping("/save")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveCaisse(@ModelAttribute Caisse caisse) {
        try {
            if (caisse.getUtilisateur() != null && caisse.getUtilisateur().getId() == null) {
                caisse.setUtilisateur(null);
            }
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
            redirectAttributes.addFlashAttribute("errorMessage", "Erreur lors de la suppression de la caisse.");
        }
        return "redirect:/gestion-caisses";
    }

    @PostMapping("/activate/{id}")
    public String activateCaisse(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        caisseService.activateCaisse(id);
        redirectAttributes.addFlashAttribute("successMessage", "Caisse activée avec succès !");
        return "redirect:/gestion-caisses";
    }

    @PostMapping("/deactivate/{id}")
    public String deactivateCaisse(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        caisseService.deactivateCaisse(id);
        redirectAttributes.addFlashAttribute("successMessage", "Caisse désactivée avec succès !");
        return "redirect:/gestion-caisses";
    }

    @PostMapping("/assigner")
    public String assignerCaissier(@RequestParam Long caisseId, @RequestParam(required=false) Long utilisateurId, RedirectAttributes redirectAttributes) {
        if (utilisateurId == null) {
            Caisse caisse = caisseService.getCaisseById(caisseId).orElseThrow(() -> new RuntimeException("Caisse non trouvée"));
            caisse.setUtilisateur(null);
            caisseService.createCaisse(caisse);
        } else {
            caisseService.assignerCaissier(caisseId, utilisateurId);
        }
        redirectAttributes.addFlashAttribute("successMessage", "Caissier assigné avec succès !");
        return "redirect:/gestion-caisses";
    }
}
