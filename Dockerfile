# Image de base avec OpenJDK 17
FROM openjdk:21-jdk-slim

# Définir le répertoire de travail
WORKDIR /app

# Copier le fichier JAR de l'application
COPY target/boutique-1.0.0-SNAPSHOT.jar app.jar

# Exposer le port (ajustez selon votre configuration dans application.yml)
EXPOSE 8080

# Commande pour démarrer l'application
ENTRYPOINT ["java", "-jar", "app.jar"]

# Exposer le port sur lequel votre application écoute (ex: 8080)
EXPOSE 8080