# Guide de Déploiement : Application Boutique avec Docker

Ce guide vous montre comment déployer votre application "Boutique" et sa base de données MySQL sur votre serveur VPS en utilisant Docker et Docker Compose.

---

## Prérequis

*   Accès SSH root à votre VPS (`109.176.197.158`).
*   Votre projet "Boutique" poussé sur un dépôt Git (par exemple, GitHub). Assurez-vous que les fichiers `Dockerfile`, `docker-compose.yml`, `.env`, et `.dockerignore` sont bien inclus.

---

### Étape 1 : Connexion au VPS

Connectez-vous à votre serveur via SSH :
```bash
ssh root@109.176.197.158
```
*(Saisissez votre mot de passe root lorsque demandé.)*

---

### Étape 2 : Mettre à Jour et Installer les Outils

Assurez-vous que votre système est à jour et que `git` et `docker` sont installés.

```bash
# Mettre à jour le système
yum update -y

# Installer git (s'il n'est pas déjà là)
yum install -y git

# Installer Docker (ces commandes ajoutent le dépôt officiel de Docker)
yum install -y yum-utils
yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
yum install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

# Démarrer et activer Docker pour qu'il se lance au redémarrage
systemctl start docker
systemctl enable docker
```

---

### Étape 3 : Récupérer Votre Projet

Clonez votre projet depuis votre dépôt Git.

```bash
# Remplacez l'URL par celle de votre dépôt Git
git clone https://github.com/VOTRE_NOM/VOTRE_DEPOT.git

# Naviguez dans le dossier du projet
cd VOTRE_DEPOT 
```

---

### Étape 4 : Configurer le Mot de Passe de la Base de Données

Avant de lancer l'application, vous devez définir un mot de passe sécurisé pour votre base de données.

1.  Ouvrez le fichier `.env` avec un éditeur de texte comme `nano` :
    ```bash
    nano .env
    ```

2.  Remplacez `VOTRE_MOT_DE_PASSE_POUR_LA_BD` par un mot de passe fort de votre choix.
    ```env
    # Exemple
    DB_PASSWORD=MonMotDePasseSuperSecret123!
    ```

3.  Enregistrez et quittez l'éditeur (`Ctrl+X`, puis `Y`, puis `Entrée`).

---

### Étape 5 : Lancer l'Application avec Docker Compose

Grâce à `docker-compose`, une seule commande suffit pour construire l'image de votre application et lancer les conteneurs de l'application et de la base de données.

```bash
# Construire les images et démarrer les conteneurs en arrière-plan (-d)
docker-compose up --build -d
```

---

### Étape 6 : Configurer le Pare-feu

Ouvrez le port `8080` pour que votre application soit accessible depuis internet.

```bash
# Installer et activer le pare-feu (si nécessaire)
dnf install firewalld -y
systemctl enable firewalld --now

# Ouvrir le port 8081
firewall-cmd --zone=public --add-port=8081/tcp --permanent

# Recharger les règles du pare-feu
firewall-cmd --reload
```

---

## Vérification

Votre application devrait maintenant être en ligne !

1.  **Vérifiez que les conteneurs sont en cours d'exécution :**
    ```bash
    docker-compose ps
    ```
    Vous devriez voir `boutique-app` et `mysql-db` avec le statut `running`.

2.  **Consultez les logs de votre application pour vérifier qu'elle a démarré sans erreur :**
    ```bash
    docker-compose logs -f boutique-app
    ```
    *(Appuyez sur `Ctrl+C` pour quitter les logs.)*

3.  **Accédez à votre application dans un navigateur :**
    Ouvrez `http://109.176.197.158:8081`

---
Votre application est maintenant déployée ! Pour mettre à jour l'application, il vous suffira de récupérer les dernières modifications avec `git pull` sur votre serveur, puis de relancer la commande `docker-compose up --build -d`.
