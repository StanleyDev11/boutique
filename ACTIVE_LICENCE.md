# Activation de la licence — guide vendeur & client

L'application fonctionne en **essai gratuit de 14 jours**, puis nécessite une
**licence à vie** (paiement unique). L'activation est **100 % hors-ligne** :
la clé est calculée en **HMAC-SHA256** à partir d'un identifiant unique au poste,
aucune connexion Internet ni serveur de licence n'est requise.

> Voir aussi : **[README](README.md)** (installation, EXE) et
> **[Guide administrateur](GUIDE_ADMIN.md)** (comptes, caisse, paramètres).

---

## 1. Principe

- À la **première ouverture de `/login`**, l'application génère et stocke :
  - un **identifiant d'installation** (`install_id`) — un UUID **unique à ce poste**,
  - une **date de début d'essai** (`install_date`).
- L'essai dure **14 jours**.
- La **clé d'activation** est calculée ainsi :
  `clé = Base32( HMAC-SHA256( SECRET, install_id normalisé ) )`, tronquée à
  **20 caractères** présentés en **5 groupes de 4** (alphabet Base32 : `A–Z`, `2–7`).
- Le **même `SECRET`** est embarqué dans l'application **et** dans l'outil de
  génération de clés → la clé produite par le vendeur est acceptée hors-ligne
  par l'app du client.
- **Où est stocké l'état ?** Dans la **base H2 du poste client** :
  `%USERPROFILE%\.boutika\data\boutique.mv.db` (table `parametre`, clé/valeur).

### Paramètres stockés en base

| Clé (paramètre)        | Rôle                                            | Valeurs                          |
| ---------------------- | ----------------------------------------------- | -------------------------------- |
| `license.install_id`   | Identifiant unique du poste (UUID)              | ex. `a1b2c3d4-...`               |
| `license.install_date` | Début de l'essai (epoch millisecondes)          | ex. `1737500000000`              |
| `license.activated`    | Licence activée ou non                          | `true` / `false` (défaut `false`)|
| `license.key`          | Clé saisie (pour information)                    | la clé, ou `MANUAL-SUPERADMIN`   |

---

## 2. Routes / URL du système de licence

| Route                     | Méthode | Qui          | Ce que ça fait                                                                 |
| ------------------------- | ------- | ------------ | ------------------------------------------------------------------------------ |
| `/login`                  | GET/POST| Tous         | Pendant l'essai, `admin.boutika` est **verrouillé** (message via `/login?locked`). |
| `/license/expired`        | GET     | Client       | Écran « Période d'essai expirée » : affiche l'**identifiant** + **champ clé**. |
| `/license/activate`       | POST    | Client       | Valide la clé saisie ; si OK → `/login?activated`, sinon → `/license/expired?invalidkey`. |
| `/license/qr`             | GET     | Client       | Renvoie le **QR code PNG** (ZXing, hors-ligne) de l'URL WhatsApp pré-remplie. |
| `/license/manage`         | GET     | Super admin  | Affiche l'identifiant, la **clé attendue**, l'état et les jours restants.       |
| `/license/admin/activate` | POST    | Super admin  | **Déblocage manuel** de la licence, sans clé.                                  |

---

## 3. Côté CLIENT — récupérer l'identifiant et saisir la clé

1. **Pendant l'essai**, le client se connecte avec le **compte démo**
   (`clientdemo`). Le compte `admin.boutika` reste verrouillé jusqu'à l'activation.
2. **À l'expiration** des 14 jours, la tentative de connexion échoue et
   l'application **redirige automatiquement vers `/license/expired`**
   (page aussi accessible directement, sans être connecté).
3. Sur l'écran **« Période d'essai expirée »** :
   - L'encart **« Identifiant d'installation »** affiche l'identifiant du poste
     (ex. `A1B2-C3D4-…`). **C'est ce que le client envoie au vendeur.**
   - Canaux de contact : **boutons WhatsApp** intégrés
     (`wa.me/22899181626` et `wa.me/22892595661` — tél. +228 99 18 16 26 / +228 92 59 56 61).
   - Le client colle la clé reçue dans le champ **« Clé d'activation »**
     (`name="key"`), puis clique sur **« Activer la licence »**
     (envoi en **POST `/license/activate`**).
   - Clé incorrecte → retour sur la page avec le bandeau rouge
     « Clé d'activation invalide ».

---

## 3 bis. Plans & tarifs — page d'achat (`/license/expired`)

La page d'expiration fait aussi office de **page de tarifs**. Le modèle est
**« 1 licence = 1 poste »** : chaque PC a son propre identifiant et sa propre clé.
Les plans multi-PC sont **commerciaux/informatifs** (bon prix + orientation vers
le contact) ; l'activation reste **par poste** via le champ clé.

### Grille tarifaire (FCFA) — prix par PC dégressif

| Plan                                   | Postes   | Prix / PC        | Total                         |
| -------------------------------------- | -------- | ---------------- | ----------------------------- |
| **Standard** *(sélectionné par défaut)*| 1 PC     | 300 000          | 300 000                       |
| 2 PC                                   | 2        | 275 000          | 550 000                       |
| 3 PC                                   | 3        | 225 000          | 675 000                       |
| 4 PC et +                              | 4+       | 200 000          | 200 000 × nb (ex. 4 = 800 000)|
| Supermarché *(multi-caisses, partagé)* | plusieurs| **Prix à discuter** | —                          |

- Le plan **4 PC et +** propose un champ **nombre de postes** (min. 4) qui
  recalcule le total en direct (`total = nb × 200 000`).
