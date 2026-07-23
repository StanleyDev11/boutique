package com.example.boutique.controller;

import com.example.boutique.security.AccountConstants;
import com.example.boutique.service.LicenseService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.ByteArrayOutputStream;
import java.util.EnumMap;
import java.util.Map;

@Controller
@RequestMapping("/license")
public class LicenseController {

    private final LicenseService licenseService;

    public LicenseController(LicenseService licenseService) {
        this.licenseService = licenseService;
    }

    /**
     * Défense en profondeur : ne dépend pas uniquement de SecurityConfig.
     * Vérifie que l'utilisateur courant possède bien ROLE_SUPERADMIN, sinon
     * lève une AccessDeniedException (403). Protège les routes d'administration
     * de licence (activation manuelle sans clé).
     */
    private void requireSuperAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isSuperAdmin = auth != null && auth.isAuthenticated()
                && auth.getAuthorities() != null
                && auth.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .anyMatch(AccountConstants.ROLE_SUPERADMIN::equals);
        if (!isSuperAdmin) {
            throw new AccessDeniedException("Accès réservé au super administrateur.");
        }
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
        // Licence déjà activée : cet écran (destiné à un essai expiré) n'a plus
        // de raison d'être. On renvoie vers le login.
        if (licenseService.isActivated()) {
            return "redirect:/login";
        }
        addContactInfo(model);
        model.addAttribute("invalidKey", invalidkey != null);
        return "license-expired";
    }

    /**
     * Génère un QR code (PNG) à partir d'un texte, 100% hors-ligne (ZXing embarqué,
     * aucun appel réseau). Utilisé par la page tarifs pour encoder l'URL WhatsApp
     * pré-remplie selon le plan sélectionné. Accessible sans login.
     */
    @GetMapping("/qr")
    @ResponseBody
    public org.springframework.http.ResponseEntity<byte[]> qr(@RequestParam("text") String text) {
        try {
            String content = (text == null || text.isBlank()) ? " " : text;
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 1);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 320, 320, hints);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return org.springframework.http.ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(out.toByteArray());
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.internalServerError().build();
        }
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
        requireSuperAdmin();
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
        requireSuperAdmin();
        licenseService.activateManually();
        return "redirect:/license/manage?activated";
    }
}
