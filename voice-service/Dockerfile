# ---------- build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY . .
RUN mvn -q -DskipTests -pl voice-service -am package

# ---------- run ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/voice-service/target/voice-service-*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java","-jar","/app/app.jar"]
