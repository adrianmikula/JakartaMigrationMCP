# Railway Deployment Guide for Jakarta Migration MCP

## Overview

Railway can auto-detect Spring Boot applications, but you need to configure it correctly for streamable-http MCP transport.

## Auto-Detection

Railway will automatically detect your Spring Boot app because:
- ✅ `build.gradle.kts` is present
- ✅ Spring Boot plugin is configured
- ✅ Railway uses Nixpacks which detects Java/Gradle projects

## Required Configuration

### 1. Environment Variables

Set these in Railway dashboard:

| Variable | Value | Required |
|----------|-------|----------|
| `SPRING_PROFILES_ACTIVE` | `mcp-streamable-http` | ✅ **Critical** |
| `PORT` | (Auto-set by Railway) | ✅ Auto-provided |
| `MCP_STREAMABLE_HTTP_PORT` | (Optional, uses PORT if not set) | ❌ Optional |

**Note**: Railway automatically provides `PORT` environment variable. The app is configured to use `PORT` first, then fall back to `MCP_STREAMABLE_HTTP_PORT`, then default to 8080.

### 2. Build Configuration

Railway will automatically:
- ✅ Detect Gradle project
- ✅ Run `./gradlew bootJar` (or use `railway.json` if present)
- ✅ Find the JAR in `build/libs/`

### 3. Start Command

Railway will try to auto-detect the start command. You can either:

**Option A: Let Railway auto-detect** (simplest)
- Railway will run: `java -jar build/libs/jakarta-migration-mcp-*.jar`
- **But**: You must set `SPRING_PROFILES_ACTIVE=mcp-streamable-http` env var

**Option B: Use `railway.json`** (recommended)
- Railway will use the start command from `railway.json`
- Ensures the correct profile is always used

**Option C: Use Procfile** (alternative)
```
web: java -jar build/libs/jakarta-migration-mcp-*.jar --spring.profiles.active=mcp-streamable-http
```

## Deployment Steps

### Step 1: Connect Repository

1. Go to https://railway.app
2. Sign up/login with GitHub
3. **New Project** → **Deploy from GitHub**
4. Select your `JakartaMigrationMCP` repository

### Step 2: Configure Environment Variables

1. In Railway project, go to **Variables** tab
2. Add required variables:
   ```
   SPRING_PROFILES_ACTIVE=mcp-streamable-http
   LICENSE_API_SERVER_API_KEY=your-generated-api-key-here
   ```
3. Railway automatically provides `PORT` (don't set it manually)

**Generate API Key**:
```bash
# Using OpenSSL
openssl rand -hex 32

# Using PowerShell
[Convert]::ToBase64String((1..32 | ForEach-Object { Get-Random -Minimum 0 -Maximum 256 }))
```

**See [RAILWAY_ENVIRONMENT_VARIABLES.md](RAILWAY_ENVIRONMENT_VARIABLES.md) for complete list of environment variables.**

### Step 3: Deploy

1. Railway will automatically:
   - Detect Gradle project
   - Run `./gradlew bootJar`
   - Start the application
2. Check **Deployments** tab for build logs
3. Wait for deployment to complete

### Step 4: Verify

1. Railway provides a URL like: `https://jakarta-migration-mcp.railway.app`
2. Test MCP endpoint:
   ```bash
   curl -X POST https://jakarta-migration-mcp.railway.app/mcp/streamable-http \
     -H "Content-Type: application/json" \
     -d '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}'
   ```
3. Expected: JSON response with all 6 Jakarta Migration tools

## Port Configuration

Railway provides a `PORT` environment variable that changes on each deployment. The app is configured to:

1. **First**: Check `PORT` (Railway's variable)
2. **Second**: Check `MCP_STREAMABLE_HTTP_PORT` (custom variable)
3. **Default**: Use 8080

This is handled in `application-mcp-streamable-http.yml`:
```yaml
server:
  port: ${PORT:${MCP_STREAMABLE_HTTP_PORT:8080}}
```

## Troubleshooting

### Problem: App starts but MCP endpoint not accessible

**Solution**: Check that `SPRING_PROFILES_ACTIVE=mcp-streamable-http` is set

### Problem: Port binding error

**Solution**: Railway's `PORT` variable is automatically used. Don't set `MCP_STREAMABLE_HTTP_PORT` manually.

### Problem: Build fails

**Solution**: 
- Check Railway build logs
- Ensure Java 21 is available (Railway auto-detects from `build.gradle.kts`)
- Verify Gradle wrapper is present

### Problem: Wrong profile active

**Solution**: 
- Verify `SPRING_PROFILES_ACTIVE` env var is set correctly
- Or use `railway.json` to specify start command with profile

## Railway.json Configuration

If you want explicit control, create `railway.json`:

```json
{
  "$schema": "https://railway.app/railway.schema.json",
  "build": {
    "builder": "NIXPACKS",
    "buildCommand": "./gradlew bootJar"
  },
  "deploy": {
    "startCommand": "java -jar build/libs/jakarta-migration-mcp-*.jar --spring.profiles.active=mcp-streamable-http",
    "restartPolicyType": "ON_FAILURE",
    "restartPolicyMaxRetries": 10
  }
}
```

This ensures:
- ✅ Correct build command
- ✅ Correct start command with profile
- ✅ Automatic restarts on failure

## Verification Checklist

After deployment, verify:

- [ ] Build succeeds in Railway logs
- [ ] App starts without errors
- [ ] Health endpoint works: `https://your-app.railway.app/actuator/health`
- [ ] MCP endpoint works: `https://your-app.railway.app/mcp/streamable-http`
- [ ] `tools/list` returns all 6 Jakarta Migration tools
- [ ] Railway URL is accessible

## Next Steps

1. ✅ Deploy to Railway
2. ⏭️ Update `glama.json` with Railway URL
3. ⏭️ Test MCP client connection
4. ⏭️ List on Glama.ai marketplace

## Resources

- [Railway Documentation](https://docs.railway.app/)
- [Railway Java/Gradle Guide](https://docs.railway.app/guides/java)
- [Spring Boot on Railway](https://docs.railway.app/guides/spring-boot)

