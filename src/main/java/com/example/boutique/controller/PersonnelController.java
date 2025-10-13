package com.example.boutique.controller;

import com.example.boutique.model.Personnel;
import com.example.boutique.repository.PersonnelRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/personnel")
public class PersonnelController {

    private final PersonnelRepository personnelRepository;

    public PersonnelController(PersonnelRepository personnelRepository) {
        this.personnelRepository = personnelRepository;
    }

    @GetMapping
    public String listPersonnel(Model model) {
        model.addAttribute("personnelList", personnelRepository.findAll());
        return "personnel-list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("personnel", new Personnel());
        model.addAttribute("pageTitle", "Ajouter un employé");
        return "personnel-form";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Personnel personnel = personnelRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Employé non trouvé pour l'ID: " + id));
            model.addAttribute("personnel", personnel);
            model.addAttribute("pageTitle", "Modifier l'employé");
            return "personnel-form";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/personnel";
        }
    }

    @PostMapping("/save")
    public String savePersonnel(@ModelAttribute("personnel") Personnel personnel, RedirectAttributes redirectAttributes) {
        try {
            personnelRepository.save(personnel);
            redirectAttributes.addFlashAttribute("successMessage", "L'employé a été sauvegardé avec succès !");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erreur lors de la sauvegarde de l'employé.");
        }
        return "redirect:/personnel";
    }

    @PostMapping("/delete/{id}")
    public String deletePersonnel(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            personnelRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "L'employé a été supprimé avec succès !");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erreur lors de la suppression de l'employé.");
        }
        return "redirect:/personnel";
    }
}
