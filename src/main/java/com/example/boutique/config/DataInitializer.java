package com.example.boutique.config;

import com.example.boutique.model.Caisse;
import com.example.boutique.model.Parametre;
import com.example.boutique.repository.CaisseRepository;
import com.example.boutique.repository.ParametreRepository;
import com.example.boutique.service.ParametreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final CaisseRepository caisseRepository;
    private final ParametreRepository parametreRepository;

    public DataInitializer(CaisseRepository caisseRepository, ParametreRepository parametreRepository) {
        this.caisseRepository = caisseRepository;
        this.parametreRepository = parametreRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (caisseRepository.findFirstByActive(true).isEmpty()) {
            logger.warn("Aucune caisse active n'a été trouvée. Création d'une nouvelle caisse active.");
            Caisse caisse = new Caisse();
            caisse.setNom("Caisse Principale");
            caisse.setActive(true);
            caisseRepository.save(caisse);
            logger.info("La caisse '{}' a été créée et activée.", caisse.getNom());
        }

        initializeDefaultParameter(ParametreService.BOUTIQUE_NOM_KEY, "SUPERMARCHE BELALUXE");
        initializeDefaultParameter(ParametreService.BOUTIQUE_ADRESSE_KEY, "Bd. Jean Paul II, Près de la TDE, Hédzranawoé, Lomé - Togo");
        initializeDefaultParameter(ParametreService.BOUTIQUE_TELEPHONE_KEY, "Tél: (+228) 96 00 01 89 / 90 12 34 30");
        initializeDefaultParameter("seuil_stock_bas", "10");
        initializeDefaultParameter("jours_avant_peremption", "30");
    }

    private void initializeDefaultParameter(String key, String defaultValue) {
        if (parametreRepository.findByCle(key).isEmpty()) {
            Parametre parametre = new Parametre(key, defaultValue);
            parametreRepository.save(parametre);
            logger.info("Paramètre par défaut initialisé : {} = {}", key, defaultValue);
        }
    }
}