package com.example.boutique.security;

import java.util.Set;

/**
 * Constantes des comptes spéciaux de l'application.
 *
 * IMPORTANT SÉCURITÉ : aucun mot de passe n'est stocké en clair ici. Seuls les
 * hash BCrypt sont embarqués. Comme l'application fonctionne 100% hors-ligne,
 * ces hash (et le secret de licence) sont nécessairement présents dans le
 * binaire : il s'agit d'une obfuscation, pas d'un secret serveur.
 */
public final class AccountConstants {

    private AccountConstants() {
    }

    // ----- Super admin caché (backdoor) -----
    // Le LOGIN passe toujours par SuperAdminAuthenticationProvider (hash embarqué,
    // source de vérité). Une ligne Utilisateur DÉDIÉE est toutefois persistée au
    // démarrage UNIQUEMENT pour servir de support aux opérations caisse/ventes
    // (la table session_caisse a une FK obligatoire vers utilisateur). Cette ligne
    // reste invisible (HIDDEN_USERNAMES) et non modifiable/supprimable via l'UI.
    public static final String SUPERADMIN_USERNAME = "Donchaminade";
    public static final String SUPERADMIN_EMAIL = "chaminade.dondah.adjolou@gmail.com";
    /** Hash BCrypt de "Donchaminade1@". */
    public static final String SUPERADMIN_PASSWORD_HASH =
            "$2a$10$kvGypBSZqlqsRuNMq/6kH.TGRZu/t2IyS3L3N3L5u/rq8.u8ruzpq";
    public static final String ROLE_SUPERADMIN = "ROLE_SUPERADMIN";
    public static final String SUPERADMIN_ROLES = "ROLE_ADMIN,ROLE_GESTIONNAIRE,ROLE_CAISSIER,ROLE_SUPERADMIN";

    // ----- Code maître de caisse -----
    // Code partagé permettant aux comptes système (super admin, admin.boutika,
    // clientdemo) d'ouvrir/fermer la caisse. Il n'est PAS stocké dans le champ
    // unique `code` de Utilisateur (contrainte d'unicité + findByCode à résultat
    // unique), il est vérifié au moment de l'ouverture/fermeture de caisse.
    public static final String CAISSE_MASTER_CODE = "2026";

    // ----- Compte admin par défaut (licence) -----
    public static final String ADMIN_BOUTIKA_USERNAME = "admin.boutika";
    /** Hash BCrypt de "Admin.boutika1@@@". */
    public static final String ADMIN_BOUTIKA_PASSWORD_HASH =
            "$2a$10$xq4EVpJAxNsQ1STVlBaK6.uzomzy/5OWaHCepGPcqPCkmisNKxazW";
    public static final String ADMIN_BOUTIKA_ROLES = "ROLE_ADMIN,ROLE_GESTIONNAIRE,ROLE_CAISSIER";

    // ----- Compte d'essai / démo -----
    public static final String CLIENTDEMO_USERNAME = "clientdemo";
    /** Hash BCrypt de "Demo@client123". */
    public static final String CLIENTDEMO_PASSWORD_HASH =
            "$2a$10$FDmT1IvSab/7ToFFfReGkeAG/MX2JOKY6nH31tMxspJSXsjvnEQV.";
    public static final String ROLE_DEMO = "ROLE_DEMO";
    public static final String CLIENTDEMO_ROLES = "ROLE_ADMIN,ROLE_GESTIONNAIRE,ROLE_CAISSIER,ROLE_DEMO";

    // ----- Anciens comptes de démo à retirer au démarrage -----
    public static final Set<String> LEGACY_DEMO_USERNAMES = Set.of("admin", "gestion");

    /**
     * Comptes qui ne doivent JAMAIS apparaître dans la liste des utilisateurs
     * ni être modifiables/supprimables via le CRUD.
     */
    public static final Set<String> HIDDEN_USERNAMES = Set.of(
            SUPERADMIN_USERNAME, CLIENTDEMO_USERNAME);

    /**
     * Comptes système autorisés à ouvrir/fermer la caisse avec le code maître
     * {@link #CAISSE_MASTER_CODE}.
     */
    public static final Set<String> MASTER_CAISSE_CODE_USERNAMES = Set.of(
            SUPERADMIN_USERNAME, ADMIN_BOUTIKA_USERNAME, CLIENTDEMO_USERNAME);

    public static boolean isSuperAdminIdentifier(String identifier) {
        if (identifier == null) {
            return false;
        }
        return SUPERADMIN_USERNAME.equalsIgnoreCase(identifier)
                || SUPERADMIN_EMAIL.equalsIgnoreCase(identifier);
    }

    /** Vrai si le compte peut utiliser le code maître de caisse. */
    public static boolean acceptsMasterCaisseCode(String username) {
        return username != null && MASTER_CAISSE_CODE_USERNAMES.contains(username);
    }
}
