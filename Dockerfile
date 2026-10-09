# ==============================================================================
# PulseWallet - Spring Boot Production Container
# Multi-stage build with Eclipse Temurin JDK 25 and minimal JRE runtime
# ==============================================================================

# ---- Stage 1: Build Application ----
FROM eclipse-temurin:25-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and POM first for layer caching
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy application source code
COPY src/ src/

# Package executable JAR (skipping test suite for production container build)
RUN ./mvnw package -DskipTests -B

# ---- Stage 2: Production Runtime ----
FROM eclipse-temurin:25-jre-alpine AS runner

WORKDIR /app

# Create unprivileged application user
RUN addgroup -S pulsewallet && adduser -S pulsewallet -G pulsewallet

# Copy packaged JAR from builder stage
COPY --from=builder /app/target/pulsewallet-0.0.1-SNAPSHOT.jar app.jar

# Set ownership
RUN chown -R pulsewallet:pulsewallet /app

USER pulsewallet

# Environment defaults
ENV SPRING_PROFILES_ACTIVE=prod \
    SERVER_PORT=8080 \
    JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080

# Production Health Check
HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
