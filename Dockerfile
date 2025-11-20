# Étape 1: Construire l'application avec Maven et Java 21
FROM maven:3.9.6-eclipse-temurin-21 AS build

# Copier les sources de l'application
WORKDIR /app
COPY pom.xml .
COPY src ./src

# Construire le projet et créer le fichier JAR
# -DskipTests pour accélérer la construction en ignorant les tests
RUN mvn clean package -DskipTests

# Étape 2: Créer l'image finale légère pour l'exécution
FROM eclipse-temurin:21-jre-alpine

# Définir le répertoire de travail
WORKDIR /app

# Copier le fichier JAR construit depuis l'étape précédente
COPY --from=build /app/target/boutique-1.0.0-SNAPSHOT.jar app.jar

# Exposer le port sur lequel l'application s'exécute
EXPOSE 8085

# Commande pour démarrer l'application
ENTRYPOINT ["java", "-jar", "app.jar"]
