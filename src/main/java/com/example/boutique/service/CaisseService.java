package com.example.boutique.service;

import com.example.boutique.model.Caisse;
import com.example.boutique.model.SessionCaisse;
import com.example.boutique.model.Utilisateur;
import com.example.boutique.repository.CaisseRepository;
import com.example.boutique.repository.UtilisateurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.boutique.repository.SessionCaisseRepository;
import com.example.boutique.repository.VenteRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CaisseService {

    private final CaisseRepository caisseRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final SessionCaisseRepository sessionCaisseRepository;
    private final VenteRepository venteRepository;

    public CaisseService(CaisseRepository caisseRepository, UtilisateurRepository utilisateurRepository, SessionCaisseRepository sessionCaisseRepository, VenteRepository venteRepository) {
        this.caisseRepository = caisseRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.sessionCaisseRepository = sessionCaisseRepository;
        this.venteRepository = venteRepository;
    }

    public List<Caisse> getAllCaisses() {
        return caisseRepository.findAll();
    }

    public List<Caisse> searchCaisses(String keyword) {
        if (keyword != null && !keyword.isEmpty()) {
            return caisseRepository.findByNomContainingIgnoreCase(keyword);
        }
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
        Caisse caisse = caisseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Caisse non trouvée avec l'id : " + id));

        Utilisateur utilisateur = caisse.getUtilisateur();

        if (utilisateur != null) {
            Optional<SessionCaisse> openSession = sessionCaisseRepository.findFirstByUtilisateurAndDateFermetureIsNullOrderByDateOuvertureDesc(utilisateur);
            if (openSession.isPresent()) {
                throw new IllegalStateException("Impossible de supprimer la caisse '" + caisse.getNom() + "' car une session est actuellement ouverte par le caissier '" + utilisateur.getUsername() + "'.");
            }
        }

        caisseRepository.delete(caisse);
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

        Utilisateur utilisateur = caisse.getUtilisateur();

        if (utilisateur != null) {
            Optional<SessionCaisse> openSession = sessionCaisseRepository.findFirstByUtilisateurAndDateFermetureIsNullOrderByDateOuvertureDesc(utilisateur);
            if (openSession.isPresent()) {
                throw new IllegalStateException("Impossible de désactiver la caisse '" + caisse.getNom() + "' car une session est actuellement ouverte par le caissier '" + utilisateur.getUsername() + "'.");
            }
        }

        caisse.setActive(false);
        return caisseRepository.save(caisse);
    }

    public Caisse assignerCaissier(Long caisseId, Long utilisateurId) {
        Caisse caisse = caisseRepository.findById(caisseId)
                .orElseThrow(() -> new RuntimeException("Caisse non trouvée avec l'id : " + caisseId));

        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'id : " + utilisateurId));

        if (!utilisateur.getRoles().contains("ROLE_CAISSIER")) {
            throw new RuntimeException("L'utilisateur n'a pas le rôle de caissier.");
        }

        caisse.setUtilisateur(utilisateur);
        return caisseRepository.save(caisse);
    }

    public Page<SessionCaisse> getOpenSessions(String keyword, Pageable pageable) {
        if (keyword != null && !keyword.isEmpty()) {
            return sessionCaisseRepository.findByDateFermetureIsNullAndUtilisateurUsernameContainingIgnoreCase(keyword, pageable);
        }
        return sessionCaisseRepository.findByDateFermetureIsNull(pageable);
    }

    public Page<SessionCaisse> getClosedSessions(String keyword, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        LocalDateTime startDateTime = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = (endDate != null) ? endDate.atTime(LocalTime.MAX) : null;
        return sessionCaisseRepository.findClosedSessions(keyword, startDateTime, endDateTime, pageable);
    }

    public List<com.example.boutique.model.Vente> getVentesBySessionCaisse(SessionCaisse sessionCaisse) {
        return venteRepository.findByUtilisateurAndDateVenteBetween(
                sessionCaisse.getUtilisateur(),
                sessionCaisse.getDateOuverture(),
                sessionCaisse.getDateFermeture() != null ? sessionCaisse.getDateFermeture() : LocalDateTime.now()
        );
    }
}