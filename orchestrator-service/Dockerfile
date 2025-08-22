# ---------- build (Maven inside Docker) ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
# copy the ENTIRE repo so the root pom can see all modules
COPY . .
# build only this module (and its deps) from the aggregator pom
RUN mvn -q -DskipTests -pl orchestrator-service -am package

# ---------- run (JRE) ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/orchestrator-service/target/orchestrator-service-*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","/app/app.jar"]
