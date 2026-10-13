# ╔══════════════════════════════════════════════════════════════════════╗
# ║                   BACKEND DOCKERFILE                                 ║
# ║                                                                       ║
# ║  MULTI-STAGE BUILD — Industry best practice                           ║
# ║                                                                       ║
# ║  Stage 1 (builder): Compile Java + run Maven                          ║
# ║  Stage 2 (runtime): Minimal JRE image with just the JAR              ║
# ║                                                                       ║
# ║  WHY MULTI-STAGE?                                                     ║
# ║  Build image = 600MB (has Maven, JDK, source code)                   ║
# ║  Final image = 200MB (only JRE + compiled JAR)                       ║
# ║  Smaller image = faster deploy + less attack surface                  ║
# ╚══════════════════════════════════════════════════════════════════════╝

# ─────────────────────────────────────────────
# STAGE 1: BUILD
# Uses full JDK 17 + Maven to compile the app
# ─────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS builder

# Set working directory inside container
WORKDIR /app

# Copy Maven wrapper and pom.xml FIRST (Docker layer caching)
# If pom.xml doesn't change, Maven dependencies are cached
# This dramatically speeds up rebuilds when only source code changes
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

# Download dependencies (cached as long as pom.xml doesn't change)
RUN ./mvnw dependency:go-offline -B

# Copy source code (changes more frequently than pom.xml)
COPY src ./src

# Build the application, skip tests (tests run in CI pipeline)
# -B = batch mode (no interactive prompts)
# package = compile + test + package into JAR
RUN ./mvnw package -DskipTests -B

# ─────────────────────────────────────────────
# STAGE 2: RUNTIME
# Only the compiled JAR + minimal JRE
# ─────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine AS runtime

# Create a non-root user for security
# Running as root in containers is a security vulnerability
RUN addgroup -S pms && adduser -S pms -G pms

WORKDIR /app

# Create uploads directory for medical reports
RUN mkdir -p uploads/medical-reports && chown -R pms:pms /app

# Copy ONLY the compiled JAR from builder stage
# The *.jar pattern matches whatever Maven named it
COPY --from=builder /app/target/*.jar app.jar

# Switch to non-root user
USER pms

# Document which port the app listens on (doesn't actually publish it)
EXPOSE 8080

# Health check — Docker/Railway/Render uses this to know if app is healthy
# --interval=30s    → Check every 30 seconds
# --timeout=10s     → Give 10 seconds for response
# --start-period=60s → Wait 60s before first check (Spring Boot takes time to start)
# --retries=3       → Mark unhealthy after 3 consecutive failures
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# JVM tuning for containers:
# -XX:+UseContainerSupport       → Respect container memory limits (not host RAM)
# -XX:MaxRAMPercentage=75.0      → Use 75% of container memory for heap
# -Djava.security.egd=...        → Faster startup (don't wait for /dev/random entropy)
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
