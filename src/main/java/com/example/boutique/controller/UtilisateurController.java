package com.example.boutique.controller;

import com.example.boutique.model.Utilisateur;
import com.example.boutique.repository.UtilisateurRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/utilisateurs")
public class UtilisateurController {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    public UtilisateurController(UtilisateurRepository utilisateurRepository, PasswordEncoder passwordEncoder) {
        this.utilisateurRepository = utilisateurRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public String listUtilisateurs(Model model,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Utilisateur> utilisateurPage = utilisateurRepository.findAll(pageable);
        model.addAttribute("utilisateursPage", utilisateurPage);
        return "utilisateur-list";
    }

    @GetMapping("/form")
    public String getFormFragment(@RequestParam(required = false) Long id, Model model) {
        Utilisateur utilisateur;
        String pageTitle;
        if (id != null) {
            utilisateur = utilisateurRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé pour l'ID: " + id));
            utilisateur.setPassword(""); // Ne jamais envoyer le hash au formulaire
            pageTitle = "Modifier l'utilisateur";
        } else {
            utilisateur = new Utilisateur();
            pageTitle = "Ajouter un nouvel utilisateur";
        }
        model.addAttribute("utilisateur", utilisateur);
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("view", "fragment"); // Pour le rendu conditionnel dans le template
        model.addAttribute("allRoles", List.of("ROLE_ADMIN", "ROLE_GESTIONNAIRE"));
        return "utilisateur-form :: form-content";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("utilisateur", new Utilisateur());
        model.addAttribute("pageTitle", "Ajouter un utilisateur");
        return "utilisateur-form";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Utilisateur utilisateur = utilisateurRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé pour l'ID: " + id));
            utilisateur.setPassword(""); // Ne jamais envoyer le hash au formulaire
            model.addAttribute("utilisateur", utilisateur);
            model.addAttribute("pageTitle", "Modifier l'utilisateur");
            return "utilisateur-form";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/utilisateurs";
        }
    }

    @PostMapping("/save")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveUtilisateur(@ModelAttribute("utilisateur") Utilisateur utilisateur, @RequestParam(name = "roles", required = false) List<String> roles) {
        try {
            // Si c'est une modification et que le mot de passe est vide, on garde l'ancien
            if (utilisateur.getId() != null && (utilisateur.getPassword() == null || utilisateur.getPassword().isEmpty())) {
                Utilisateur existingUser = utilisateurRepository.findById(utilisateur.getId()).orElseThrow();
                utilisateur.setPassword(existingUser.getPassword());
            } else {
                // Sinon, on chiffre le nouveau mot de passe
                utilisateur.setPassword(passwordEncoder.encode(utilisateur.getPassword()));
            }
            if (roles != null) {
                utilisateur.setRoles(String.join(",", roles));
            }
            utilisateurRepository.save(utilisateur);
            return ResponseEntity.ok(Map.of("success", true, "message", "L'utilisateur a été sauvegardé avec succès !"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Erreur lors de la sauvegarde de l'utilisateur."));
        }
    }

    @PostMapping("/delete/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteUtilisateur(@PathVariable Long id, Principal principal) {
        try {
            Utilisateur userToDelete = utilisateurRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));
            // Empêcher un admin de supprimer son propre compte
            if (principal.getName().equals(userToDelete.getUsername())) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Vous ne pouvez pas supprimer votre propre compte."));
            }
            utilisateurRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "L'utilisateur a été supprimé avec succès !"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Erreur lors de la suppression de l'utilisateur."));
        }
    }
}
