FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN ./gradlew installDist --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/install/ /opt/install/
RUN set -eux; app=$(find /opt/install -mindepth 1 -maxdepth 1 -type d | head -1); cp -R "$app"/* /app/
CMD ["./bin/users-service"]
