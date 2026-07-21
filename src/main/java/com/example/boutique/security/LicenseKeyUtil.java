package com.example.boutique.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Utilitaires cryptographiques pour la licence hors-ligne.
 *
 * Algorithme de la clé d'activation :
 *   1. On part de l'identifiant d'installation (UUID stable stocké en base).
 *   2. On le normalise : suppression de tout caractère non alphanumérique + MAJUSCULES.
 *   3. cle = HMAC-SHA256(SECRET, identifiantNormalisé).
 *   4. On encode les premiers octets du HMAC en Base32 (RFC 4648, sans padding).
 *   5. On garde 20 caractères, formatés en 5 groupes de 4 séparés par des tirets.
 *
 * La validation refait exactement le même calcul et compare (après normalisation)
 * la clé saisie avec la clé attendue.
 *
 * LIMITE DE SÉCURITÉ : l'application étant 100% hors-ligne, le SECRET est
 * obligatoirement embarqué dans le binaire. Ce n'est donc PAS un secret serveur :
 * quelqu'un qui décompile le .jar peut retrouver le SECRET et générer des clés.
 * Il s'agit d'une protection par obfuscation, pas d'une sécurité cryptographique
 * forte. Le seul générateur "officiel" reste l'outil du propriétaire.
 */
public final class LicenseKeyUtil {

    private LicenseKeyUtil() {
    }

    /**
     * SECRET embarqué (aléatoire, 256 bits en Base64). Utilisé par l'application
     * ET par le générateur de clés (tools/LicenseKeyGenerator). NE PAS modifier
     * après diffusion, sinon toutes les clés déjà générées deviennent invalides.
     */
    static final String SECRET = "b7Qw9K2mZ4xR8vN1sT6yU3pC0aH5jE7dL2fG9kV4nB8oM1qX6rW3zY0tS5uP8cA";

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"; // Base32 RFC 4648
    private static final int KEY_LENGTH = 20; // 5 groupes de 4

    /** Normalise un identifiant/clé : alphanumérique uniquement, en majuscules. */
    public static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
    }

    /** Calcule la clé d'activation attendue pour un identifiant d'installation donné. */
    public static String computeKey(String installId) {
        String normalizedId = normalize(installId);
        byte[] hmac = hmacSha256(SECRET, normalizedId);
        String base32 = base32Encode(hmac);
        String raw = base32.substring(0, Math.min(KEY_LENGTH, base32.length()));
        return group(raw);
    }

    /** Vérifie qu'une clé saisie correspond à l'identifiant d'installation. */
    public static boolean isValidKey(String installId, String candidate) {
        String expected = normalize(computeKey(installId));
        String given = normalize(candidate);
        if (expected.isEmpty() || given.isEmpty()) {
            return false;
        }
        return constantTimeEquals(expected, given);
    }

    /** Formatage lisible d'un identifiant/clé en groupes de 4 séparés par des tirets. */
    public static String group(String value) {
        String normalized = normalize(value);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < normalized.length(); i++) {
            if (i > 0 && i % 4 == 0) {
                sb.append('-');
            }
            sb.append(normalized.charAt(i));
        }
        return sb.toString();
    }

    private static byte[] hmacSha256(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de calculer le HMAC de licence", e);
        }
    }

    private static String base32Encode(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int buffer = 0;
        int bitsLeft = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                int index = (buffer >> (bitsLeft - 5)) & 0x1F;
                bitsLeft -= 5;
                sb.append(ALPHABET.charAt(index));
            }
        }
        if (bitsLeft > 0) {
            int index = (buffer << (5 - bitsLeft)) & 0x1F;
            sb.append(ALPHABET.charAt(index));
        }
        return sb.toString();
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
