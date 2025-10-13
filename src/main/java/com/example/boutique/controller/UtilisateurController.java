package com.example.boutique.controller;

import com.example.boutique.model.Utilisateur;
import com.example.boutique.repository.UtilisateurRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

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
    public String listUtilisateurs(Model model) {
        model.addAttribute("utilisateurs", utilisateurRepository.findAll());
        return "utilisateur-list";
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
    public String saveUtilisateur(@ModelAttribute("utilisateur") Utilisateur utilisateur, RedirectAttributes redirectAttributes) {
        try {
            // Si c'est une modification et que le mot de passe est vide, on garde l'ancien
            if (utilisateur.getId() != null && (utilisateur.getPassword() == null || utilisateur.getPassword().isEmpty())) {
                Utilisateur existingUser = utilisateurRepository.findById(utilisateur.getId()).orElseThrow();
                utilisateur.setPassword(existingUser.getPassword());
            } else {
                // Sinon, on chiffre le nouveau mot de passe
                utilisateur.setPassword(passwordEncoder.encode(utilisateur.getPassword()));
            }
            utilisateurRepository.save(utilisateur);
            redirectAttributes.addFlashAttribute("successMessage", "L'utilisateur a été sauvegardé avec succès !");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erreur lors de la sauvegarde de l'utilisateur.");
        }
        return "redirect:/utilisateurs";
    }

    @PostMapping("/delete/{id}")
    public String deleteUtilisateur(@PathVariable Long id, RedirectAttributes redirectAttributes, Principal principal) {
        try {
            Utilisateur userToDelete = utilisateurRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));
            // Empêcher un admin de supprimer son propre compte
            if (principal.getName().equals(userToDelete.getUsername())) {
                redirectAttributes.addFlashAttribute("errorMessage", "Vous ne pouvez pas supprimer votre propre compte.");
                return "redirect:/utilisateurs";
            }
            utilisateurRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "L'utilisateur a été supprimé avec succès !");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Erreur lors de la suppression de l'utilisateur.");
        }
        return "redirect:/utilisateurs";
    }
}
