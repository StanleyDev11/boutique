package com.example.boutique.controller;

import com.example.boutique.model.Personnel;
import com.example.boutique.repository.PersonnelRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/personnel")
public class PersonnelController {

    private final PersonnelRepository personnelRepository;

    public PersonnelController(PersonnelRepository personnelRepository) {
        this.personnelRepository = personnelRepository;
    }

    @GetMapping
    public String listPersonnel(Model model,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Personnel> personnelPage = personnelRepository.findAll(pageable);
        model.addAttribute("personnelPage", personnelPage);
        return "personnel-list";
    }

    @GetMapping("/form")
    public String getFormFragment(@RequestParam(required = false) Long id, Model model) {
        Personnel personnel;
        String pageTitle;
        if (id != null) {
            personnel = personnelRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Employé non trouvé pour l'ID: " + id));
            pageTitle = "Modifier l'employé";
        } else {
            personnel = new Personnel();
            pageTitle = "Ajouter un employé";
        }
        model.addAttribute("personnel", personnel);
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("view", "fragment");
        return "personnel-form :: form-content";
    }

    @PostMapping("/save")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> savePersonnel(@ModelAttribute("personnel") Personnel personnel) {
        try {
            personnelRepository.save(personnel);
            return ResponseEntity.ok(Map.of("success", true, "message", "L'employé a été sauvegardé avec succès !"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Erreur lors de la sauvegarde de l'employé."));
        }
    }

    @PostMapping("/delete/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deletePersonnel(@PathVariable Long id) {
        try {
            personnelRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "L'employé a été supprimé avec succès !"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Erreur lors de la suppression de l'employé."));
        }
    }

    // --- Fallback methods ---
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
}
