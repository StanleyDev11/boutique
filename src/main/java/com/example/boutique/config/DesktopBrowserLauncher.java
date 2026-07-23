package com.example.boutique.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Ouvre automatiquement le navigateur par défaut sur l'application au démarrage.
 *
 * Actif uniquement sous le profil "desktop" (packaging portable / installateur) :
 * le double-clic sur le raccourci lance le serveur ET affiche l'interface. En
 * profil dev classique, ce composant n'est pas chargé.
 *
 * L'ouverture réelle est déléguée à {@link BrowserUtil} (cascade rundll32 →
 * cmd start → Desktop) et exécutée dans un thread daemon pour ne jamais bloquer
 * le démarrage de l'application.
 */
@Component
@Profile("desktop")
public class DesktopBrowserLauncher {

    private static final Logger logger = LoggerFactory.getLogger(DesktopBrowserLauncher.class);

    private final Environment environment;

    public DesktopBrowserLauncher(Environment environment) {
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void openBrowser() {
        String port = environment.getProperty("server.port", "8085");
        String url = "http://localhost:" + port;
        logger.info("Ouverture automatique du navigateur sur {}", url);

        Thread t = new Thread(() -> BrowserUtil.open(url), "browser-launcher");
        t.setDaemon(true);
        t.start();
    }
}
