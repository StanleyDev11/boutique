# Guide du Système d'Abonnement et de l'Interface Super Admin

Ce document décrit l'architecture et l'utilisation du système d'abonnement et de l'interface Super Admin.

## 1. Architecture du Système d'Abonnement

Le système est basé sur trois concepts principaux : les **Features** (fonctionnalités), les **Plans** et les **Licences**.

### 1.1. `Feature.java`

C'est une énumération qui liste toutes les fonctionnalités granulaires de l'application qui peuvent être soumises à une licence.

**Fichier:** `src/main/java/com/example/boutique/model/Feature.java`

Exemples de fonctionnalités :
- `GESTION_PRODUITS`
- `GESTION_PERSONNEL`
- `PARAMETRES`
- `RAPPORTS_AVANCES`

### 1.2. `Plan.java`

C'est une entité qui représente un plan d'abonnement ou une licence. Un plan est défini par :
- Un **nom** (ex: "BASIC", "PRO", "LICENCE_UNIQUE")
- Un **prix**
- Une **liste de `Feature`** incluses dans le plan.

**Fichier:** `src/main/java/com/example/boutique/model/Plan.java`

### 1.3. `Licence.java`

C'est une entité qui lie un `Utilisateur` à un `Plan`. Elle contient :
- L'**utilisateur** concerné.
- Le **plan** souscrit.
- Une **date de début**.
- Une **date de fin** (peut être nulle pour les licences à vie).
- Un **statut** (`ACTIVE`, `EXPIREE`, `ANNULEE`).

**Fichier:** `src/main/java/com/example/boutique/model/Licence.java`

## 2. Contrôle d'Accès aux Fonctionnalités (Feature Gating)

Pour restreindre l'accès à une fonctionnalité en fonction de l'abonnement de l'utilisateur, nous utilisons une annotation personnalisée : `@RequiresFeature`.

### Comment l'utiliser ?

Pour protéger une méthode dans un contrôleur (ou un service), il suffit d'ajouter l'annotation au-dessus de la déclaration de la méthode.

**Exemple :** Protéger l'accès à la page des paramètres.

```java
// Dans src/main/java/com/example/boutique/controller/ParametreController.java

import com.example.boutique.aspect.RequiresFeature;
import com.example.boutique.model.Feature;

// ...

@GetMapping
@RequiresFeature(Feature.PARAMETRES) // <-- Annotation de contrôle d'accès
public String showParametresPage(Model model) {
    // ...
}
```

Si un utilisateur sans la `Feature.PARAMETRES` dans son plan essaie d'accéder à cette méthode, il sera redirigé vers une page d'erreur "Accès non autorisé".

## 3. Interface Super Admin

Une interface de gestion complète est disponible pour l'utilisateur ayant le rôle `ROLE_SUPER_ADMIN`.

### 3.1. Accès

- **URL :** `/superadmin`
- **Utilisateur par défaut :** `admin`
- **Mot de passe par défaut :** `password`

Le tableau de bord principal donne accès à deux sections : "Gérer les Plans" et "Gérer les Licences".

### 3.2. Gérer les Plans

- **URL :** `/superadmin/plans`
- **Fonctionnalités :**
    - **Lister :** Affiche tous les plans existants, leur prix et leurs fonctionnalités.
    - **Créer :** Permet de créer un nouveau plan en définissant un nom, un prix et en cochant les fonctionnalités à inclure.
    - **Modifier :** Permet de mettre à jour un plan existant.
    - **Supprimer :** Supprime un plan de la base de données.

### 3.3. Gérer les Licences

- **URL :** `/superadmin/licences`
- **Fonctionnalités :**
    - **Lister :** Affiche toutes les licences attribuées aux utilisateurs, avec leur plan, statut et dates de validité.
    - **Assigner :** Permet de créer une nouvelle licence en liant un utilisateur à un plan, avec des dates de début/fin et un statut.
    - **Modifier :** Permet de changer le plan, les dates ou le statut d'une licence existante.

## 4. Initialisation des Données

Pour faciliter les tests et le déploiement initial, des données par défaut sont créées au démarrage de l'application.

**Fichier:** `src/main/java/com/example/boutique/config/DataInitializer.java`

Ce fichier est responsable de la création :
- Des utilisateurs par défaut (`admin`, `gestion`, `testuser`).
- Des rôles, y compris le `ROLE_SUPER_ADMIN` pour l'utilisateur `admin`.
- Des plans par défaut (LICENCE_UNIQUE, BASIC, PRO).
- Des licences initiales pour les utilisateurs `admin` et `testuser`.

Vous pouvez modifier ce fichier pour ajuster les données créées au démarrage.
