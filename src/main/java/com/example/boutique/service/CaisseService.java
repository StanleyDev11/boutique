package com.example.boutique.service;

import com.example.boutique.model.Caisse;
import com.example.boutique.model.SessionCaisse;
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
    private final SessionCaisseRepository sessionCaisseRepository;
    private final VenteRepository venteRepository;

    public CaisseService(CaisseRepository caisseRepository, UtilisateurRepository utilisateurRepository, SessionCaisseRepository sessionCaisseRepository, VenteRepository venteRepository) {
        this.caisseRepository = caisseRepository;
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

        return caisseRepository.save(caisse);
    }

    public void deleteCaisse(Long id) {
        Caisse caisse = caisseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Caisse non trouvée avec l'id : " + id));

        // La logique de vérification de session ouverte doit être réévaluée car elle était liée à l'utilisateur de la caisse
        // Pour l'instant, on supprime la vérification directe ici.
        // Une vérification globale pourrait être nécessaire pour voir si une session est liée à un utilisateur
        // avant de supprimer une caisse, mais cela dépend des nouvelles règles métier.

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

        // La logique de vérification de session ouverte est retirée ici aussi.

        caisse.setActive(false);
        return caisseRepository.save(caisse);
    }

    public Page<SessionCaisse> getOpenSessions(String keyword, Pageable pageable) {
        return sessionCaisseRepository.findOpenSessions(keyword, pageable);
    }

    public Page<SessionCaisse> getClosedSessions(String keyword, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        LocalDateTime startDateTime = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = (endDate != null) ? endDate.atTime(LocalTime.MAX) : null;
        return sessionCaisseRepository.findClosedSessions(keyword, startDateTime, endDateTime, pageable);
    }

    public List<com.example.boutique.model.Vente> getVentesBySessionCaisse(SessionCaisse sessionCaisse) {
        return venteRepository.findBySessionCaisse(
                sessionCaisse,
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "dateVente")
        );
    }
}