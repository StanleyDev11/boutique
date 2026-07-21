package com.example.boutique.tools;

import com.example.boutique.security.LicenseKeyUtil;

/**
 * GÉNÉRATEUR DE CLÉS DE LICENCE — outil du propriétaire, à lancer À LA MAIN.
 *
 * Il n'est PAS branché dans le flux de l'application (aucune annotation Spring,
 * jamais instancié au démarrage). Il utilise le MÊME secret embarqué que
 * l'application via {@link LicenseKeyUtil}, donc la clé produite est acceptée
 * par l'app installée chez le client.
 *
 * ------------------------------------------------------------------------
 * MODE D'EMPLOI (Windows / PowerShell), depuis la racine du projet :
 *
 *   1) Compiler le projet (une fois) :
 *        $env:JAVA_HOME='C:\Program Files\Java\jdk-21'
 *        .\mvnw.cmd -q compile
 *
 *   2) Lancer le générateur avec l'Identifiant d'installation communiqué par
 *      le client (celui affiché sur la page "Période d'essai expirée") :
 *        java -cp target\classes com.example.boutique.tools.LicenseKeyGenerator "XXXX-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX"
 *
 *   3) Copier la clé affichée et la donner au client, qui la saisit sur la
 *      page d'activation. L'app se débloque définitivement (à vie).
 *
 * Les tirets/espaces dans l'identifiant sont ignorés (normalisation interne),
 * le client peut donc recopier l'identifiant tel qu'affiché.
 * ------------------------------------------------------------------------
 */
public class LicenseKeyGenerator {

    public static void main(String[] args) {
        if (args.length < 1 || args[0].isBlank()) {
            System.out.println("Usage : java -cp target\\classes com.example.boutique.tools.LicenseKeyGenerator <identifiant-installation>");
            System.out.println("Exemple : java -cp target\\classes com.example.boutique.tools.LicenseKeyGenerator \"A1B2-C3D4-E5F6-7890-A1B2-C3D4-E5F6-7890\"");
            return;
        }

        String installId = args[0];
        String key = LicenseKeyUtil.computeKey(installId);

        System.out.println("Identifiant (normalisé) : " + LicenseKeyUtil.group(installId));
        System.out.println("Clé d'activation        : " + key);
    }
}
