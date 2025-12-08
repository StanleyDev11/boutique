# Guide d'Implémentation : Améliorations de la Gestion des Factures

Ce document détaille les étapes techniques pour implémenter les fonctionnalités de confirmation, suppression, modification et les améliorations d'interface pour les factures d'achat.

---

### 1. Alerte de Confirmation Avant Sauvegarde de Facture

**Objectif :** Afficher une alerte de confirmation avec le montant total de la facture avant son enregistrement.

**Comment faire :**

1.  **Créer une fonction de calcul en JavaScript (`produits.html`)**
    -   Une fonction `calculateInvoiceTotal()` a été ajoutée. Elle parcourt toutes les lignes de produits (`.product-line`), lit la quantité et le prix d'achat, puis retourne le total.

    ```javascript
    function calculateInvoiceTotal() {
        let total = 0;
        const productLines = document.querySelectorAll('#product-invoice-list .product-line');
        productLines.forEach(line => {
            const prixAchatInput = line.querySelector('.prix-achat');
            const quantiteInput = line.querySelector('.quantite');
            const prixAchat = parseFloat(prixAchatInput.value) || 0;
            const quantite = parseFloat(quantiteInput.value) || 0;
            if (!isNaN(prixAchat) && !isNaN(quantite)) {
                total += prixAchat * quantite;
            }
        });
        return total;
    }
    ```

2.  **Modifier la logique de `preConfirm` dans SweetAlert (`produits.html`)**
    -   Dans la fonction `openFactureModal`, le bloc `preConfirm` de SweetAlert a été modifié.
    -   Au lieu de soumettre directement le formulaire, il appelle `calculateInvoiceTotal()`.
    -   Il affiche ensuite une **nouvelle alerte SweetAlert imbriquée** pour demander la confirmation du montant.
    -   Si l'utilisateur confirme, la logique originale de soumission du formulaire (via `fetch`) est exécutée. Sinon, le processus est annulé.

---

### 2. Suppression de Facture

**Objectif :** Permettre la suppression d'une facture tout en annulant son impact sur le stock des produits.

**Comment faire :**

1.  **Frontend (`produits.html`)**
    -   Un bouton "Supprimer" a été ajouté à la liste des factures, avec une restriction d'accès au rôle `ADMIN`.
    -   Une fonction JavaScript `confirmDeleteFacture(factureId, factureNumero)` a été créée. Elle affiche une alerte de confirmation et, si confirmée, envoie une requête `POST` à l'endpoint de suppression.

2.  **Backend**
    -   **Repository (`MouvementStockRepository.java`) :** Ajout de la méthode `List<MouvementStock> findByFacture(Facture facture);` pour retrouver les mouvements de stock liés à une facture.
    -   **Service (`ProduitService.java`) :** Une méthode `deleteFacture(Long id)` a été ajoutée. Son rôle est crucial :
        1.  Elle récupère la facture.
        2.  Pour chaque ligne de la facture, elle crée un mouvement de stock de type `SORTIE_PERTE` pour annuler l'entrée initiale. Elle utilise pour cela la méthode `stockService.enregistrerMouvement(mouvement)`.
        3.  Elle dé-lie les mouvements de stock de la facture en mettant leur champ `facture` à `null` pour éviter les erreurs de contrainte de clé étrangère.
        4.  Enfin, elle supprime l'entité `Facture` (ce qui supprime les `LigneFacture` par cascade).
    -   **Contrôleur (`ProduitController.java`) :** Un endpoint `@PostMapping("/facture/delete/{id}")` a été créé pour appeler `produitService.deleteFacture(id)`.

---

### 3. Modification de Facture

**Objectif :** Permettre la modification d'une facture existante.

**Stratégie adoptée :** Pour garantir la cohérence des données de stock, une modification est traitée comme une **annulation (suppression) suivie d'une re-création** de la facture.

**Comment faire :**

1.  **DTOs (Data Transfer Objects)**
    -   Ajout d'un champ `id` à `FactureDto.java` pour identifier la facture à modifier.
    -   Ajout d'un champ `nom` à `ProduitFactureDto.java` pour faciliter l'affichage.

2.  **Backend**
    -   **Contrôleur (`ProduitController.java`) :**
        -   L'endpoint `@GetMapping("/facture-form")` a été étendu à `@GetMapping({"/facture-form", "/facture-form/{id}"})` pour gérer à la fois la création (sans ID) et la modification (avec ID). En mode modification, il charge la facture, la convertit en DTO et la passe à la vue.
        -   L'endpoint `@PostMapping("/save-facture")` a été mis à jour : il vérifie si un `id` est présent dans le DTO. Si oui, il appelle `produitService.deleteFacture(id)` avant d'appeler `produitService.createFacture(dto)`.
    -   **Service (`ProduitService.java`) :**
        -   La méthode `saveFacture` a été renommée `createFacture` pour plus de clarté.

3.  **Frontend**
    -   **Template (`facture-form.html`) :**
        -   Le formulaire est maintenant lié à l'objet `factureDto` via `th:object`.
        -   Un champ `<input type="hidden" th:field="*{id}">` a été ajouté.
        -   Une boucle `th:each` a été ajoutée pour afficher les lignes de produits pré-remplies si la facture existe.
        -   L'attribut `x-init` des lignes de produit a été modifié pour passer les données initiales au composant Alpine.js, en utilisant `th:attr` et `#strings.escapeJavaScript` pour une construction sûre de l'appel JavaScript.
    -   **JavaScript (`produits.html`) :**
        -   La fonction `openFactureModal(factureId)` a été mise à jour pour appeler l'URL avec ou sans ID.
        -   Le composant Alpine.js `productLine` a été modifié pour accepter des données initiales et pré-remplir l'état du composant.

---

### 4. Améliorations de l'Interface Utilisateur (UI)

**Objectif :** Améliorer la visibilité des champs de formulaire.

**Comment faire :**

1.  **CSS (`facture-form.html`)**
    -   La classe Tailwind CSS `bg-slate-100` a été ajoutée à tous les champs `<input>` dans le formulaire de facture (champs d'en-tête, lignes existantes et template pour les nouvelles lignes). Cela leur donne un fond "bleu-gris" clair qui les distingue du fond blanc du formulaire.

---
### Résolution des Erreurs de Compilation

- **Dépendance Circulaire :** Une erreur "cannot find symbol" persistante a été résolue en corrigeant une dépendance circulaire entre `ProduitService` et `StockService`. L'appel dans `ProduitService.deleteFacture` a été modifié pour utiliser la méthode `stockService.enregistrerMouvement(mouvement)` (à un argument) au lieu de la version surchargée qui dépendait indirectement d'autres services.
- **Enum `TypeMouvement` :** L'utilisation de `TypeMouvement.SORTIE` (qui n'existait pas) a été corrigée en `TypeMouvement.SORTIE_PERTE`.
- **Initialisation Alpine.js :** Le problème du nom de produit qui ne s'affichait pas en mode édition a été résolu en passant correctement les données initiales de Thymeleaf au composant Alpine.js.
