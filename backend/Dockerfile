# ---------- Stage 1: build ----------
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Build the jar
COPY src ./src
RUN mvn package -DskipTests -B

# ---------- Stage 2: runtime ----------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S pms && adduser -S pms -G pms \
    && mkdir -p /app/uploads/medical-reports \
    && chown -R pms:pms /app

COPY --from=builder /app/target/*.jar app.jar

USER pms
EXPOSE 8080

# Render sets PORT; application.properties reads it via server.port=${PORT:8080}
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
