package com.example.boutique;

import com.example.boutique.config.BrowserUtil;
import com.example.boutique.model.Produit;
import com.example.boutique.model.Utilisateur;
import com.example.boutique.repository.ProduitRepository;
import com.example.boutique.repository.UtilisateurRepository;
import com.example.boutique.security.AccountConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.LocalDate;
import java.util.List;

@SpringBootApplication
@EnableCaching
public class BoutiqueApplication {

    private static final Logger logger = LoggerFactory.getLogger(BoutiqueApplication.class);

    /** Port du serveur (aligné sur server.port de application.yml). */
    private static final int DESKTOP_PORT = 8085;

    public static void main(String[] args) {
        // --- Garde "instance unique" : UNIQUEMENT en profil desktop ---
        // Si une instance écoute déjà sur le port 8085 (l'utilisateur a fermé le
        // navigateur mais le serveur tourne encore), on ne démarre PAS un 2e
        // serveur : on rouvre simplement le navigateur sur l'instance existante
        // puis on quitte proprement. Évite l'échec "port déjà occupé" au relancement.
        boolean desktop = isDesktopProfile();
        if (desktop && isAlreadyRunning(DESKTOP_PORT)) {
            String url = "http://localhost:" + DESKTOP_PORT;
            logger.info("Instance déjà en cours sur le port {} : réouverture du navigateur puis sortie.", DESKTOP_PORT);
            BrowserUtil.open(url);
            System.exit(0);
        }

        SpringApplication application = new SpringApplication(BoutiqueApplication.class);
        if (desktop) {
            // Indispensable pour l'icône SystemTray : Spring Boot fixe
            // java.awt.headless=true AVANT de charger application.yml, donc
            // "spring.main.headless" dans le yml n'a aucun effet. On force donc
            // le mode non-headless directement sur l'instance SpringApplication,
            // et UNIQUEMENT en profil desktop (le profil dev reste inchangé).
            application.setHeadless(false);
        }
        application.run(args);
    }

    /** Le profil desktop est actif quand l'exe/raccourci lance avec -Dspring.profiles.active=desktop. */
    private static boolean isDesktopProfile() {
        String prop = System.getProperty("spring.profiles.active", "");
        if (prop.contains("desktop")) {
            return true;
        }
        String env = System.getenv("SPRING_PROFILES_ACTIVE");
        return env != null && env.contains("desktop");
    }

    /** Test rapide : une instance écoute-t-elle déjà sur localhost:port ? */
    private static boolean isAlreadyRunning(int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", port), 800);
            return true;
        } catch (Exception e) {
            return false;
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
                p1.setQuantiteEnStock(BigDecimal.valueOf(50));
                p1.setUniteDeVente("PIECE");
                p1.setDatePeremption(null);

                Produit p2 = new Produit();
                p2.setNom("Café en Grains");
                p2.setPrixAchat(new BigDecimal("10.00"));
                p2.setPrixVenteUnitaire(new BigDecimal("19.99"));
                p2.setCategorie("Alimentaire");
                p2.setQuantiteEnStock(BigDecimal.valueOf(200));
                p2.setUniteDeVente("KG");
                p2.setDatePeremption(LocalDate.now().plusYears(1));

                produitRepository.saveAll(List.of(p1, p2));
            }

            // ----- Migration / mise en place des comptes -----
            // 1) Retirer les anciens comptes de démo (admin, gestion) s'ils existent
            //    dans une base déjà peuplée. Les hash sont embarqués, jamais en clair.
            for (String legacy : AccountConstants.LEGACY_DEMO_USERNAMES) {
                utilisateurRepository.findByUsername(legacy).ifPresent(u -> {
                    utilisateurRepository.delete(u);
                    logger.info("Ancien compte de démo supprimé : {}", legacy);
                });
            }

            // 2) Compte admin par défaut (verrouillé tant que la licence n'est pas activée)
            ensureAccount(utilisateurRepository,
                    AccountConstants.ADMIN_BOUTIKA_USERNAME,
                    AccountConstants.ADMIN_BOUTIKA_PASSWORD_HASH,
                    AccountConstants.ADMIN_BOUTIKA_ROLES);

            // 3) Compte d'essai / démo (caché, non modifiable, restrictions gestion users)
            ensureAccount(utilisateurRepository,
                    AccountConstants.CLIENTDEMO_USERNAME,
                    AccountConstants.CLIENTDEMO_PASSWORD_HASH,
                    AccountConstants.CLIENTDEMO_ROLES);

            // 4) Ligne DÉDIÉE au super admin (caché) : sert uniquement de support aux
            //    opérations caisse/ventes (FK obligatoire session_caisse -> utilisateur).
            //    Le login reste géré par SuperAdminAuthenticationProvider.
            ensureAccount(utilisateurRepository,
                    AccountConstants.SUPERADMIN_USERNAME,
                    AccountConstants.SUPERADMIN_PASSWORD_HASH,
                    AccountConstants.SUPERADMIN_ROLES);
        };
    }

    /**
     * Crée le compte s'il est absent. Ne réécrit PAS un compte existant : après
     * activation, le client peut avoir modifié admin.boutika (mot de passe/infos)
     * et on ne veut pas écraser ces changements.
     */
    private void ensureAccount(UtilisateurRepository repo, String username, String passwordHash, String roles) {
        if (repo.findByUsername(username).isEmpty()) {
            Utilisateur u = new Utilisateur();
            u.setUsername(username);
            u.setPassword(passwordHash); // déjà hashé en BCrypt (constante)
            u.setRoles(roles);
            repo.save(u);
            logger.info("Compte initialisé : {}", username);
        }
    }
}