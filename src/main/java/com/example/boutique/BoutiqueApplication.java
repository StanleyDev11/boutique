package com.example.boutique;

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
import java.time.LocalDate;
import java.util.List;

@SpringBootApplication
@EnableCaching
public class BoutiqueApplication {

    private static final Logger logger = LoggerFactory.getLogger(BoutiqueApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(BoutiqueApplication.class, args);
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