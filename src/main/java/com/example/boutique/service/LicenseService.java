package com.example.boutique.service;

import com.example.boutique.model.Parametre;
import com.example.boutique.repository.ParametreRepository;
import com.example.boutique.security.AccountConstants;
import com.example.boutique.security.LicenseAwaitingActivationException;
import com.example.boutique.security.LicenseKeyUtil;
import com.example.boutique.security.TrialExpiredException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Cœur de la logique d'essai (14 jours) + licence à vie (activation hors-ligne).
 *
 * État persisté via l'entité Parametre (table clé/valeur) :
 *   - license.install_id   : UUID stable généré à la première utilisation
 *   - license.install_date : instant (epoch millis) de première utilisation
 *   - license.activated    : "true"/"false" (défaut false)
 *   - license.key          : clé d'activation saisie (pour information)
 */
@Service
public class LicenseService {

    private static final Logger logger = LoggerFactory.getLogger(LicenseService.class);

    public static final String INSTALL_ID_KEY = "license.install_id";
    public static final String INSTALL_DATE_KEY = "license.install_date";
    public static final String ACTIVATED_KEY = "license.activated";
    public static final String KEY_KEY = "license.key";

    public static final int TRIAL_DAYS = 14;

    private final ParametreRepository parametreRepository;

    public LicenseService(ParametreRepository parametreRepository) {
        this.parametreRepository = parametreRepository;
    }

    /**
     * Garantit la présence de l'identifiant d'installation ET de la date de
     * première utilisation. Appelé à la première ouverture de /login.
     */
    public synchronized void ensureInitialized() {
        if (parametreRepository.findByCle(INSTALL_ID_KEY).isEmpty()) {
            String id = UUID.randomUUID().toString();
            parametreRepository.save(new Parametre(INSTALL_ID_KEY, id));
            logger.info("Licence : identifiant d'installation généré.");
        }
        if (parametreRepository.findByCle(INSTALL_DATE_KEY).isEmpty()) {
            parametreRepository.save(new Parametre(INSTALL_DATE_KEY,
                    Long.toString(Instant.now().toEpochMilli())));
            logger.info("Licence : début de la période d'essai enregistré.");
        }
    }

    public String getInstallId() {
        ensureInitialized();
        return parametreRepository.findByCle(INSTALL_ID_KEY)
                .map(Parametre::getValeur)
                .orElse("");
    }

    /** Identifiant d'installation formaté et lisible (à communiquer au support). */
    public String getInstallationIdentifier() {
        return LicenseKeyUtil.group(getInstallId());
    }

    public boolean isActivated() {
        return parametreRepository.findByCle(ACTIVATED_KEY)
                .map(p -> "true".equalsIgnoreCase(p.getValeur()))
                .orElse(false);
    }

    public Instant getInstallDate() {
        ensureInitialized();
        return parametreRepository.findByCle(INSTALL_DATE_KEY)
                .map(p -> {
                    try {
                        return Instant.ofEpochMilli(Long.parseLong(p.getValeur()));
                    } catch (NumberFormatException e) {
                        return Instant.now();
                    }
                })
                .orElse(Instant.now());
    }

    /** Instant de fin d'essai (install_date + 14 jours) en epoch millisecondes UTC. */
    public long getTrialEndMs() {
        return getInstallDate().plus(Duration.ofDays(TRIAL_DAYS)).toEpochMilli();
    }

    public boolean isTrialExpired() {
        if (isActivated()) {
            return false;
        }
        Instant expiry = getInstallDate().plus(Duration.ofDays(TRIAL_DAYS));
        return Instant.now().isAfter(expiry);
    }

    public long getTrialDaysRemaining() {
        if (isActivated()) {
            return Long.MAX_VALUE;
        }
        Instant expiry = getInstallDate().plus(Duration.ofDays(TRIAL_DAYS));
        long millis = expiry.toEpochMilli() - Instant.now().toEpochMilli();
        if (millis <= 0) {
            return 0;
        }
        return (long) Math.ceil(millis / (1000.0 * 60 * 60 * 24));
    }

    /** Clé d'activation attendue pour cette installation (usage interne/support). */
    public String getExpectedKey() {
        return LicenseKeyUtil.computeKey(getInstallId());
    }

    /** Active la licence via une clé saisie par le client. */
    public synchronized boolean activateWithKey(String key) {
        if (!LicenseKeyUtil.isValidKey(getInstallId(), key)) {
            return false;
        }
        setActivated(true);
        parametreRepository.save(new Parametre(KEY_KEY, LicenseKeyUtil.group(key)));
        logger.info("Licence : activation réussie via clé.");
        return true;
    }

    /** Activation manuelle (secours super admin), sans clé. */
    public synchronized void activateManually() {
        setActivated(true);
        parametreRepository.save(new Parametre(KEY_KEY, "MANUAL-SUPERADMIN"));
        logger.warn("Licence : activation manuelle par le super admin.");
    }

    private void setActivated(boolean activated) {
        Parametre p = parametreRepository.findByCle(ACTIVATED_KEY)
                .orElse(new Parametre(ACTIVATED_KEY, "false"));
        p.setValeur(Boolean.toString(activated));
        parametreRepository.save(p);
    }

    /**
     * Applique les règles d'accès au moment du login pour un utilisateur de la
     * base. Le super admin caché n'est jamais soumis à ces règles (il est
     * authentifié par un provider dédié qui n'appelle pas cette méthode).
     */
    public void enforceLoginRules(Authentication authentication) {
        boolean isDemo = hasAuthority(authentication, AccountConstants.ROLE_DEMO);

        if (isActivated()) {
            // App débloquée à vie : le compte démo reste neutralisé.
            if (isDemo) {
                throw new LicenseAwaitingActivationException(
                        "Le compte de démonstration est désactivé après activation de la licence.");
            }
            return;
        }

        // Non activé.
        if (isDemo) {
            if (isTrialExpired()) {
                throw new TrialExpiredException("Période d'essai expirée.");
            }
            return; // essai en cours : le compte démo peut se connecter
        }

        // Compte non-démo (ex: admin.boutika) pendant l'essai : bloqué.
        throw new LicenseAwaitingActivationException(
                "Ce compte sera disponible après l'activation de la licence.");
    }

    private boolean hasAuthority(Authentication authentication, String role) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        for (GrantedAuthority a : authentication.getAuthorities()) {
            if (role.equals(a.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
