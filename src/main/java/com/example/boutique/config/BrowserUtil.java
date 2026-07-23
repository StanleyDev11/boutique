package com.example.boutique.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.net.URI;
import java.util.concurrent.TimeUnit;

/**
 * Utilitaire d'ouverture du navigateur par défaut, robuste sous Windows.
 *
 * Sous {@code javaw.exe} (application fenêtrée), {@link java.awt.Desktop#browse}
 * échoue souvent silencieusement (contexte AWT indisponible / headless). On
 * privilégie donc des méthodes natives Windows indépendantes d'AWT
 * ({@code rundll32}, puis {@code cmd start}), avec {@code Desktop} en ultime
 * recours. Aucune exception n'est propagée : l'appelant n'est jamais bloqué.
 */
public final class BrowserUtil {

    private static final Logger logger = LoggerFactory.getLogger(BrowserUtil.class);

    private BrowserUtil() {
    }

    /** Ouvre l'URL dans le navigateur par défaut via une cascade de méthodes. */
    public static void open(String url) {
        // a) rundll32 : voie Windows la plus fiable, indépendante d'AWT.
        if (tryProcess(url, new String[]{"rundll32", "url.dll,FileProtocolHandler", url})) {
            return;
        }
        // b) cmd start : le "" sert de titre à la commande start ; l'URL est un arg séparé.
        if (tryProcess(url, new String[]{"cmd", "/c", "start", "\"\"", url})) {
            return;
        }
        // c) Desktop.browse : ultime recours (peut échouer sous javaw / headless).
        if (tryDesktop(url)) {
            return;
        }
        logger.warn("Impossible d'ouvrir le navigateur automatiquement. Ouvrez manuellement : {}", url);
    }

    private static boolean tryProcess(String url, String[] command) {
        try {
            Process process = new ProcessBuilder(command).start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (finished && process.exitValue() != 0) {
                logger.debug("Commande d'ouverture navigateur '{}' a retourné le code {}",
                        command[0], process.exitValue());
                return false;
            }
            logger.info("Navigateur ouvert automatiquement sur {} (via {}).", url, command[0]);
            return true;
        } catch (Exception e) {
            logger.debug("Échec de l'ouverture via '{}' : {}", command[0], e.getMessage());
            return false;
        }
    }

    private static boolean tryDesktop(String url) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(url));
                logger.info("Navigateur ouvert automatiquement sur {} (via Desktop).", url);
                return true;
            }
        } catch (Exception e) {
            logger.debug("Échec de l'ouverture via Desktop : {}", e.getMessage());
        }
        return false;
    }
}
