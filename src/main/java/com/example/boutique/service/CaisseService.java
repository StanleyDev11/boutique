package com.example.boutique.service;

import com.example.boutique.model.Caisse;
import com.example.boutique.model.Utilisateur;
import com.example.boutique.repository.CaisseRepository;
import com.example.boutique.repository.UtilisateurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CaisseService {

    private final CaisseRepository caisseRepository;
    private final UtilisateurRepository utilisateurRepository;

    public CaisseService(CaisseRepository caisseRepository, UtilisateurRepository utilisateurRepository) {
        this.caisseRepository = caisseRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    public List<Caisse> getAllCaisses() {
        return caisseRepository.findAll();
    }

    public Optional<Caisse> getCaisseById(Long id) {
        return caisseRepository.findById(id);
    }

    public Caisse createCaisse(Caisse caisse) {
        return caisseRepository.save(caisse);
    }

    public Caisse updateCaisse(Long id, Caisse caisseDetails) {
        Caisse caisse = caisseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Caisse non trouvée avec l'id : " + id));

        caisse.setNom(caisseDetails.getNom());
        caisse.setActive(caisseDetails.isActive());
        caisse.setUtilisateur(caisseDetails.getUtilisateur());

        return caisseRepository.save(caisse);
    }

    public void deleteCaisse(Long id) {
        caisseRepository.deleteById(id);
    }

    public Caisse activateCaisse(Long id) {
        Caisse caisse = caisseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Caisse non trouvée avec l'id : " + id));
        caisse.setActive(true);
        return caisseRepository.save(caisse);
    }

    public Caisse deactivateCaisse(Long id) {
        Caisse caisse = caisseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Caisse non trouvée avec l'id : " + id));
        caisse.setActive(false);
        return caisseRepository.save(caisse);
    }

    public Caisse assignerCaissier(Long caisseId, Long utilisateurId) {
        Caisse caisse = caisseRepository.findById(caisseId)
                .orElseThrow(() -> new RuntimeException("Caisse non trouvée avec l'id : " + caisseId));

        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'id : " + utilisateurId));

        // Optional: Check if the user has the 'CAISSIER' role
        if (!utilisateur.getRoles().contains("ROLE_CAISSIER")) {
            throw new RuntimeException("L'utilisateur n'a pas le rôle de caissier.");
        }

        caisse.setUtilisateur(utilisateur);
        return caisseRepository.save(caisse);
    }
}
