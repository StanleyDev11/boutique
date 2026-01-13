# Cahier des Charges - Application Mobile Boutique

## 1. Introduction

### 1.1. Contexte et Objectifs du Projet

*   **Contexte :** Le projet vise à développer une application mobile pour une boutique spécialisée dans la vente de pagnes et tissus de qualité. Cette application permettra aux clients de découvrir le catalogue des produits, de les visualiser en détail et de passer des commandes directement depuis leur smartphone. Elle s'appuiera sur les données gérées par l'application de gestion de stock existante de la boutique, qui servira de backend API.
*   **Objectifs :**
    *   **Principal :** Offrir une plateforme mobile conviviale pour la consultation des produits et la passation de commandes par les clients.
    *   **Secondaire :** Améliorer la visibilité des produits, moderniser l'expérience client et potentiellement augmenter le chiffre d'affaires.
    *   **Technique :** Développer une application mobile performante et intuitive, capable de s'intégrer de manière fluide avec le système de gestion de stock existant.

### 1.2. Portée de l'Application Mobile

*   **Inclus :**
    *   Consultation du catalogue de produits (par catégories, recherche, filtres).
    *   Visualisation détaillée des produits (images, descriptions, prix, disponibilité).
    *   Fonctionnalités de panier d'achat (ajout, modification, suppression d'articles).
    *   Processus de commande (informations client, adresse de livraison, choix de paiement).
    *   Gestion de compte client (création, connexion, modification de profil, historique des commandes).
*   **Exclu de cette version initiale :**
    *   Notifications push avancées (promotions personnalisées, rappels de panier).
    *   Système de fidélité intégré.
    *   Gestion des retours de produits via l'application.
    *   Chat en direct avec le service client.

### 1.3. Glossaire

*   **API (Application Programming Interface) :** Interface permettant à l'application mobile de communiquer avec le backend de gestion de stock.
*   **Backend API :** Partie serveur de l'application de gestion de stock exposant des services pour le mobile.
*   **UI (User Interface) :** Interface utilisateur de l'application mobile.
*   **UX (User Experience) :** Expérience utilisateur globale de l'application mobile.
*   **MVP (Minimum Viable Product) :** Version minimale de l'application avec les fonctionnalités essentielles.

## 2. Besoins Fonctionnels

Cette section détaille les actions que l'utilisateur pourra effectuer et les informations qu'il pourra consulter via l'application mobile.

### 2.1. Navigation et Consultation des Produits

*   **Catalogue Principal :**
    *   L'utilisateur doit pouvoir parcourir l'ensemble du catalogue de produits.
    *   Les produits doivent être organisés par catégories (ex: "Pagne", "Tissu Wax", "Tissu Bogolan").
*   **Recherche de Produits :**
    *   L'utilisateur doit pouvoir rechercher des produits par nom, description ou code (si pertinent).
    *   L'utilisateur doit pouvoir filtrer les résultats de recherche par prix, couleur, disponibilité.
*   **Détails du Produit :**
    *   En cliquant sur un produit, l'utilisateur doit accéder à une page dédiée affichant :
        *   Nom, prix.
        *   Description détaillée (matériaux, dimensions, entretien, histoire).
        *   **Galerie d'images haute résolution avec fonctionnalité de zoom.**
        *   Indicateur de disponibilité en stock.
        *   Bouton "Ajouter au panier".

### 2.2. Panier d'Achat

*   **Ajout au Panier :** L'utilisateur doit pouvoir ajouter un ou plusieurs produits au panier depuis la page de détails produit.
*   **Visualisation du Panier :** L'utilisateur doit pouvoir consulter à tout moment le contenu de son panier, avec :
    *   Liste des articles, quantités, prix unitaires et sous-totaux.
    *   Prix total du panier.
*   **Modification du Panier :**
    *   L'utilisateur doit pouvoir ajuster la quantité d'un article directement depuis le panier.
    *   L'utilisateur doit pouvoir supprimer un article du panier.

### 2.3. Processus de Commande

*   **Initiation de la Commande :** L'utilisateur doit pouvoir passer à l'étape de commande depuis le panier.
*   **Informations Client :**
    *   Si non connecté, l'utilisateur doit pouvoir s'inscrire ou se connecter.
    *   Les informations de profil du client connecté doivent être pré-remplies et modifiables.
*   **Adresse de Livraison :** L'utilisateur doit pouvoir sélectionner une adresse de livraison existante ou en ajouter une nouvelle.
*   **Méthodes de Paiement :** L'utilisateur doit pouvoir choisir parmi les méthodes de paiement configurées (ex: Paiement à la livraison, Mobile Money, Carte Bancaire - **détailler les options souhaitées**).
*   **Récapitulatif et Confirmation :**
    *   Affichage d'un récapitulatif final de la commande (produits, prix, livraison, total).
    *   Bouton de confirmation de commande.
*   **Confirmation de Commande :** Affichage d'un message de succès et d'un numéro de commande.

### 2.4. Gestion du Compte Client

*   **Inscription :** L'utilisateur doit pouvoir créer un compte avec une adresse e-mail / numéro de téléphone et un mot de passe.
*   **Connexion / Déconnexion :** L'utilisateur doit pouvoir se connecter et se déconnecter de son compte.
*   **Profil Utilisateur :**
    *   L'utilisateur doit pouvoir consulter et modifier ses informations personnelles (nom, prénom, e-mail, téléphone).
    *   L'utilisateur doit pouvoir gérer ses adresses de livraison enregistrées.
*   **Historique des Commandes :** L'utilisateur doit pouvoir accéder à la liste de toutes ses commandes passées, avec la possibilité de consulter les détails de chaque commande.
*   **Réinitialisation de Mot de Passe :** L'utilisateur doit pouvoir initier une procédure de réinitialisation de mot de passe en cas d'oubli.

