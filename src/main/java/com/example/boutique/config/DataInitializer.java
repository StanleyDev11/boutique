package com.example.boutique.config;

import com.example.boutique.model.Caisse;
import com.example.boutique.repository.CaisseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final CaisseRepository caisseRepository;

    public DataInitializer(CaisseRepository caisseRepository) {
        this.caisseRepository = caisseRepository;
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
    }
}
