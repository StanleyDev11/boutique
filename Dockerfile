# Stage 1: Build the application using Maven and JDK 21
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy the pom.xml and download dependencies first to leverage Docker cache
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy the rest of the source code
COPY src ./src

# Package the application, skipping tests
RUN mvn clean package -DskipTests

# Stage 2: Create the final lightweight image using JRE 21
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Expose the port the application runs on
EXPOSE 8085

# Copy the JAR file from the build stage
# The wildcard is used to match the versioned JAR file
COPY --from=build /app/target/boutique-*.jar app.jar

# Command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
