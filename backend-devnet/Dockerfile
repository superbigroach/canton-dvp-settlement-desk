# syntax=docker/dockerfile:1
# ---------------------------------------------------------------------------
# Multi-stage build for the Canton DvP Settlement Desk backend.
#
#   Stage 1 (build): Temurin 21 JDK + the committed Gradle wrapper builds the
#     Spring Boot fat jar. Self-contained — needs NO Daml SDK, because the Daml
#     Java codegen output is committed under src/main/generated-java.
#   Stage 2 (run):   Temurin 21 JRE runs the jar as a non-root user.
#
# Build from the REPO ROOT so the build context includes backend/ :
#     docker build -f backend/Dockerfile -t canton-dvp-desk:1.0.0 .
#
# The ledger endpoint + auth are runtime env (see application.yml). Example:
#     docker run -p 8080:8080 \
#       -e LEDGER_HOST=host.docker.internal -e LEDGER_PORT=6865 \
#       canton-dvp-desk:1.0.0
# ---------------------------------------------------------------------------

FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Warm the Gradle wrapper + dependency cache on the build files first so source
# edits don't re-download the world.
COPY backend/gradlew backend/settings.gradle backend/build.gradle ./
COPY backend/gradle ./gradle
RUN chmod +x gradlew && ./gradlew --version

# Now the sources (handwritten + committed Daml codegen output) and build.
COPY backend/src ./src
RUN ./gradlew bootJar --no-daemon

# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# Run as an unprivileged user.
RUN groupadd --system app && useradd --system --gid app --home /app app

# Copy the fat jar (bootJar excludes the -plain.jar via the wildcard + a guard).
COPY --from=build /workspace/build/libs/canton-dvp-desk-1.0.0.jar /app/app.jar

USER app
EXPOSE 8080

# Sensible container defaults; override at `docker run` / in k8s.
ENV SERVER_PORT=8080 \
    LEDGER_HOST=localhost \
    LEDGER_PORT=6865 \
    LEDGER_TLS=false

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
