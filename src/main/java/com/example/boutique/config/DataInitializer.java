package com.example.boutique.config;

import com.example.boutique.model.Caisse;
import com.example.boutique.model.Parametre;
import com.example.boutique.repository.CaisseRepository;
import com.example.boutique.repository.ParametreRepository;
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

        if (parametreRepository.findByCle("nom_entreprise").isEmpty()) {
            Parametre nomEntreprise = new Parametre("nom_entreprise", "SUPERMARCHE BELALUXE");
            parametreRepository.save(nomEntreprise);
        }
        if (parametreRepository.findByCle("adresse_entreprise").isEmpty()) {
            Parametre adresseEntreprise = new Parametre("adresse_entreprise", "Bd. Jean Paul II, Près de la TDE, Hédzran");
            parametreRepository.save(adresseEntreprise);
        }
        if (parametreRepository.findByCle("telephone_entreprise").isEmpty()) {
            Parametre telephoneEntreprise = new Parametre("telephone_entreprise", "(+228) 96 00 01 89 / 90 12 34 30");
            parametreRepository.save(telephoneEntreprise);
        }
    }
}
