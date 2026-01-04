# ---------- Build stage ----------
FROM maven:3.9.12-eclipse-temurin-25 AS build
WORKDIR /workspace

# Cache Maven
COPY pom.xml .
RUN mvn -q -e -DskipTests dependency:go-offline

# Sources
COPY src ./src

# Build du jar
RUN mvn -q -DskipTests package

# ---------- Runtime stage ----------
FROM eclipse-temurin:25-jre
WORKDIR /app

# Dossier persistance SQLite
RUN mkdir -p /app/storage

# Copier le jar Spring Boot
COPY --from=build /workspace/target/*.jar /app/app.jar

EXPOSE 8000

# Tuning JVM (ajustable)
ENV JAVA_OPTS="-XX:MaxRAMPercentage=60 -XX:InitialRAMPercentage=20 -Dfile.encoding=UTF-8"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
