# Guide d'Implémentation : Améliorations de la Gestion des Factures

Ce document détaille les étapes techniques pour implémenter les fonctionnalités de confirmation, suppression, modification et les améliorations d'interface pour les factures d'achat.

---

### 1. Alerte de Confirmation Avant Sauvegarde de Facture

**Objectif :** Afficher une alerte de confirmation avec le montant total de la facture avant son enregistrement.

**Comment faire :**

1.  **Créer une fonction de calcul en JavaScript (`produits.html`)**
    -   Une fonction `calculateInvoiceTotal()` a été ajoutée. Elle parcourt toutes les lignes de produits (`.product-line`), lit la quantité et le prix d'achat, puis retourne le total.

2.  **Modifier la logique de `preConfirm` dans SweetAlert (`produits.html`)**
    -   Dans la fonction `openFactureModal`, le bloc `preConfirm` de SweetAlert a été modifié pour appeler `calculateInvoiceTotal()` et afficher une nouvelle alerte imbriquée pour demander la confirmation du montant.

---

### 2. Suppression de Facture

**Objectif :** Permettre la suppression d'une facture tout en annulant son impact sur le stock des produits.

**Comment faire :**

1.  **Frontend (`produits.html`)**
    -   Ajout du bouton "Supprimer" et de la fonction JavaScript `confirmDeleteFacture(...)` qui envoie une requête `POST` à l'endpoint de suppression.

2.  **Backend**
    -   **Repository (`MouvementStockRepository.java`) :** Ajout de la méthode `findByFacture(Facture facture)` pour retrouver les mouvements de stock liés.
    -   **Service (`ProduitService.java`) :** Création de la méthode `deleteFacture(Long id)` qui annule l'impact sur le stock (en créant un mouvement de `SORTIE_PERTE`) et dé-lie les anciens mouvements avant de supprimer la facture.
    -   **Contrôleur (`ProduitController.java`) :** Création de l'endpoint `@PostMapping("/facture/delete/{id}")` pour appeler le service.

---

### 3. Modification de Facture

**Objectif :** Permettre la modification d'une facture existante.

**Stratégie :** Annulation (suppression) de l'ancienne facture puis re-création avec les nouvelles données pour garantir la cohérence du stock.

**Comment faire :**

1.  **DTOs :** Ajout d'un champ `id` à `FactureDto.java` et `nom` à `ProduitFactureDto.java`.

2.  **Backend**
    -   **Contrôleur (`ProduitController.java`) :**
        -   L'endpoint `@GetMapping("/facture-form")` a été étendu à `@GetMapping({"/facture-form", "/facture-form/{id}"})` pour gérer les modes création et édition.
        -   L'endpoint `@PostMapping("/save-facture")` a été mis à jour pour appeler `produitService.deleteFacture(id)` si un ID est présent, avant de recréer la facture via `produitService.createFacture(dto)`.
    -   **Service (`ProduitService.java`) :** La méthode `saveFacture` a été renommée `createFacture`.

3.  **Frontend**
    -   **Template (`facture-form.html`) :** Le formulaire a été rendu dynamique avec `th:object`, `th:field`, et une boucle `th:each` pour afficher les lignes existantes. L'attribut `x-init` a été mis à jour pour passer les données initiales au composant Alpine.js.
    -   **JavaScript (`produits.html`) :** La fonction `openFactureModal(factureId)` a été adaptée pour utiliser l'URL avec ou without ID, et le composant Alpine.js a été modifié pour accepter les données initiales.

---

### 4. Améliorations de l'Interface (Factures)

**Objectif :** Améliorer la visibilité des champs de formulaire.

**Comment faire :**

1.  **CSS (`facture-form.html`) :** La classe Tailwind CSS `bg-slate-100` a été ajoutée à tous les champs `<input>` du formulaire de facture.

---

### 5. Rapport Amélioré des Ventes (`ventes-historique.html`)

**Objectif :** Ajouter un onglet "Ventes par Produit" avec des fonctionnalités de recherche, de filtrage par date et d'export.

**Comment faire :**

1.  **Backend**
    -   **DTO (`ProduitVenteStatsDto.java`) :** Création d'un nouveau DTO pour contenir les statistiques de vente agrégées par produit (Produit, quantité totale, revenu total).
    -   **Repository (`LigneVenteRepository.java`) :**
        -   Création d'une nouvelle méthode `findProduitVenteStats` avec une requête JPQL.
        -   La requête agrège les `LigneVente` par produit et accepte des paramètres pour filtrer par mot-clé (nom du produit), date de début et date de fin.
    -   **Contrôleur (`RapportController.java`) :**
        -   La méthode `ventesHistorique` a été mise à jour pour accepter un paramètre `tab` afin de gérer l'onglet actif.
        -   Elle appelle `findProduitVenteStats` en passant les filtres de la requête.
        -   Deux nouveaux endpoints d'export ont été ajoutés : `/rapports/produits/export/excel` et `/rapports/produits/export/pdf`. Ils réutilisent la même méthode de repository filtrée pour générer les fichiers correspondants (Excel via Apache POI, PDF via un nouveau template `rapport-ventes-par-produit-print.html`).

2.  **Frontend (`ventes-historique.html`)**
    -   **Structure à Onglets :** Une navigation par onglets a été ajoutée pour basculer entre "Historique des Ventes" et "Ventes par Produit". Le contenu est affiché conditionnellement avec `th:if` basé sur le paramètre d'URL `tab`.
    -   **Filtres :** Un formulaire de filtre (nom produit, dates) a été ajouté à l'onglet "Ventes par Produit".
    -   **JavaScript :**
        -   La fonction `updateLinks` a été modifiée pour construire dynamiquement les URLs d'export en fonction de l'onglet actif et des filtres saisis.
        -   Le bouton "Imprimer" est maintenant masqué lorsque l'onglet "Ventes par Produit" est actif.
        -   L'initialisation des graphiques a été corrigée pour ne se déclencher que lorsque leur onglet est visible, résolvant une `ReferenceError`.
        -   L'ordre des scripts a été corrigé pour s'assurer que toutes les fonctions sont définies avant d'être appelées.

---

### Résolution des Erreurs de Compilation

- **Dépendance Circulaire :** Une erreur "cannot find symbol" a été résolue en corrigeant une dépendance circulaire entre `ProduitService` et `StockService`.
- **Enum `TypeMouvement` :** L'utilisation de `TypeMouvement.SORTIE` (qui n'existait pas) a été corrigée en `TypeMouvement.SORTIE_PERTE`.
- **Initialisation Alpine.js :** Le problème du nom de produit qui ne s'affichait pas en mode édition a été résolu.
- **Erreur 404 pour `.well-known` :** La configuration de Spring Security a été mise à jour pour ignorer ces requêtes des outils de développement de Chrome, afin de ne pas polluer les logs.