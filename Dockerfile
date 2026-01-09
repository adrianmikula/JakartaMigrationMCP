# Multi-stage build for Jakarta Migration MCP Server on Apify
# Based on Apify Dockerfile requirements for Java applications

# Stage 1: Build the application
FROM gradle:8.5-jdk21 AS builder

WORKDIR /app

# Copy Gradle files first for better caching
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY gradle ./gradle

# Download dependencies (this layer will be cached if build files don't change)
RUN gradle dependencies --no-daemon || true

# Copy source code
COPY src ./src

# Build the application
RUN gradle bootJar --no-daemon

# Stage 2: Runtime image
# Use Apify base image for platform compatibility
FROM apify/actor-node:20

# Install Java 21 JRE in the Apify container
# Apify images are Alpine-based, so we use apk
USER root
RUN apk add --no-cache \
    openjdk21-jre-headless \
    wget

# Set up working directory (Apify uses /home/myuser)
WORKDIR /home/myuser

# Copy the JAR from builder stage
COPY --from=builder /app/build/libs/jakarta-migration-mcp-*.jar ./app.jar

# Copy Apify-specific configuration files
COPY .actor ./.actor
COPY README.md ./README.md

# Ensure the 'myuser' user owns the files (Apify base image uses myuser)
RUN chown -R myuser:myuser /home/myuser

# Switch to myuser (Apify base image default user)
USER myuser

# Expose the port for SSE transport
# Use APIFY_CONTAINER_PORT if available (Apify requirement), otherwise default to 8080
EXPOSE 8080

# Set environment variables for Apify/Streamable HTTP mode
# APIFY_CONTAINER_PORT will be set by Apify platform at runtime
ENV MCP_TRANSPORT=streamable-http
ENV MCP_STREAMABLE_HTTP_PORT=8080
ENV SPRING_PROFILES_ACTIVE=mcp-streamable-http

# Health check endpoint (Apify can use this)
# Use APIFY_CONTAINER_PORT if available, otherwise default to 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:${APIFY_CONTAINER_PORT:-8080}/actuator/health || exit 1

# Run the Spring Boot application
# Using shell form to support environment variable substitution
# APIFY_CONTAINER_PORT is set by Apify platform at runtime
# Pass APIFY_CONTAINER_PORT to Spring Boot via system property and server.port
# Set memory limits explicitly (Java 21+ is container-aware, but explicit limits are safer)
# Use -Xmx to set max heap size (adjust based on Actor memory allocation)
CMD ["sh", "-c", "java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Xmx1g -Djava.security.egd=file:/dev/./urandom -DMCP_STREAMABLE_HTTP_PORT=${APIFY_CONTAINER_PORT:-8080} -jar app.jar --spring.profiles.active=mcp-streamable-http --spring.ai.mcp.server.transport=streamable-http --server.port=${APIFY_CONTAINER_PORT:-8080}"]

