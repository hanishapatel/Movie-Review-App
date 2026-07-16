# ---- build ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

# ---- run ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
# JVM flags tuned for tiny instances (Render free = 0.1 CPU / 512 MB):
# serial GC + C1-only JIT = less CPU/memory overhead; cap heap to leave headroom.
ENTRYPOINT ["java", "-XX:+UseSerialGC", "-XX:TieredStopAtLevel=1", "-XX:MaxRAMPercentage=70", "-jar", "app.jar"]
