# ╔══════════════════════════════════════════════════════════════════════╗
# ║                   BACKEND DOCKERFILE                               ║
# ╚══════════════════════════════════════════════════════════════════════╝

# ─────────────────────────────────────────────
# STAGE 1: BUILD
# Uses Maven image directly
# No mvnw or maven-wrapper.jar needed
# ─────────────────────────────────────────────
FROM maven:3.9.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# Copy pom.xml first for Docker cache
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build application
RUN mvn clean package -DskipTests -B

# ─────────────────────────────────────────────
# STAGE 2: RUNTIME
# ─────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine AS runtime

# Create non-root user
RUN addgroup -S pms && adduser -S pms -G pms

WORKDIR /app

# Create upload directory
RUN mkdir -p uploads/medical-reports && \
    chown -R pms:pms /app

# Copy compiled jar
COPY --from=builder /app/target/*.jar app.jar

USER pms

EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# JVM options
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", \
    "app.jar"]