## 3. Besoins Non-Fonctionnels

Ces exigences définissent la qualité et les contraintes techniques de l'application mobile.

### 3.1. Performance

*   L'application mobile doit charger le catalogue de produits initial en moins de [X] secondes sur une connexion [Y]G standard.
*   Le temps de réponse pour la navigation entre les pages (détails produit, panier) doit être inférieur à [Z] secondes.
*   Le processus de finalisation d'une commande doit être fluide et rapide.

### 3.2. Sécurité

*   Toutes les communications entre l'application mobile et le backend doivent être sécurisées via HTTPS.
*   Les données sensibles des utilisateurs (informations personnelles, paiement) doivent être protégées et stockées conformément aux meilleures pratiques de sécurité.
*   L'authentification et l'autorisation des utilisateurs doivent être robustes.

### 3.3. Ergonomie et Expérience Utilisateur (UI/UX)

*   **Design Visuel :** L'application doit présenter un design moderne, élégant et cohérent avec l'image de marque de la boutique, en mettant en valeur les motifs et les couleurs des pagnes et tissus.
*   **Navigation Intuitive :** La navigation doit être simple et logique, permettant à l'utilisateur de trouver facilement les informations et fonctionnalités.
*   **Fluidité :** L'expérience utilisateur doit être fluide et réactive, sans latence perceptible.
*   **Accessibilité :** Conception prenant en compte les principes d'accessibilité (ex: tailles de texte ajustables, contrastes suffisants).

### 3.4. Compatibilité

*   **Systèmes d'exploitation :** L'application doit être compatible avec les versions majeures d'iOS (à partir de [version]) et Android (à partir de [version]).
*   **Appareils :** Fonctionnement optimal sur une gamme d'appareils (smartphones) avec différentes tailles d'écran.
*   **Connexion Réseau :** L'application doit gérer les conditions de réseau fluctuantes et fournir des retours utilisateurs appropriés en cas de perte de connexion.

### 3.5. Maintenance et Évolutivité

*   Le code source doit être modulaire, bien structuré et documenté, facilitant la maintenance et l'ajout de futures fonctionnalités.
*   L'architecture doit permettre une évolution progressive de l'application.

### 3.6. Localisation

*   **Langue(s) :** L'application sera disponible en [Français / autres langues].
*   **Devise :** La devise affichée sera [XOF / autre].

## 4. Architecture Technique de l'Application Mobile (Haut niveau)

*   **Technologie :** Développement de l'application avec [**Flutter (Dart)** ou **React Native (JavaScript/TypeScript)**], sélectionné pour sa capacité à créer des applications multiplateformes performantes avec une excellente UI.
*   **Interaction Backend :** L'application mobile communiquera avec le Backend API existant (Spring Boot) via des appels RESTful sécurisés.
*   **Gestion de l'état :** Utilisation d'une solution de gestion de l'état adaptée au framework choisi (ex: Provider/Bloc pour Flutter, Redux/Context API pour React Native).

## 5. Contraintes et Hypothèses

### 5.1. Contraintes

*   **Délais :** La date de livraison souhaitée pour le MVP est le [date].
*   **Budget :** Le budget alloué au développement de cette application mobile est de [montant].
*   **Ressources :** La disponibilité des ressources (développeurs, designers) est essentielle.

### 5.2. Hypothèses

*   L'application backend de gestion de stock exposera des APIs REST stables et documentées pour la consommation par l'application mobile.
*   Les images de produits fournies sont de haute qualité et optimisées pour le web/mobile.
*   Une connexion internet est requise pour la plupart des fonctionnalités (consultation, commande).

## 6. Planning Prévisionnel (Haut niveau)

*   **Phase 1 : Conception (Semaines 1-2)**
    *   Spécification détaillée des APIs Backend.
    *   Conception UX/UI (wireframes, maquettes, prototypes).
*   **Phase 2 : Développement Backend APIs (Semaines 3-6)**
    *   Mise en place des APIs REST nécessaires dans l'application Spring Boot existante.
    *   Mise en place de la sécurité API (Authentification/Autorisation).
*   **Phase 3 : Développement Application Mobile (MVP) (Semaines 4-10)**
    *   Développement des fonctionnalités de base (catalogue, détails produit, panier, authentification simple).
*   **Phase 4 : Tests et Recette (Semaines 11-12)**
    *   Tests fonctionnels, tests de performance, correction des bugs.
    *   Recette client.
*   **Phase 5 : Déploiement Initial (Semaine 12)**
    *   Préparation et soumission aux stores (App Store, Google Play Store).

## 7. Livrables Attendus

*   **Documentation :** Spécifications API détaillées, plan de test, documentation technique du code.
*   **Design :** Maquettes UX/UI complètes, assets graphiques.
*   **Code Source :** Code source complet de l'application mobile.
*   **Application :** Fichiers binaires compilés de l'application mobile (APK pour Android, IPA pour iOS) prêts pour publication.
*   **Rapports :** Rapports de tests, rapports de performance.

## 8. Critères d'Acceptation

*   L'application mobile permet la consultation fluide du catalogue et des détails produits.
*   L'ajout d'un produit au panier et la modification des quantités fonctionnent correctement.
*   Un client peut s'inscrire, se connecter et passer une commande complète.
*   Le processus de commande est fonctionnel de bout en bout (du panier à la confirmation).
*   L'application est stable et ne présente pas de bugs bloquants.
*   L'application est conforme au design UX/UI validé.
*   Les données des produits et des commandes sont correctement synchronisées avec le backend.

Ce document servira de référence principale pour l'équipe de développement et le client tout au long du projet.
