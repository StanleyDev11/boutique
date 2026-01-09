package com.example.boutique.config;

import com.example.boutique.model.*;
import com.example.boutique.repository.*;
import com.example.boutique.service.ParametreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final CaisseRepository caisseRepository;
    private final ParametreRepository parametreRepository;
    private final PlanRepository planRepository;
    private final LicenceRepository licenceRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final OrganizationRepository organizationRepository; // Added
    private final ProduitRepository produitRepository; // Added
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(CaisseRepository caisseRepository, ParametreRepository parametreRepository, PlanRepository planRepository, LicenceRepository licenceRepository, UtilisateurRepository utilisateurRepository, OrganizationRepository organizationRepository, ProduitRepository produitRepository, PasswordEncoder passwordEncoder) {
        this.caisseRepository = caisseRepository;
        this.parametreRepository = parametreRepository;
        this.planRepository = planRepository;
        this.licenceRepository = licenceRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.organizationRepository = organizationRepository; // Added
        this.produitRepository = produitRepository; // Added
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        initializeDefaultParameter(ParametreService.BOUTIQUE_NOM_KEY, "SUPERMARCHE BELALUXE");
        initializeDefaultParameter(ParametreService.BOUTIQUE_ADRESSE_KEY, "Bd. Jean Paul II, Près de la TDE, Hédzranawoé, Lomé - Togo");
        initializeDefaultParameter(ParametreService.BOUTIQUE_TELEPHONE_KEY, "Tél: (+228) 96 00 01 89 / 90 12 34 30");
        initializeDefaultParameter("seuil_stock_bas", "10");
        initializeDefaultParameter("jours_avant_peremption", "30");
        initializeDefaultParameter(ParametreService.TAILWIND_HEADER_BACKGROUND_COLOR_KEY, "#1F2937");
        initializeDefaultParameter(ParametreService.TAILWIND_HEADER_TEXT_COLOR_KEY, "#D1D5DB");

        // Initialize Organizations
        Organization defaultOrg = initializeOrganization("Default Organization", "default");
        Organization premiumOrg = initializeOrganization("Premium Client Corp", "premium");

        // Initialize Plans
        Plan planUnique = initializePlan("LICENCE_UNIQUE", 500.0, Arrays.asList(Feature.values()));
        Plan planBasic = initializePlan("BASIC", 20.0, Arrays.asList(Feature.VENTE_CAISSIER, Feature.GESTION_PRODUITS));
        Plan planPro = initializePlan("PRO", 50.0, Arrays.asList(Feature.VENTE_CAISSIER, Feature.GESTION_PRODUITS, Feature.GESTION_CAISSES, Feature.RAPPORTS_STOCK));

        // Initialize Users
        Utilisateur admin = initializeUser("admin", "password", "ROLE_ADMIN,ROLE_GESTIONNAIRE", defaultOrg);
        Utilisateur superadmin = initializeUser("superadmin", "password", "ROLE_SUPER_ADMIN", defaultOrg); // Superadmin is part of default org
        Utilisateur gestion = initializeUser("gestion", "password", "ROLE_GESTIONNAIRE", defaultOrg);
        Utilisateur testuser = initializeUser("testuser", "password", "ROLE_CAISSIER", premiumOrg);

        // Initialize Licenses
        initializeLicence(superadmin, planUnique, LocalDate.now(), null, LicenceStatus.ACTIVE);
        initializeLicence(testuser, planBasic, LocalDate.now(), LocalDate.now().plusMonths(1), LicenceStatus.ACTIVE);

        // Initialize Caisse
        initializeCaisse("Caisse Principale", defaultOrg.getClientId(), true);

        // Initialize Produits
        initializeProduit("Ordinateur Portable", null, new BigDecimal("800.00"), new BigDecimal("1200.50"), "Électronique", BigDecimal.valueOf(50), "PIECE", null, defaultOrg.getClientId());
        initializeProduit("Café en Grains", null, new BigDecimal("10.00"), new BigDecimal("19.99"), "Alimentaire", BigDecimal.valueOf(200), "KG", LocalDate.now().plusYears(1), defaultOrg.getClientId());
    }

    private void initializeDefaultParameter(String key, String defaultValue) {
        if (parametreRepository.findByCle(key).isEmpty()) {
            Parametre parametre = new Parametre(key, defaultValue);
            parametreRepository.save(parametre);
            logger.info("Paramètre par défaut initialisé : {} = {}", key, defaultValue);
        }
    }

    private Organization initializeOrganization(String nom, String clientId) {
        Optional<Organization> existingOrg = organizationRepository.findByClientId(clientId);
        Organization org = existingOrg.orElseGet(Organization::new);

        org.setNom(nom);
        org.setClientId(clientId);
        organizationRepository.save(org);
        logger.info("Organisation initialisée : {} ({})", nom, clientId);
        return org;
    }

    private Utilisateur initializeUser(String username, String password, String roles, Organization organization) {
        Optional<Utilisateur> existingUser = utilisateurRepository.findByUsername(username);
        Utilisateur user = existingUser.orElseGet(Utilisateur::new);

        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRoles(roles);
        user.setOrganization(organization); // Set the organization
        utilisateurRepository.save(user);
        logger.info("Utilisateur initialisé : {} dans l'organisation {}", username, organization.getNom());
        return user;
    }

    private Plan initializePlan(String nom, Double prix, List<Feature> features) {
        Optional<Plan> existingPlan = planRepository.findByNom(nom);
        Plan plan = existingPlan.orElseGet(Plan::new);

        plan.setNom(nom);
        plan.setPrix(prix);
        plan.setFeatures(features);
        planRepository.save(plan);
        logger.info("Plan initialisé : {}", nom);
        return plan;
    }

    private Caisse initializeCaisse(String nom, String clientId, boolean active) {
        Optional<Caisse> existingCaisse = caisseRepository.findByNomAndClientId(nom, clientId);
        Caisse caisse = existingCaisse.orElseGet(Caisse::new);

        caisse.setNom(nom);
        caisse.setClientId(clientId);
        caisse.setActive(active);
        caisseRepository.save(caisse);
        logger.info("Caisse initialisée : {} pour le client {}", nom, clientId);
        return caisse;
    }

    private Produit initializeProduit(String nom, String codeBarres, BigDecimal prixAchat, BigDecimal prixVenteUnitaire, String categorie, BigDecimal quantiteEnStock, String uniteDeVente, LocalDate datePeremption, String clientId) {
        Optional<Produit> existingProduit = produitRepository.findByNomAndClientId(nom, clientId);
        Produit produit = existingProduit.orElseGet(Produit::new);

        produit.setNom(nom);
        produit.setCodeBarres(codeBarres);
        produit.setPrixAchat(prixAchat);
        produit.setPrixVenteUnitaire(prixVenteUnitaire);
        produit.setCategorie(categorie);
        produit.setQuantiteEnStock(quantiteEnStock);
        produit.setUniteDeVente(uniteDeVente);
        produit.setDatePeremption(datePeremption);
        produit.setClientId(clientId);
        produitRepository.save(produit);
        logger.info("Produit initialisé : {} pour le client {}", nom, clientId);
        return produit;
    }

    private Licence initializeLicence(Utilisateur utilisateur, Plan plan, LocalDate dateDebut, LocalDate dateFin, LicenceStatus status) {
        Optional<Licence> existingLicence = licenceRepository.findByUtilisateur(utilisateur);
        Licence licence = existingLicence.orElseGet(Licence::new);

        licence.setUtilisateur(utilisateur);
        licence.setPlan(plan);
        licence.setDateDebut(dateDebut);
        licence.setDateFin(dateFin);
        licence.setStatut(status);
        licenceRepository.save(licence);
        logger.info("Licence initialisée pour l'utilisateur {} avec le plan {}", utilisateur.getUsername(), plan.getNom());
        return licence;
    }
}