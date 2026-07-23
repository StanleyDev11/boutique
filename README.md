# Boutique — Logiciel de caisse & gestion de boutique

Application web de **caisse et de gestion de boutique** (Spring Boot 3.2.2, Java 21,
Thymeleaf). Elle s'utilise dans le navigateur et peut être empaquetée en
**application desktop Windows** (`.exe`) **100 % hors-ligne** : runtime Java embarqué,
base de données locale, aucune connexion Internet requise.

---

## Sommaire

- [Présentation](#présentation)
- [Fonctionnalités clés](#fonctionnalités-clés)
- [Prérequis (développement)](#prérequis-développement)
- [Lancer en développement](#lancer-en-développement)
- [Générer l'installateur Windows (.exe)](#générer-linstallateur-windows-exe)
- [Comportement de l'application desktop](#comportement-de-lapplication-desktop)
- [Comptes par défaut](#comptes-par-défaut)
- [Documentation](#documentation)

---

## Présentation

- **Type** : logiciel de caisse / gestion de boutique (produits, stock, ventes,
  factures, rapports, utilisateurs).
- **Stack** : Spring Boot + Thymeleaf, sécurité Spring Security, JPA/Hibernate,
  migrations Flyway (profil desktop).
- **Deux modes** :
  - **Développement** : profil par défaut, base **MySQL** (XAMPP typiquement).
  - **Desktop (.exe)** : profil `desktop`, base **H2 fichier locale**, hors-ligne.
- **UI** : interfaces responsives (mobile / tablette / desktop), **CSS & JS bundlés
  localement** dans `/vendor` (aucune ressource CDN → fonctionne sans Internet).

## Fonctionnalités clés

- **Caisse** : ouverture/fermeture de session, ventes, code caissier de traçabilité.
- **Gestion** : produits, stock, factures d'achat, rapports (Excel/PDF), utilisateurs.
- **Personnalisation** : nom de la boutique, **logo** (par défaut `icon.png`),
  numéro **WhatsApp**, couleurs d'en-tête — configurables dans la page Paramètres.
- **Licence** : **essai gratuit de 14 jours** puis **licence à vie** (activation
  hors-ligne par clé HMAC), **page de tarifs** intégrée avec **bouton WhatsApp** et
  **QR code généré hors-ligne**.
- **Comptes** : compte admin par défaut, compte de démonstration, **super admin caché**.
- **Caisse — code maître `2026`** réservé aux comptes système.
- **Expérience desktop** :
  - **Instance unique** : relancer l'app alors qu'elle tourne déjà rouvre simplement
    le navigateur (pas de conflit de port).
  - **Icône dans la barre des tâches système** (menu « Ouvrir Boutique » / « Quitter »).
  - **Ouverture automatique du navigateur** au démarrage.

## Prérequis (développement)

- **JDK 21 obligatoire** (le build échoue en JDK 17). Réglez `JAVA_HOME` :

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-21'
```

- **Maven wrapper** fourni (`mvnw.cmd`) : pas besoin d'installer Maven.
- **Base de données** :
  - Dev (profil par défaut) : **MySQL** (voir `src/main/resources/application.yml`),
    Flyway **désactivé**, `ddl-auto=update`.
  - Desktop (profil `desktop`) : **H2 fichier**, Flyway **activé**
    (voir `src/main/resources/application-desktop.yml`).
- **Port `8085`** doit être libre.

## Lancer en développement

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-21'
.\mvnw.cmd spring-boot:run
```

- Profil par défaut (MySQL). L'application écoute sur <http://localhost:8085>.
- Pour simuler le mode desktop (H2 local) :

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=desktop"
```

## Générer l'installateur Windows (.exe)

L'installateur est produit avec **Inno Setup** + un **runtime Java embarqué (jlink)**.
Les raccourcis lancent l'application **directement via `javaw.exe`** — ce qui évite le
bug « *Failed to launch JVM* » de l'ancien lanceur natif jpackage. **Aucun Java n'est
requis sur le PC client** (le runtime est inclus).

### Prérequis

- **JDK 21** (`JAVA_HOME` réglé comme ci-dessus).
- **Inno Setup** (gratuit) : à télécharger depuis <https://jrsoftware.org/isdl.php>
  (dernière version stable, ex. Inno Setup 6.x).
- **Icône** : `icon.ico` (multi-résolutions) et `icon.png` sont à la racine du projet.

### Étape 1 — Construire le jar + le payload

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-21'
.\mvnw.cmd clean package -DskipTests
powershell -ExecutionPolicy Bypass -File installer\build-payload.ps1
```

Le script `build-payload.ps1` (re)construit le fat jar, génère un **runtime jlink**
(contenant `java.exe` **et** `javaw.exe`), puis assemble `installer\payload\` :

- `runtime\` — runtime Java embarqué (`runtime\bin\javaw.exe`),
- `boutique-1.0.0-SNAPSHOT.jar` — le fat jar Spring Boot,
- `icon.ico` — l'icône de l'application.

> Options : `-SkipBuild` (réutiliser le jar de `target\`), `-JavaHome '<chemin>'`.

### Étape 2 — Compiler dans Inno Setup

1. Ouvrez `installer\boutique.iss` dans **Inno Setup Compiler**.
2. Menu **Build → Compile** (ou le bouton ▶).
3. L'installateur est produit dans : `installer\Output\Boutique-Setup-1.0.0.exe`.

> En ligne de commande (si Inno Setup est installé) :
>
> ```powershell
> & "C:\Program Files (x86)\Inno Setup 6\ISCC.exe" installer\boutique.iss
> ```

### Étape 3 — Ce que fait l'installateur

- Installe **par utilisateur, sans droits admin**, dans `%LOCALAPPDATA%\Programs\Boutique`.
- Crée un **raccourci menu Démarrer** « Boutique » et, en option, un **raccourci Bureau**.
- Les raccourcis exécutent :
  `runtime\bin\javaw.exe -Dspring.profiles.active=desktop -jar "…\boutique-1.0.0-SNAPSHOT.jar"`.
- Propose de **lancer l'app** en fin d'installation ; se **désinstalle** via
  *Paramètres → Applications*.

> **SmartScreen** : l'installateur n'étant pas signé, un avertissement « Éditeur
> inconnu » peut apparaître (*Informations complémentaires → Exécuter quand même*).
> La signature de code (Authenticode) est une amélioration future.

### Nouvelle version

Relancez l'**étape 1** (`build-payload.ps1`) puis recompilez le `.iss`. Pensez à mettre
à jour `MyAppVersion` (et le nom du jar) dans `installer\boutique.iss` si la version change.

## Comportement de l'application desktop

- **Données** : base H2 fichier dans
  `%USERPROFILE%\.boutika\data\boutique.mv.db` — **persistante** et **conservée à la
  désinstallation**.
- **Migrations** : gérées par **Flyway** (profil `desktop`). Base neuve → baseline `V1` ;
  base existante → adoptée via `baseline-on-migrate`, sans recréation.
- **Instance unique** : si l'app tourne déjà (port `8085` occupé), relancer le raccourci
  **rouvre le navigateur** sur l'instance existante puis se termine (pas de conflit).
- **Barre des tâches système** : une icône permet « **Ouvrir Boutique** » et « **Quitter** »
  (Quitter arrête proprement le serveur et libère le port).
- **Réinitialiser** l'application (repartir d'une base neuve) :

```powershell
Remove-Item -Recurse -Force "$env:USERPROFILE\.boutika"
```

## Comptes par défaut

L'application crée automatiquement des comptes système au premier démarrage
(compte admin par défaut, compte de démonstration, super admin caché). Le compte
admin par défaut est **verrouillé pendant l'essai** et **débloqué après activation**
de la licence.

> Détail complet des comptes, rôles, codes caisse et gestion quotidienne :
> voir **[Guide administrateur](GUIDE_ADMIN.md)**.

## Documentation

- **[Guide administrateur](GUIDE_ADMIN.md)** — comptes & rôles, caissiers, code maître
  caisse, licence, paramètres, dépannage.
- **[Activation de la licence](ACTIVE_LICENCE.md)** — principe HMAC, génération de la clé
  (côté vendeur), saisie côté client, plans & tarifs, activation super admin.
