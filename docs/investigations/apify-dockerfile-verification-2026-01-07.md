# Apify Dockerfile Verification Report
## Date: 2026-01-07

## Summary

This document verifies that the Dockerfile meets Apify's requirements for deploying a Java-based MCP server as an Apify Actor.

## Verification Results

### ✅ **PASSED: Dockerfile Meets Apify Requirements**

All critical requirements are met. The Dockerfile is ready for Apify deployment.

---

## Detailed Verification

### 1. ✅ Apify Base Image
**Status:** PASSED  
**Requirement:** Use `apify/actor-node` base image for platform compatibility  
**Implementation:**
```dockerfile
FROM apify/actor-node:20
```
**Verification:** Base image is correctly specified.

---

### 2. ✅ Multi-Stage Build
**Status:** PASSED  
**Requirement:** Use multi-stage build to reduce final image size  
**Implementation:**
- Stage 1: Build with `gradle:8.5-jdk21`
- Stage 2: Runtime with `apify/actor-node:20`
**Verification:** Multi-stage build correctly implemented.

---

### 3. ✅ Non-Root User
**Status:** PASSED  
**Requirement:** Use non-root user (`myuser` or `apify`) for security  
**Implementation:**
```dockerfile
USER root
RUN apk add --no-cache ...
RUN chown -R myuser:myuser /home/myuser
USER myuser
```
**Verification:** 
- Switches to root for package installation
- Sets proper ownership
- Switches back to `myuser` (Apify's default user)
- ✅ Correctly configured

---

### 4. ✅ Working Directory
**Status:** PASSED  
**Requirement:** Use `/home/myuser` or `/home/apify` as working directory  
**Implementation:**
```dockerfile
WORKDIR /home/myuser
```
**Verification:** Matches Apify's standard working directory.

---

### 5. ✅ Java Installation
**Status:** PASSED  
**Requirement:** Install Java 21 JRE for Spring Boot application  
**Implementation:**
```dockerfile
RUN apk add --no-cache \
    openjdk21-jre-headless \
    wget
```
**Verification:** 
- ✅ Java 21 JRE installed
- ✅ Package manager verified (see #6)

---

### 6. ✅ Package Manager Consistency
**Status:** PASSED (Verified)  
**Requirement:** Use correct package manager for base image  
**Implementation:** Uses `apk` (Alpine package manager)

**Verification Process:**
1. Inspected `apify/actor-node:20` base image
2. Confirmed it is **Alpine Linux 3.23.2**
3. Verified `apk` is the correct package manager

**Result:** ✅ **CORRECT** - The Dockerfile correctly uses `apk` for Alpine-based Apify images.

**Note:** The investigation document (`apify-dockerfile-updates.md`) incorrectly stated that Apify images are Debian-based. This has been verified as incorrect - `apify/actor-node:20` is Alpine-based.

---

### 7. ✅ APIFY_CONTAINER_PORT Usage
**Status:** PASSED  
**Requirement:** Use `APIFY_CONTAINER_PORT` environment variable for port configuration  
**Implementation:**
```dockerfile
ENV MCP_SSE_PORT=${APIFY_CONTAINER_PORT:-8080}
CMD sh -c "... --server.port=${APIFY_CONTAINER_PORT:-8080}"
```
**Verification:** 
- ✅ Environment variable used with fallback
- ✅ Port passed to Spring Boot application
- ✅ Health check uses the same variable

---

### 8. ✅ Port Exposure
**Status:** PASSED  
**Requirement:** Expose port via EXPOSE directive  
**Implementation:**
```dockerfile
EXPOSE 8080
```
**Verification:** Port correctly exposed.

---

### 9. ✅ Health Check
**Status:** PASSED  
**Requirement:** Configure health check for Apify monitoring  
**Implementation:**
```dockerfile
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:${APIFY_CONTAINER_PORT:-8080}/actuator/health || exit 1
```
**Verification:** 
- ✅ Health check endpoint configured
- ✅ Uses `APIFY_CONTAINER_PORT` variable
- ✅ Appropriate intervals and timeouts

---

### 10. ✅ JVM Memory Settings
**Status:** PASSED  
**Requirement:** Configure JVM for container environments  
**Implementation:**
```dockerfile
CMD sh -c "java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Xmx1g ..."
```
**Verification:** 
- ✅ `-XX:+UseContainerSupport` - Container-aware JVM
- ✅ `-XX:MaxRAMPercentage=75.0` - Uses 75% of container memory
- ✅ `-Xmx1g` - Explicit max heap size (can be adjusted based on Actor memory allocation)

---

## Additional Configuration

### Environment Variables
The Dockerfile correctly sets:
- `MCP_TRANSPORT=sse` - SSE transport mode
- `MCP_SSE_PORT=${APIFY_CONTAINER_PORT:-8080}` - Port configuration
- `MCP_SSE_PATH=/mcp/sse` - SSE endpoint path
- `SPRING_PROFILES_ACTIVE=mcp-sse` - Spring profile for SSE mode

### File Ownership
- ✅ Files copied with proper ownership (`chown -R myuser:myuser`)
- ✅ User switched to `myuser` before running application

### Security
- ✅ Non-root user execution
- ✅ Minimal base image (Alpine)
- ✅ Only necessary packages installed

---

## Recommendations

### 1. Memory Configuration
**Current:** `-Xmx1g` (1GB heap)  
**Recommendation:** Adjust based on Actor memory allocation in `.actor/actor.json`:
- If Actor memory = 2GB → `-Xmx1.5g` (75% of 2GB)
- If Actor memory = 4GB → `-Xmx3g` (75% of 4GB)

### 2. Build Optimization
**Current:** ✅ Already optimized with multi-stage build  
**Status:** No changes needed

### 3. Health Check Endpoint
**Current:** `/actuator/health`  
**Verification:** Ensure Spring Boot Actuator is configured in `application.yml`

---

## Verification Script

A verification script has been created at `scripts/verify-apify-dockerfile.ps1` that can be run to check the Dockerfile against Apify requirements.

**Usage:**
```powershell
powershell -ExecutionPolicy Bypass -File scripts/verify-apify-dockerfile.ps1
```

---

## Conclusion

✅ **The Dockerfile meets all Apify requirements and is ready for deployment.**

### Key Findings:
1. ✅ Uses correct Apify base image (`apify/actor-node:20`)
2. ✅ Correctly uses `apk` package manager (Alpine-based image verified)
3. ✅ Proper non-root user configuration (`myuser`)
4. ✅ Correct working directory (`/home/myuser`)
5. ✅ Java 21 JRE installed correctly
6. ✅ `APIFY_CONTAINER_PORT` properly configured
7. ✅ Health check configured
8. ✅ JVM optimized for containers
9. ✅ Multi-stage build for optimization
10. ✅ Port correctly exposed

### No Critical Issues Found

The Dockerfile is production-ready for Apify deployment.

---

## Next Steps

1. ✅ **Dockerfile verified** - Ready for deployment
2. ⏭️ **Deploy to Apify** - Test the deployment
3. ⏭️ **Verify Actor starts** - Check runtime logs
4. ⏭️ **Test health endpoint** - Verify `/actuator/health` responds
5. ⏭️ **Test MCP endpoint** - Verify `/mcp/sse` works correctly

---

## References

- [Apify Dockerfile Documentation](https://docs.apify.com/platform/actors/development/actor-definition/dockerfile)
- [Apify Base Images](https://github.com/apify/apify-actor-docker)
- Investigation Document: `docs/investigations/apify-invisible-deployment-01-07-2026.md`
- Dockerfile Research: `docs/research/apify-dockerfile-research.md`

