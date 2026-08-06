FROM gradle:9.1.0-jdk21 AS build
WORKDIR /workspace
COPY settings.gradle build.gradle gradle.properties ./
COPY backend/build.gradle backend/build.gradle
COPY mock-pg/build.gradle mock-pg/build.gradle
RUN gradle :mock-pg:dependencies --no-daemon
COPY mock-pg/src mock-pg/src
RUN gradle :mock-pg:bootJar --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/mock-pg/build/libs/mock-pg-*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