- Le plan **Supermarché** n'affiche **aucun prix fixe** (« Prix à discuter »).
- Changer de plan/quantité met à jour **en direct** le total, le message
  WhatsApp pré-rempli **et** le QR code.

### Paiement & contact (hors-app)

Le paiement se règle **hors application** avec le vendeur, qui envoie ensuite la
clé. Aucune passerelle de paiement en ligne. Deux moyens de contact sur la page :

1. **Bouton « Contacter sur WhatsApp »** : ouvre
   `https://wa.me/<numero>?text=<message pré-rempli>` (nécessite Internet sur ce PC).
2. **QR code hors-ligne** : encode la **même URL** `wa.me/...?text=...`. Généré
   **côté serveur avec ZXing** (endpoint `GET /license/qr?text=...`, PNG),
   **sans aucun appel réseau**. Le client le scanne avec son téléphone → WhatsApp
   s'ouvre sur le tél. avec le message pré-rempli, même si le PC de caisse n'a pas
   Internet.

**Message pré-rempli** (url-encodé) :

- Plans chiffrés :
  `Bonjour, je suis intéressé par le plan {NOM} ({nb} PC – {TOTAL} FCFA). Mon identifiant d'installation : {INSTALL_ID}.`
- Supermarché :
  `Bonjour, je suis intéressé par le plan Supermarché (multi-caisses). Mon identifiant d'installation : {INSTALL_ID}.`

> Le QR et le bouton WhatsApp sont un **canal de commande** : ils n'activent pas
> la licence. L'activation se fait toujours en collant la clé reçue dans le champ
> **« Clé d'activation »** (POST `/license/activate`).

---

## 4. Côté VENDEUR — générer la clé

Depuis la racine du projet, projet **compilé** (dossier `target\classes` présent) :

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-21'
# (si le projet n'est pas déjà compilé) : .\mvnw.cmd -q compile
& "$env:JAVA_HOME\bin\java.exe" -cp target\classes com.example.boutique.tools.LicenseKeyGenerator "IDENTIFIANT-DU-CLIENT"
```

**Exemple de sortie réelle** (avec l'identifiant d'exemple
`A1B2-C3D4-E5F6-7890-A1B2-C3D4-E5F6-7890`) :

```text
Identifiant (normalisé) : A1B2-C3D4-E5F6-7890-A1B2-C3D4-E5F6-7890
Clé d'activation        : GZS3-654A-LWOC-CFEY-TKIU
```

- **Format de la clé** : `XXXX-XXXX-XXXX-XXXX-XXXX` (5 groupes de 4, Base32 `A–Z`/`2–7`).
- **Déterministe et tolérant** : le même identifiant, avec ou sans tirets, en
  majuscules ou minuscules, produit **toujours la même clé** (normalisation interne).
  Le client peut donc recopier l'identifiant tel qu'affiché.
- Copiez la ligne **« Clé d'activation »** et transmettez-la au client.

> **Variante (fat jar)** — si vous ne conservez pas `target\classes`, vous pouvez
> lancer le générateur depuis le jar packagé :
>
> ```powershell
> & "$env:JAVA_HOME\bin\java.exe" -cp "target\boutique-1.0.0-SNAPSHOT.jar" `
>   -Dloader.main=com.example.boutique.tools.LicenseKeyGenerator `
>   org.springframework.boot.loader.launch.PropertiesLauncher "IDENTIFIANT-DU-CLIENT"
> ```
>
> La méthode `target\classes` ci-dessus reste la plus simple (et celle testée).

---

## 5. Alternative SUPER ADMIN — activation manuelle (secours)

Le super administrateur peut activer la licence **sans clé** :

1. Ouvrir **`/license/manage`** (« Gestion de la licence »). La page affiche
   l'identifiant d'installation **et** la **clé attendue** (calculée localement),
   l'état de la licence et les jours d'essai restants.
2. Cliquer sur **« Débloquer la licence maintenant »** → **POST `/license/admin/activate`** :
   la licence passe à `activated=true` et `license.key = "MANUAL-SUPERADMIN"`.
   Redirection vers `/license/manage?activated` (bandeau vert de confirmation).

---

## 6. Après activation (définitif, à vie sur ce poste)

| Compte           | Pendant l'essai (non activé)                    | Après activation                          |
| ---------------- | ----------------------------------------------- | ----------------------------------------- |
| `admin.boutika`  | **Verrouillé** (« disponible après activation »)| **Débloqué** — accès complet              |
| `clientdemo`     | Autorisé (compte de démonstration)              | **Neutralisé/désactivé**                  |
| Super admin      | Toujours autorisé (provider dédié)              | Toujours autorisé                         |

Autres effets :

- Plus de **compte à rebours d'essai** (l'app est débloquée en permanence).
- L'activation est **permanente** : stockée en base (`license.activated=true`),
  aucune ré-expiration.

---

## 7. Pièges & limites

- **Une clé est liée à UN poste** : elle dépend de l'`install_id`, unique à chaque
  installation. Elle **n'est pas transférable** vers un autre PC (autre identifiant
  → autre clé). Une réinstallation qui régénère l'`install_id` nécessite une nouvelle clé.
- **Sécurité par obfuscation, pas cryptographique** : l'app étant hors-ligne, le
  `SECRET` HMAC est **embarqué dans le binaire**. Quelqu'un qui décompile le `.jar`
  peut le retrouver et fabriquer des clés. Le seul générateur « officiel » reste
  l'outil du vendeur.
- **Ne jamais modifier le `SECRET`** après diffusion : toutes les clés déjà
  générées deviendraient invalides.
- L'état d'activation **survit à une désinstallation/réinstallation** de l'application
  tant que le dossier `%USERPROFILE%\.boutika` n'est pas supprimé (données conservées).
