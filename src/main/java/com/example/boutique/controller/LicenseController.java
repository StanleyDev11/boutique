package com.example.boutique.controller;

import com.example.boutique.service.LicenseService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/license")
public class LicenseController {

    private final LicenseService licenseService;

    public LicenseController(LicenseService licenseService) {
        this.licenseService = licenseService;
    }

    private void addContactInfo(Model model) {
        model.addAttribute("installId", licenseService.getInstallationIdentifier());
        model.addAttribute("phone1", "+228 99 18 16 26");
        model.addAttribute("phone2", "+228 92 59 56 61");
        model.addAttribute("wa1", "22899181626");
        model.addAttribute("wa2", "22892595661");
    }

    /** Page d'expiration de l'essai + formulaire de saisie de la clé. Accessible sans login. */
    @GetMapping("/expired")
    public String expired(@RequestParam(required = false) String invalidkey, Model model) {
        addContactInfo(model);
        model.addAttribute("invalidKey", invalidkey != null);
        return "license-expired";
    }

    /** Activation via clé saisie par le client (hors-ligne). Accessible sans login. */
    @PostMapping("/activate")
    public String activate(@RequestParam("key") String key) {
        boolean ok = licenseService.activateWithKey(key);
        if (ok) {
            return "redirect:/login?activated";
        }
        return "redirect:/license/expired?invalidkey";
    }

    /** Page de gestion réservée au super admin (activation manuelle de secours). */
    @GetMapping("/manage")
    public String manage(@RequestParam(required = false) String activated, Model model) {
        addContactInfo(model);
        model.addAttribute("activated", licenseService.isActivated());
        model.addAttribute("justActivated", activated != null);
        model.addAttribute("expectedKey", licenseService.getExpectedKey());
        model.addAttribute("trialExpired", licenseService.isTrialExpired());
        model.addAttribute("daysRemaining", licenseService.getTrialDaysRemaining());
        return "license-manage";
    }

    /** Déblocage manuel de la licence par le super admin, sans clé. */
    @PostMapping("/admin/activate")
    public String adminActivate() {
        licenseService.activateManually();
        return "redirect:/license/manage?activated";
    }
}
