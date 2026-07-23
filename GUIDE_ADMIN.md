# Guide administrateur — Boutique

Ce guide explique comment **administrer l'application** au quotidien : comptes et
rôles, caissiers et codes de caisse, ouverture/fermeture de caisse, licence,
paramètres configurables, dépannage et sécurité.

> Voir aussi : **[README](README.md)** (installation, EXE, mode desktop) et
> **[Activation de la licence](ACTIVE_LICENCE.md)** (génération de clé détaillée).

---

## Sommaire

- [1. Comptes & rôles](#1-comptes--rôles)
- [2. Gestion des utilisateurs / caissiers](#2-gestion-des-utilisateurs--caissiers)
- [3. Ouverture / fermeture de caisse](#3-ouverture--fermeture-de-caisse)
- [4. Licence & tarifs](#4-licence--tarifs)
- [5. Paramètres configurables](#5-paramètres-configurables)
- [6. Réinitialisation & dépannage](#6-réinitialisation--dépannage)
- [7. Sécurité](#7-sécurité)

---

## 1. Comptes & rôles

L'application crée automatiquement trois comptes système au premier démarrage. Les
mots de passe ci-dessous sont ceux **par défaut** (à communiquer/changer selon vos règles).

| Compte | Identifiant | Mot de passe par défaut | Rôles | État pendant l'essai | Après activation |
| --- | --- | --- | --- | --- | --- |
| **Admin principal** | `admin.boutika` | `Admin.boutika1@@@` | ADMIN, GESTIONNAIRE, CAISSIER | **Verrouillé** (message « disponible après activation ») | **Débloqué** — accès complet |
| **Démo / essai** | `clientdemo` | `Demo@client123` | ADMIN, GESTIONNAIRE, CAISSIER, **DEMO** | Autorisé (pour tester pendant 14 j) | **Neutralisé/désactivé** |
| **Super admin (caché)** | `Donchaminade` *(ou* `chaminade.dondah.adjolou@gmail.com`*)* | *(confidentiel, non affiché)* | ADMIN, GESTIONNAIRE, CAISSIER, **SUPERADMIN** | Toujours autorisé | Toujours autorisé |

- Le **super admin** est **invisible** dans la liste des utilisateurs et **non
  modifiable/supprimable** via l'interface. Il n'est jamais soumis aux règles de licence.
- Le compte **`clientdemo`** est caché lui aussi et **restreint** : il ne peut pas gérer
  les utilisateurs (rôle DEMO exclu de `/utilisateurs/**`).

### Ce que permet chaque rôle (résumé)

| Rôle | Peut notamment |
| --- | --- |
| **ADMIN** | Tout : produits, stock, ventes, rapports, **gestion des utilisateurs**, personnel, tableau de bord. |
| **GESTIONNAIRE** | Produits, stock, création/édition, rapports, gestion des caisses (pas la suppression de produits). |
| **CAISSIER** | Caisse (ventes), historique des ventes. |
| **DEMO** | Comme ADMIN mais **sans la gestion des utilisateurs** ; sert à la démonstration pendant l'essai. |
| **SUPERADMIN** | Accès total + routes réservées d'administration de licence (`/license/manage`, `/license/admin/**`). |

## 2. Gestion des utilisateurs / caissiers

L'admin crée les comptes du personnel depuis la page **Utilisateurs** (`/utilisateurs`,
réservée à ADMIN, hors compte DEMO).

Il existe **deux notions de code** à ne pas confondre :

| Notion | À quoi ça sert | Qui | Impact sur les ventes |
| --- | --- | --- | --- |
| **Code caissier (personnel)** | Identifier le caissier à l'ouverture/fermeture de caisse — **traçabilité** des ventes | Chaque caissier a **son** code, **unique** | Les ventes sont **attribuées** à ce caissier |
| **Code maître de caisse `2026`** | Permettre aux **comptes système** d'ouvrir/fermer la caisse sans code personnel | Uniquement `Donchaminade`, `admin.boutika`, `clientdemo` | **N'attribue pas** de ventes à un caissier nominatif |

- Le **code caissier est unique** par utilisateur (recherche par `findByCode`) : deux
  caissiers ne peuvent pas partager le même code — c'est ce qui garantit la traçabilité.
- Le **code maître `2026`** est un raccourci pour les 3 comptes système ; il ne remplace
  pas un code caissier nominatif et ne sert qu'aux opérations d'ouverture/fermeture.

## 3. Ouverture / fermeture de caisse

- À l'**ouverture** comme à la **fermeture** de la caisse, l'utilisateur saisit un **code**.
- Le code est validé ainsi :
  - soit c'est **le code personnel** de l'utilisateur connecté,
  - soit c'est le **code maître `2026`** **et** le compte fait partie des comptes système
    autorisés (`Donchaminade`, `admin.boutika`, `clientdemo`).
- Un caissier normal ouvre donc la caisse avec **son** code ; les comptes système peuvent
  utiliser `2026`.

## 4. Licence & tarifs

- **Essai** : 14 jours, lié à un **identifiant d'installation** unique au poste.
- **Activation** (à vie, hors-ligne) — deux voies :
  - **Client** : sur `/license/expired`, il communique son identifiant, reçoit une **clé**
    et la saisit dans le champ « Clé d'activation » (POST `/license/activate`).
  - **Super admin** : activation manuelle **sans clé** via `/license/manage` →
    bouton « Débloquer » (POST `/license/admin/activate`).

### Plans & tarifs (FCFA)

| Plan | Postes | Prix / PC | Total |
| --- | --- | --- | --- |
| **Standard** *(par défaut)* | 1 PC | 300 000 | 300 000 |
| 2 PC | 2 | 275 000 | 550 000 |
| 3 PC | 3 | 225 000 | 675 000 |
| 4 PC et + | 4+ | 200 000 | 200 000 × nb |
| Supermarché *(multi-caisses, données partagées)* | plusieurs | **Prix à discuter** | — |

- Le modèle est **1 licence = 1 poste** : chaque PC a son identifiant et sa clé. Les plans
  multi-PC sont **commerciaux** (le bon prix s'affiche et oriente vers le contact).
- La page d'expiration propose un **bouton « Contacter sur WhatsApp »** et un **QR code
  hors-ligne** (à scanner avec un téléphone) : tous deux encodent un message pré-rempli
  incluant l'identifiant d'installation et le plan choisi. Le paiement se fait avec le
  vendeur, qui envoie ensuite la clé.
- Après activation, le lien « Activer une licence » **disparaît** du login et
  `/license/expired` **redirige** vers `/login`.

> Génération de la clé côté vendeur, format et détails : voir
> **[Activation de la licence](ACTIVE_LICENCE.md)**.

## 5. Paramètres configurables

Depuis la page **Paramètres**, l'admin peut personnaliser (stocké en base, clé/valeur) :

| Paramètre (clé) | Rôle | Valeur par défaut |
| --- | --- | --- |
| `boutique.nom` | Nom de la boutique (titre, en-tête, login) | `Ma boutique` |
| `boutique.logo` | Logo affiché partout | `/icon.png` |
| `boutique.whatsapp` | Numéro WhatsApp de contact | `22899181626` |
| `boutique.adresse` | Adresse affichée | *(adresse par défaut)* |
| `boutique.telephone` | Téléphone affiché | *(numéro par défaut)* |
| `tailwind.header.background.color` | Couleur de fond de l'en-tête | `#1F2937` |
| `tailwind.header.text.color` | Couleur du texte de l'en-tête | `#D1D5DB` |
| `seuil_stock_bas` | Seuil d'alerte de stock bas | `10` |
| `jours_avant_peremption` | Alerte de péremption (jours) | `30` |

> **Important** : une valeur par défaut ne s'applique **que si le paramètre n'est pas
> encore enregistré** (bases neuves / nouvelles installations). Les installations
> existantes conservent la valeur déjà enregistrée (ex. un logo déjà choisi ne change pas).

Le logo par défaut `/icon.png` est servi depuis les ressources statiques ; le fichier
`icon.png` (devanture de boutique) est aussi utilisé pour l'icône de l'installateur.

## 6. Réinitialisation & dépannage

- **Repartir d'une base neuve** (⚠️ supprime toutes les données locales) :

```powershell
Remove-Item -Recurse -Force "$env:USERPROFILE\.boutika"
```

- **Le navigateur ne s'ouvre pas automatiquement** : ouvrez manuellement
  <http://localhost:8085>. (Au démarrage, l'app tente `rundll32`, puis `cmd start`, puis
  l'API Java ; en dernier recours, saisissez l'URL vous-même.)
- **Relancer alors que l'app tourne déjà** : le raccourci **rouvre simplement le
  navigateur** (instance unique) — aucun conflit de port.
- **Quitter proprement** : clic droit sur l'**icône de la barre des tâches système** →
  « **Quitter** » (arrête le serveur et libère le port `8085`).
- **Port `8085` occupé** : quittez l'instance via l'icône système, ou terminez le
  processus `javaw`/`java` résiduel.

## 7. Sécurité

- Les routes d'**administration de licence** sont **réservées au super admin** :
  `/license/manage` (GET) et `/license/admin/**` (POST) exigent le rôle `SUPERADMIN`
  (double protection : règle Spring Security **et** vérification dans le contrôleur).
- Les routes **publiques** liées à la licence restent accessibles sans connexion :
  `/license/expired`, `/license/activate` (saisie de clé) et `/license/qr` (QR code).
- La **gestion des utilisateurs** (`/utilisateurs/**`) est réservée à **ADMIN** et
  **interdite au compte DEMO**.
- Le **super admin** et le compte **démo** sont cachés et non modifiables via l'interface.
- L'activation de licence repose sur un secret HMAC **embarqué** (obfuscation, pas un
  secret serveur) : ne jamais le modifier après diffusion (invaliderait toutes les clés).
