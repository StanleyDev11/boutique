package com.example.boutique.controller;

import com.example.boutique.service.ParametreService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/parametres")
public class ParametreController {

    private final ParametreService parametreService;

    public ParametreController(ParametreService parametreService) {
        this.parametreService = parametreService;
    }

    @GetMapping
    public String showParametresPage(Model model) {
        Map<String, String> parametres = parametreService.getAllParametres();
        // Ensure default values are present if not in the database
        parametres.putIfAbsent("seuil_stock_bas", String.valueOf(parametreService.getSeuilStockBas()));
        parametres.putIfAbsent("jours_avant_peremption", String.valueOf(parametreService.getJoursAvantPeremption()));
        parametres.putIfAbsent("delai_inactivite", String.valueOf(parametreService.getDelaiInactivite()));
        model.addAttribute("parametres", parametres);
        return "parametres";
    }

    @PostMapping("/sauvegarder")
    public String saveParametres(@RequestParam Map<String, String> parametres, RedirectAttributes redirectAttributes) {
        try {
            // Validate numeric fields
            Integer.parseInt(parametres.get("seuil_stock_bas"));
            Integer.parseInt(parametres.get("jours_avant_peremption"));
            Integer.parseInt(parametres.get("delai_inactivite"));

            parametreService.updateParametres(parametres);
            redirectAttributes.addFlashAttribute("successMessage", "Paramètres sauvegardés avec succès !");
        } catch (NumberFormatException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erreur : Une des valeurs saisies n'est pas un nombre valide. Les modifications n'ont pas été sauvegardées.");
        }
        return "redirect:/parametres";
    }
}
