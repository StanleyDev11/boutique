package com.example.boutique.service;

import com.example.boutique.model.Parametre;
import com.example.boutique.repository.ParametreRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ParametreService {

    private final ParametreRepository parametreRepository;

    // Numerical parameters
    private static final String SEUIL_STOCK_BAS_KEY = "seuil_stock_bas";
    private static final String JOURS_AVANT_PEREMPTION_KEY = "jours_avant_peremption";
    private static final int SEUIL_STOCK_BAS_DEFAULT = 10;
    private static final int JOURS_AVANT_PEREMPTION_DEFAULT = 30;

    // Boutique Info Parameters
    public static final String BOUTIQUE_NOM_KEY = "boutique.nom";
    public static final String BOUTIQUE_ADRESSE_KEY = "boutique.adresse";
    public static final String BOUTIQUE_TELEPHONE_KEY = "boutique.telephone";
    public static final String BOUTIQUE_LOGO_KEY = "boutique.logo";
    private static final String BOUTIQUE_NOM_DEFAULT = "SUPERMARCHE BELALUXE";
    private static final String BOUTIQUE_ADRESSE_DEFAULT = "Bd. Jean Paul II, Près de la TDE, Hédzranawoé, Lomé - Togo";
    private static final String BOUTIQUE_TELEPHONE_DEFAULT = "Tél: (+228) 96 00 01 89 / 90 12 34 30";
    private static final String BOUTIQUE_LOGO_DEFAULT = "/lo.png";


    public ParametreService(ParametreRepository parametreRepository) {
        this.parametreRepository = parametreRepository;
    }

    private String getStringParametre(String key, String defaultValue) {
        return parametreRepository.findByCle(key)
                .map(Parametre::getValeur)
                .orElse(defaultValue);
    }

    private int getIntParametre(String key, int defaultValue) {
        return parametreRepository.findByCle(key)
                .map(p -> {
                    try {
                        return Integer.parseInt(p.getValeur());
                    } catch (NumberFormatException e) {
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }

    public String getBoutiqueNom() {
        return getStringParametre(BOUTIQUE_NOM_KEY, BOUTIQUE_NOM_DEFAULT);
    }

    public String getBoutiqueAdresse() {
        return getStringParametre(BOUTIQUE_ADRESSE_KEY, BOUTIQUE_ADRESSE_DEFAULT);
    }

    public String getBoutiqueTelephone() {
        return getStringParametre(BOUTIQUE_TELEPHONE_KEY, BOUTIQUE_TELEPHONE_DEFAULT);
    }

    public String getBoutiqueLogo() {
        return getStringParametre(BOUTIQUE_LOGO_KEY, BOUTIQUE_LOGO_DEFAULT);
    }

    public int getSeuilStockBas() {
        return getIntParametre(SEUIL_STOCK_BAS_KEY, SEUIL_STOCK_BAS_DEFAULT);
    }

    public int getJoursAvantPeremption() {
        return getIntParametre(JOURS_AVANT_PEREMPTION_KEY, JOURS_AVANT_PEREMPTION_DEFAULT);
    }

    public void updateParametres(Map<String, String> parametres) {
        List<String> validKeys = List.of(
            SEUIL_STOCK_BAS_KEY, 
            JOURS_AVANT_PEREMPTION_KEY,
            BOUTIQUE_NOM_KEY,
            BOUTIQUE_ADRESSE_KEY,
            BOUTIQUE_TELEPHONE_KEY,
            BOUTIQUE_LOGO_KEY
        );

        for (String key : validKeys) {
            if (parametres.containsKey(key)) {
                String value = parametres.get(key);
                Parametre parametre = parametreRepository.findByCle(key)
                        .orElse(new Parametre(key, value));
                parametre.setValeur(value);
                parametreRepository.save(parametre);
            }
        }
    }

    public Map<String, String> getAllParametres() {
        return parametreRepository.findAll().stream()
                .collect(Collectors.toMap(Parametre::getCle, Parametre::getValeur));
    }
}