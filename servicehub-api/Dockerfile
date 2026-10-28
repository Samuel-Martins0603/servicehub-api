# ---- build ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

# ---- runtime ----
FROM eclipse-temurin:17-jre
RUN useradd --system --uid 1001 app
WORKDIR /app
COPY --from=build /build/target/servicehub-api-*.jar app.jar
USER app
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
