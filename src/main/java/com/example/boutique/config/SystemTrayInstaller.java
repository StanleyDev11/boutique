package com.example.boutique.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.io.InputStream;

/**
 * Installe une icône dans la barre des tâches système (System Tray) — profil
 * "desktop" uniquement.
 *
 * L'application étant un serveur web sans fenêtre, cette icône permet à
 * l'utilisateur de SAVOIR qu'elle tourne et de la QUITTER proprement (ce qui
 * libère le port 8085). Menu : « Ouvrir Boutique » et « Quitter ».
 *
 * Ne charge rien en profil dev. Ne fait jamais échouer le démarrage : tout
 * problème (tray non supporté, image illisible…) est simplement journalisé.
 */
@Component
@Profile("desktop")
public class SystemTrayInstaller {

    private static final Logger logger = LoggerFactory.getLogger(SystemTrayInstaller.class);

    private final Environment environment;
    private final ConfigurableApplicationContext context;

    public SystemTrayInstaller(Environment environment, ConfigurableApplicationContext context) {
        this.environment = environment;
        this.context = context;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void install() {
        if (!SystemTray.isSupported()) {
            logger.info("La barre des tâches système (SystemTray) n'est pas supportée sur cette plateforme.");
            return;
        }
        try {
            String url = "http://localhost:" + environment.getProperty("server.port", "8085");

            PopupMenu popup = new PopupMenu();

            MenuItem openItem = new MenuItem("Ouvrir Boutique");
            openItem.addActionListener(e -> BrowserUtil.open(url));

            MenuItem quitItem = new MenuItem("Quitter");
            quitItem.addActionListener(e -> quit());

            popup.add(openItem);
            popup.addSeparator();
            popup.add(quitItem);

            TrayIcon trayIcon = new TrayIcon(loadTrayImage(), "Boutique — en cours d'exécution", popup);
            trayIcon.setImageAutoSize(true);
            // Double-clic sur l'icône = ouvrir l'application.
            trayIcon.addActionListener(e -> BrowserUtil.open(url));

            SystemTray.getSystemTray().add(trayIcon);
            logger.info("Icône de la barre des tâches système installée (menu : Ouvrir Boutique / Quitter).");
        } catch (Exception e) {
            logger.warn("Impossible d'installer l'icône de la barre système : {}", e.getMessage());
        }
    }

    /** Arrêt propre : ferme le contexte Spring (libère le port) puis termine la JVM. */
    private void quit() {
        logger.info("Fermeture demandée depuis la barre système : arrêt de l'application.");
        int exitCode = SpringApplication.exit(context, () -> 0);
        System.exit(exitCode);
    }

    /** Charge l'image de la barre système depuis les ressources (PNG lisible par AWT). */
    private Image loadTrayImage() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/tray-icon.png")) {
            if (in != null) {
                return ImageIO.read(in);
            }
        }
        // Repli : réutiliser le logo statique si présent.
        try (InputStream in = getClass().getResourceAsStream("/static/lo.png")) {
            if (in != null) {
                return ImageIO.read(in);
            }
        }
        throw new IllegalStateException("Aucune image d'icône trouvée dans les ressources.");
    }
}
