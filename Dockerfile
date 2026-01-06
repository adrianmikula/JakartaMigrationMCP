# Multi-stage build for Jakarta Migration MCP Server on Apify
# Based on Apify Dockerfile best practices for custom base images

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
FROM eclipse-temurin:21-jre-alpine

# Install wget for health checks (requires root)
RUN apk add --no-cache wget

# Create a non-root user for security (following Apify best practices)
# Apify base images use 'myuser', but for custom images we can use any name
# Using 'appuser' to avoid confusion with Apify-specific setup
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Set working directory
WORKDIR /app

# Copy the JAR from builder stage
COPY --from=builder --chown=appuser:appgroup /app/build/libs/jakarta-migration-mcp-*.jar app.jar

# Switch to non-root user (after all root operations)
USER appuser

# Expose the port for SSE transport
EXPOSE 8080

# Set environment variables for Apify/SSE mode
ENV MCP_TRANSPORT=sse
ENV MCP_SSE_PORT=8080
ENV MCP_SSE_PATH=/mcp/sse
ENV SPRING_PROFILES_ACTIVE=mcp-sse

# Health check endpoint (Apify can use this)
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the Spring Boot application
# Using exec form for proper signal handling
CMD ["java", \
     "-XX:+UseContainerSupport", \
     "-XX:MaxRAMPercentage=75.0", \
     "-Djava.security.egd=file:/dev/./urandom", \
     "-jar", \
     "app.jar", \
     "--spring.profiles.active=mcp-sse", \
     "--spring.ai.mcp.server.transport=sse"]

