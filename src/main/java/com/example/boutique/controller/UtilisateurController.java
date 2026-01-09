package com.example.boutique.controller;

import com.example.boutique.model.Organization;
import com.example.boutique.model.Utilisateur;
import com.example.boutique.repository.OrganizationRepository;
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
import java.util.Optional;

@Controller
@RequestMapping("/utilisateurs")
public class UtilisateurController {

    private final UtilisateurRepository utilisateurRepository;
    private final OrganizationRepository organizationRepository; // Added
    private final PasswordEncoder passwordEncoder;

    public UtilisateurController(UtilisateurRepository utilisateurRepository, OrganizationRepository organizationRepository, PasswordEncoder passwordEncoder) {
        this.utilisateurRepository = utilisateurRepository;
        this.organizationRepository = organizationRepository; // Added
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
        model.addAttribute("allRoles", List.of("ROLE_ADMIN", "ROLE_GESTIONNAIRE", "ROLE_CAISSIER", "ROLE_SUPER_ADMIN")); // Added ROLE_SUPER_ADMIN
        model.addAttribute("organizations", organizationRepository.findAll()); // Added
        return "utilisateur-form :: form-content";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("utilisateur", new Utilisateur());
        model.addAttribute("pageTitle", "Ajouter un utilisateur");
        model.addAttribute("allRoles", List.of("ROLE_ADMIN", "ROLE_GESTIONNAIRE", "ROLE_CAISSIER", "ROLE_SUPER_ADMIN")); // Added ROLE_SUPER_ADMIN
        model.addAttribute("organizations", organizationRepository.findAll()); // Added
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
            model.addAttribute("allRoles", List.of("ROLE_ADMIN", "ROLE_GESTIONNAIRE", "ROLE_CAISSIER", "ROLE_SUPER_ADMIN")); // Added ROLE_SUPER_ADMIN
            model.addAttribute("organizations", organizationRepository.findAll()); // Added
            return "utilisateur-form";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/utilisateurs";
        }
    }

    @PostMapping("/save")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> saveUtilisateur(@ModelAttribute("utilisateur") Utilisateur utilisateur,
                                                               @RequestParam(name = "roles", required = false) List<String> roles,
                                                               @RequestParam(name = "organizationId", required = false) Long organizationId) { // Added organizationId
        // Vérifier l'unicité du nom d'utilisateur
        utilisateurRepository.findByUsername(utilisateur.getUsername()).ifPresent(existingUser -> {
            if (utilisateur.getId() == null || !existingUser.getId().equals(utilisateur.getId())) {
                throw new IllegalStateException("Le nom d'utilisateur '" + utilisateur.getUsername() + "' est déjà utilisé.");
            }
        });

        // Gestion des rôles
        if (roles != null) {
            utilisateur.setRoles(String.join(",", roles));
        } else {
            utilisateur.setRoles(""); // ou une valeur par défaut si nécessaire
        }

        // Gestion de l'organisation
        if (organizationId != null) {
            Organization organization = organizationRepository.findById(organizationId)
                    .orElseThrow(() -> new IllegalArgumentException("Organisation non trouvée pour l'ID: " + organizationId));
            utilisateur.setOrganization(organization);
        } else {
            utilisateur.setOrganization(null); // Clear organization if none selected
        }

        // Gestion du code caissier
        if (utilisateur.getRoles().contains("ROLE_CAISSIER")) {
            if (utilisateur.getCode() == null || utilisateur.getCode().trim().isEmpty()) {
                throw new IllegalArgumentException("Le code caissier est obligatoire pour ce rôle.");
            }
            // Vérifier l'unicité du code
            utilisateurRepository.findByCode(utilisateur.getCode()).ifPresent(existingUser -> {
                if (utilisateur.getId() == null || !existingUser.getId().equals(utilisateur.getId())) {
                    throw new IllegalStateException("Le code caissier '" + utilisateur.getCode() + "' est déjà utilisé par un autre utilisateur.");
                }
            });
        } else {
            utilisateur.setCode(null); // Assurer que le code est nul si l'utilisateur n'est pas caissier
        }

        // Gestion du mot de passe
        if (utilisateur.getId() != null && (utilisateur.getPassword() == null || utilisateur.getPassword().isEmpty())) {
            Utilisateur existingUser = utilisateurRepository.findById(utilisateur.getId())
                .orElseThrow(() -> new IllegalArgumentException("Impossible de trouver l'utilisateur existant pour mettre à jour le mot de passe."));
            utilisateur.setPassword(existingUser.getPassword());
        } else {
            utilisateur.setPassword(passwordEncoder.encode(utilisateur.getPassword()));
        }

        utilisateurRepository.save(utilisateur);
        return ResponseEntity.ok(Map.of("success", true, "message", "L'utilisateur a été sauvegardé avec succès !"));
    }

    @PostMapping("/delete/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteUtilisateur(@PathVariable Long id, Principal principal) {
        Utilisateur userToDelete = utilisateurRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé avec l'ID: " + id));
        // Empêcher un admin de supprimer son propre compte
        if (principal.getName().equals(userToDelete.getUsername())) {
            throw new IllegalStateException("Vous ne pouvez pas supprimer votre propre compte.");
        }
        utilisateurRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("success", true, "message", "L'utilisateur a été supprimé avec succès !"));
    }
}
