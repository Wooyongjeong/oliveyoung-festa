FROM gradle:9.1.0-jdk21 AS build
WORKDIR /workspace
COPY settings.gradle build.gradle gradle.properties ./
COPY backend/build.gradle backend/build.gradle
COPY mock-pg/build.gradle mock-pg/build.gradle
RUN gradle :backend:dependencies --no-daemon
COPY backend/src backend/src
RUN gradle :backend:bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/backend/build/libs/backend-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
