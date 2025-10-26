package com.example.boutique;

import com.example.boutique.model.Produit;
import com.example.boutique.model.Utilisateur;
import com.example.boutique.repository.ProduitRepository;
import com.example.boutique.repository.UtilisateurRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.awt.Desktop;
import java.net.URI;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@SpringBootApplication
public class BoutiqueApplication {

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "false");
        SpringApplication.run(BoutiqueApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void launchBrowser() {
        try {
            String url = "http://localhost:8085";
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                Runtime rt = Runtime.getRuntime();
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("win")) {
                    rt.exec("rundll32 url.dll,FileProtocolHandler " + url);
                } else if (os.contains("mac")) {
                    rt.exec("open " + url);
                } else if (os.contains("nix") || os.contains("nux")) {
                    rt.exec("xdg-open " + url);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Bean
    CommandLineRunner commandLineRunner(ProduitRepository produitRepository, UtilisateurRepository utilisateurRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // Création de produits de démo
            if (produitRepository.count() == 0) {
                Produit p1 = new Produit();
                p1.setNom("Ordinateur Portable");
                p1.setPrixAchat(new BigDecimal("800.00"));
                p1.setPrixVenteUnitaire(new BigDecimal("1200.50"));
                p1.setCategorie("Électronique");
                p1.setQuantiteEnStock(50);
                p1.setDatePeremption(null);

                Produit p2 = new Produit();
                p2.setNom("Café en Grains");
                p2.setPrixAchat(new BigDecimal("10.00"));
                p2.setPrixVenteUnitaire(new BigDecimal("19.99"));
                p2.setCategorie("Alimentaire");
                p2.setQuantiteEnStock(200);
                p2.setDatePeremption(LocalDate.now().plusYears(1));

                produitRepository.saveAll(List.of(p1, p2));
            }

            // Création d'utilisateurs de démo
            if (utilisateurRepository.count() == 0) {
                Utilisateur admin = new Utilisateur();
                admin.setUsername("admin");
                admin.setPassword(passwordEncoder.encode("password"));
                admin.setRoles("ROLE_ADMIN,ROLE_GESTIONNAIRE");

                Utilisateur gestion = new Utilisateur();
                gestion.setUsername("gestion");
                gestion.setPassword(passwordEncoder.encode("password"));
                gestion.setRoles("ROLE_GESTIONNAIRE");

                utilisateurRepository.saveAll(List.of(admin, gestion));
            }
        };
    }
}