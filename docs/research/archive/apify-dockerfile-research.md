# Apify Dockerfile Research for Java MCP Server

## Executive Summary

This document summarizes research on creating an optimal Dockerfile for running a Java-based MCP (Model Context Protocol) server on the Apify platform. Since Apify provides base images for Node.js and Python but not for Java, we need to create a custom Dockerfile using a standard Java base image.

## Key Findings

### 1. Apify Base Images

**Available Base Images:**
- **Node.js**: `apify/actor-node` (versions 20, 22, 24)
- **Python**: `apify/actor-python` (versions 3.9-3.13)
- **Java**: ❌ **Not available** - requires custom Dockerfile

**Reference:** [Apify Dockerfile Documentation](https://docs.apify.com/platform/actors/development/actor-definition/dockerfile)

### 2. Custom Dockerfile Support

Apify **fully supports** custom Dockerfiles with any base image. Key points:

- Dockerfile can be placed in:
  1. `.actor/Dockerfile` (preferred)
  2. `Dockerfile` in root directory
  3. Referenced from `.actor/actor.json` via `dockerfile` field

- **Performance Note**: Apify base images are pre-cached on Apify servers, providing faster builds. Custom images don't have this advantage but are fully supported.

### 3. Security Best Practices

**Non-Root User Requirement:**
- Apify base images use a non-root user `myuser` with working directory `/home/myuser`
- For custom images, we should follow the same pattern for security
- Use `--chown` flag on `COPY` commands to set proper ownership

**Reference:** [Apify Dockerfile Security Updates](https://docs.apify.com/platform/actors/development/actor-definition/dockerfile#updating-older-dockerfiles)

### 4. Dockerfile Optimization Tips

From Apify documentation:

1. **Layer Caching**: Copy dependency files first, then install dependencies, then copy source code
2. **Multi-Stage Builds**: Use for smaller final images
3. **Minimize Layers**: Combine RUN commands where possible
4. **Use Specific Tags**: Avoid `latest` tag for reproducibility

### 5. Application-Specific Requirements

**Our Java MCP Server:**
- **Language**: Java 21
- **Framework**: Spring Boot 3.2.0
- **Transport**: SSE (Server-Sent Events) for Apify
- **Port**: 8080
- **Endpoint**: `/mcp/sse`
- **Health Check**: `/actuator/health`

**Build Artifact:**
- JAR file: `jakarta-migration-mcp-1.0.0-SNAPSHOT.jar`
- Built with Gradle `bootJar` task

## Recommended Dockerfile Strategy

### Option 1: Multi-Stage Build (Recommended)

**Advantages:**
- Smaller final image (only JRE, not JDK)
- Faster builds (dependencies cached separately)
- Production-ready optimization

**Base Image Choices:**
- `eclipse-temurin:21-jre-alpine` - Small, official, well-maintained
- `openjdk:21-jre-slim` - Alternative option
- `amazoncorretto:21-alpine` - AWS-optimized option

### Option 2: Single-Stage Build

**Use Case**: Simpler setup, but larger image size

**Base Image:**
- `gradle:8.5-jdk21` - Includes Gradle and JDK
- Build and run in same image

## Implementation Details

### Environment Variables for Apify

```bash
MCP_TRANSPORT=sse
MCP_SSE_PORT=8080
MCP_SSE_PATH=/mcp/sse
SPRING_PROFILES_ACTIVE=mcp-sse
```

### Health Check Configuration

Apify can use health checks to monitor Actor status:
- Endpoint: `http://localhost:8080/actuator/health`
- Should return HTTP 200 when healthy

### Port Exposure

- **Required**: Expose port 8080
- Apify will route traffic to this port

### JVM Optimization for Containers

```bash
-XX:+UseContainerSupport
-XX:MaxRAMPercentage=75.0
```

These flags ensure Java respects container memory limits.

## Comparison with Apify Base Images

| Feature | Apify Base Images | Custom Java Image |
|---------|-------------------|-------------------|
| Pre-cached | ✅ Yes | ❌ No |
| Non-root user | ✅ `myuser` | ⚠️ Must configure |
| Working directory | `/home/myuser` | Custom (e.g., `/app`) |
| SDK included | ✅ Yes (Node/Python) | ❌ N/A for Java |
| Build optimization | ✅ Optimized | ⚠️ Manual optimization |

## Dockerfile Location Options

1. **Root Directory** (`Dockerfile`)
   - Simplest option
   - Automatically detected by Apify

2. **`.actor/Dockerfile`**
   - Better organization
   - Keeps Apify-specific files together

3. **`.actor/actor.json` Reference**
   ```json
   {
     "dockerfile": "path/to/Dockerfile"
   }
   ```

## Testing the Dockerfile

### Local Testing

```bash
# Build the image
docker build -t jakarta-migration-mcp .

# Run locally
docker run -p 8080:8080 \
  -e MCP_TRANSPORT=sse \
  jakarta-migration-mcp

# Test health endpoint
curl http://localhost:8080/actuator/health

# Test MCP endpoint
curl http://localhost:8080/mcp/sse
```

### Apify Deployment

1. Push code to Apify platform
2. Apify will automatically detect and use the Dockerfile
3. Build will use the custom Dockerfile
4. Actor will run with SSE transport on port 8080

## References

- [Apify Dockerfile Documentation](https://docs.apify.com/platform/actors/development/actor-definition/dockerfile)
- [Apify Base Docker Images](https://github.com/apify/apify-actor-docker)
- [Docker Best Practices](https://docs.docker.com/develop/dev-best-practices/)
- [Spring Boot Docker Guide](https://spring.io/guides/gs/spring-boot-docker/)

## Next Steps

1. ✅ Create optimized Dockerfile (multi-stage build)
2. ✅ Test Dockerfile locally
3. ⏭️ Create `.actor/actor.json` if needed for Apify configuration
4. ⏭️ Deploy to Apify platform
5. ⏭️ Verify SSE transport works correctly
6. ⏭️ Test MCP tools via Apify Actor

## Notes

- The Dockerfile uses Alpine Linux for smaller image size
- Non-root user (`appuser`) is created for security
- Health check is configured for Apify monitoring
- JVM is optimized for container environments
- Multi-stage build separates build and runtime concerns

