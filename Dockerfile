# Java 17 API and the Vue production build, one origin, sized for Render Free (512 MiB).
FROM node:20-bookworm-slim AS web
WORKDIR /src/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
ENV VITE_SHOW_DEMO_ACCOUNTS=false
RUN npm run build

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY backend/pom.xml backend/pom.xml
COPY backend/src backend/src
COPY --from=web /src/frontend/dist/ backend/src/main/resources/static/
RUN mvn -f backend/pom.xml -DskipTests package

FROM eclipse-temurin:17-jre-jammy
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
ENV PORT=8080 \
    PUBLIC_MODE=true \
    SPRING_PROFILES_ACTIVE=h2 \
    H2_CONSOLE_ENABLED=false \
    SPRINGDOC_ENABLED=false \
    KNIFE4J_ENABLE=false \
    H2_FILE=/tmp/procurement \
    H2_EXTRA=DB_CLOSE_ON_EXIT=FALSE \
    UPLOAD_DIR=/tmp/uploads \
    TOMCAT_MAX_THREADS=20 \
    TOMCAT_MIN_SPARE=2 \
    TOMCAT_MAX_CONNECTIONS=80 \
    DB_POOL_SIZE=3 \
    JAVA_TOOL_OPTIONS="-Xms64m -Xmx256m -XX:MaxMetaspaceSize=96m -XX:MaxDirectMemorySize=32m -Xss256k -XX:+UseSerialGC -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"
COPY --from=build /src/backend/target/intelligent-procurement-1.0.0.jar /app/app.jar
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
  CMD curl -fsS "http://127.0.0.1:${PORT}/api/health" || exit 1
USER 65532:65532
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
