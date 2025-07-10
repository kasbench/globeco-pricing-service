# syntax=docker/dockerfile:1

# ---- Build Stage ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN apt-get update && apt-get install -y ca-certificates && update-ca-certificates
RUN ./gradlew clean bootJar --no-daemon

# ---- Run Stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# Create non-root user
RUN useradd -ms /bin/bash appuser
USER appuser

COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8083

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8083/actuator/health/liveness || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"] 