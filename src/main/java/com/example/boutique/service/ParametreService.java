package com.example.boutique.service;

import com.example.boutique.model.Parametre;
import com.example.boutique.repository.ParametreRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ParametreService {

    private final ParametreRepository parametreRepository;

    private static final String SEUIL_STOCK_BAS_KEY = "seuil_stock_bas";
    private static final String JOURS_AVANT_PEREMPTION_KEY = "jours_avant_peremption";
    private static final int SEUIL_STOCK_BAS_DEFAULT = 10;
    private static final int JOURS_AVANT_PEREMPTION_DEFAULT = 30;

    public ParametreService(ParametreRepository parametreRepository) {
        this.parametreRepository = parametreRepository;
    }

    public int getSeuilStockBas() {
        return parametreRepository.findByCle(SEUIL_STOCK_BAS_KEY)
                .map(p -> {
                    try {
                        return Integer.parseInt(p.getValeur());
                    } catch (NumberFormatException e) {
                        return SEUIL_STOCK_BAS_DEFAULT; // Fallback to default if value is not a valid integer
                    }
                })
                .orElse(SEUIL_STOCK_BAS_DEFAULT);
    }

    public int getJoursAvantPeremption() {
        return parametreRepository.findByCle(JOURS_AVANT_PEREMPTION_KEY)
                .map(p -> {
                    try {
                        return Integer.parseInt(p.getValeur());
                    } catch (NumberFormatException e) {
                        return JOURS_AVANT_PEREMPTION_DEFAULT; // Fallback to default
                    }
                })
                .orElse(JOURS_AVANT_PEREMPTION_DEFAULT);
    }


    public void updateParametres(Map<String, String> parametres) {
        List<String> validKeys = List.of(SEUIL_STOCK_BAS_KEY, JOURS_AVANT_PEREMPTION_KEY);

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
