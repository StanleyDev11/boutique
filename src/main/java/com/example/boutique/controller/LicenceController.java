package com.example.boutique.controller;

import com.example.boutique.model.Licence;
import com.example.boutique.model.LicenceStatus;
import com.example.boutique.repository.LicenceRepository;
import com.example.boutique.repository.PlanRepository;
import com.example.boutique.repository.UtilisateurRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
@RequestMapping("/superadmin/licences")
public class LicenceController {

    private final LicenceRepository licenceRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final PlanRepository planRepository;

    public LicenceController(LicenceRepository licenceRepository, UtilisateurRepository utilisateurRepository, PlanRepository planRepository) {
        this.licenceRepository = licenceRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.planRepository = planRepository;
    }

    @GetMapping
    public String listLicences(Model model) {
        model.addAttribute("licences", licenceRepository.findAll());
        return "superadmin/licences/list";
    }

    @GetMapping("/new")
    public String showNewLicenceForm(Model model) {
        model.addAttribute("licence", new Licence());
        model.addAttribute("users", utilisateurRepository.findAll());
        model.addAttribute("plans", planRepository.findAll());
        return "superadmin/licences/form";
    }

    @PostMapping("/save")
    public String saveLicence(@ModelAttribute Licence licence) {
        licenceRepository.save(licence);
        return "redirect:/superadmin/licences";
    }

    @GetMapping("/edit/{id}")
    public String showEditLicenceForm(@PathVariable Long id, Model model) {
        Optional<Licence> licence = licenceRepository.findById(id);
        if (licence.isPresent()) {
            model.addAttribute("licence", licence.get());
            model.addAttribute("users", utilisateurRepository.findAll());
            model.addAttribute("plans", planRepository.findAll());
            return "superadmin/licences/form";
        }
        return "redirect:/superadmin/licences";
    }

    @PostMapping("/update/{id}")
    public String updateLicence(@PathVariable Long id, @ModelAttribute Licence licence) {
        licence.setId(id);
        licenceRepository.save(licence);
        return "redirect:/superadmin/licences";
    }

    @GetMapping("/delete/{id}")
    public String deleteLicence(@PathVariable Long id) {
        licenceRepository.findById(id).ifPresent(licence -> {
            licence.setStatut(LicenceStatus.ANNULEE);
            licenceRepository.save(licence);
        });
        return "redirect:/superadmin/licences";
    }
}
