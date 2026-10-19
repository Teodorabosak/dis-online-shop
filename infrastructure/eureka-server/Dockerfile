# Faza 1: build (Maven unutar kontejnera - na racunaru ne treba instaliran Maven)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY . .
RUN --mount=type=cache,target=/root/.m2 \
    mvn -q -pl infrastructure/eureka-server -am package -DskipTests

# Faza 2: runtime (samo JRE + jar, manja slika)
FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /src/infrastructure/eureka-server/target/*.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"]
