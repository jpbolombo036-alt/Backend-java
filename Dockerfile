# Étape 1 : Build
FROM maven:3.8.5-openjdk-17-slim AS build
WORKDIR /app

# Copier le pom.xml et télécharger les dépendances (cache)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copier le code source et compiler
COPY src ./src
RUN mvn clean package -Dmaven.test.skip=true

# Étape 2 : Exécution
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-8000} -jar app.jar"]