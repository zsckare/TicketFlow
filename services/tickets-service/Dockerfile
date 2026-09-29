# ============================================================
# BUILD STAGE
# ============================================================
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

# Copy Gradle Wrapper and build configuration first so Docker can
# preserve these layers when only application source code changes.
COPY gradlew .
COPY gradle ./gradle
COPY settings.gradle.kts .
COPY build.gradle.kts .
COPY gradle.properties .

RUN chmod +x gradlew

# Cache the Gradle distribution between Docker builds.
RUN --mount=type=cache,target=/root/.gradle     ./gradlew --version --no-daemon

# Source code changes more frequently than Gradle configuration.
COPY src ./src

# Reuse Gradle's distribution/dependency cache while building Ktor.
RUN --mount=type=cache,target=/root/.gradle     ./gradlew installDist --no-daemon

# ============================================================
# RUNTIME STAGE
# ============================================================
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/build/install/ /opt/install/

RUN set -eux;     app=$(find /opt/install -mindepth 1 -maxdepth 1 -type d | head -1);     cp -R "$app"/* /app/

CMD ["./bin/tickets-service"]
