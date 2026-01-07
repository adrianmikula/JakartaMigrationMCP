# Apify Dockerfile Updates - Based on Investigation Requirements

## Date: 2026-01-07

## Summary

Updated Dockerfile to comply with Apify's requirements for Java applications, based on the investigation document recommendations.

## Key Changes

### 1. ✅ Use Apify Base Image

**Before**: Used `eclipse-temurin:21-jre-alpine` (custom Java image)

**After**: Uses `apify/actor-node:20` (Apify base image) with Java installed on top

**Why**: 
- Ensures compatibility with Apify platform
- Provides Apify SDK/CLI tools if needed
- Follows Apify security protocols

### 2. ✅ Use Apify User (UID 1000)

**Before**: Created custom `appuser` with custom UID

**After**: Uses `apify` user (UID 1000) as required by Apify

**Why**:
- Apify enforces non-root user with UID 1000
- Prevents permission issues
- Required for Apify platform security

### 3. ✅ Install Java 21 JRE

**Before**: Used Alpine-based Java image

**After**: Installs `openjdk-21-jre-headless` in Debian-based Apify image

**Why**:
- Apify base images are Debian-based
- Need to install Java manually when using Apify base image
- Java 21 for compatibility with Spring Boot application

### 4. ✅ Set Memory Limits Explicitly

**Before**: Relied on container memory awareness only

**After**: Added explicit `-Xmx1g` JVM flag

**Why**:
- Java JVM needs explicit memory limits for container environments
- Prevents OOM errors in Apify's memory-constrained environment
- Can be adjusted based on Actor memory allocation

### 5. ✅ Use /home/apify Working Directory

**Before**: Used `/app` as working directory

**After**: Uses `/home/apify` (Apify standard)

**Why**:
- Apify expects files in `/home/apify`
- Matches Apify's file system structure
- Required for proper file permissions

## Updated Dockerfile Structure

```dockerfile
# Stage 1: Build (unchanged)
FROM gradle:8.5-jdk21 AS builder
...

# Stage 2: Runtime (updated)
FROM apify/actor-node:20

# Install Java 21 JRE
USER root
RUN apt-get update && apt-get install -y \
    openjdk-21-jre-headless \
    wget \
    && rm -rf /var/lib/apt/lists/*

# Use Apify working directory
WORKDIR /home/apify

# Copy JAR and config files
COPY --from=builder /app/build/libs/jakarta-migration-mcp-*.jar ./app.jar
COPY .actor ./.actor
COPY README.md ./README.md

# Set ownership to apify user
RUN chown -R apify:apify /home/apify

# Switch to apify user
USER apify

# Run with explicit memory limits
CMD sh -c "java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Xmx1g ..."
```

## Additional Updates

### actor.json - Memory Configuration

Added `defaultRunOptions.memory: 2048` to ensure Actor has sufficient memory for Java migration tasks:

```json
{
  "defaultRunOptions": {
    "memory": 2048
  }
}
```

**Why**:
- Migration tasks can be memory-intensive
- Default 256MB/512MB is insufficient for large Java projects
- 2GB provides headroom for complex migrations

## Apify Infrastructure Integration

### What We DON'T Need

Since this is an **MCP server** (not a traditional Actor), we don't need to:
- ❌ Read from Apify Key-Value Store (`$APIFY_DEFAULT_KEY_VALUE_STORE_ID/INPUT`)
- ❌ Write to Apify Dataset (`POST $APIFY_DEFAULT_DATASET_ID`)
- ❌ Use Apify JavaScript SDK

### What We DO Need

Since this is an **MCP server in standby mode**, we need:
- ✅ HTTP server listening on `APIFY_CONTAINER_PORT`
- ✅ MCP endpoint at `/mcp/sse`
- ✅ Health check endpoint at `/actuator/health`
- ✅ Proper user permissions (`apify` user)
- ✅ Memory limits for JVM

## Testing Checklist

After deploying with these changes:

1. ✅ **Actor builds successfully** - Check Apify Console build logs
2. ✅ **Actor starts successfully** - Check runtime logs
3. ✅ **Health check passes** - `/actuator/health` responds
4. ✅ **MCP endpoint accessible** - `GET /mcp/sse` returns SSE stream
5. ✅ **Port configuration** - Server listens on `APIFY_CONTAINER_PORT`
6. ✅ **Memory usage** - JVM respects memory limits
7. ✅ **User permissions** - No permission errors in logs

## Memory Configuration Notes

The `-Xmx1g` flag sets max heap to 1GB. This can be adjusted based on:
- Actor memory allocation in Apify Console
- Typical project sizes being migrated
- Performance requirements

**Recommendation**: Start with 1GB, monitor usage, and adjust as needed.

## References

- [Apify Actor Documentation](https://docs.apify.com/platform/actors)
- [Apify Base Images](https://docs.apify.com/platform/actors/development/base-images)
- [Apify Standby Mode](https://docs.apify.com/platform/actors/development/standby-mode)
- [Investigation Document](./apify-invisible-deployment-01-07-2026.md)

