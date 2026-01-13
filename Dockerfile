# Utilise une image Maven pour la phase de build
FROM maven:3.9.5-eclipse-temurin-21 AS build

# Définit le répertoire de travail dans le conteneur
WORKDIR /app

# Copie le fichier pom.xml et télécharge les dépendances
# Cela permet de mettre en cache les dépendances si le pom.xml ne change pas
COPY pom.xml .
RUN mvn dependency:go-offline

# Copie le reste du code source
COPY src ./src

# Construit l'application en un fichier JAR exécutable
RUN mvn clean package -DskipTests

# Utilise une image JRE légère pour la phase d'exécution
FROM eclipse-temurin:21-jre-jammy

# Définit le répertoire de travail
WORKDIR /app

# Copie le fichier JAR construit depuis la phase de build
COPY --from=build /app/target/*.jar app.jar

# Expose le port sur lequel l'application Spring Boot s'exécutera
EXPOSE 8080

# Commande pour exécuter l'application
ENTRYPOINT ["java", "-jar", "app.jar"]
