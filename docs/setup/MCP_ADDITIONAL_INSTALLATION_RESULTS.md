# Additional MCP Server Installation and Testing Results

**Date**: 2026-01-05  
**Purpose**: Install and test additional recommended MCP servers from MCP_SETUP.md

## Installation Summary

### ✅ Successfully Installed

1. **Maven Dependencies MCP Server** (`mcp-maven-deps`)
   - Status: ✅ Installed globally via npm
   - Version: 0.1.7
   - Purpose: Check Maven dependency versions
   - Alternative to: `@maven-tools/mcp-server` (not found on npm)

2. **Docker MCP Server** (`docker-mcp-server`)
   - Status: ✅ Installed globally via npm
   - Version: 2.1.1
   - Purpose: Docker container and image management
   - Alternative to: `@modelcontextprotocol/server-docker` (not found on npm)

3. **Architect MCP Server** (`@agiflowai/architect-mcp`)
   - Status: ✅ Installed globally via npm
   - Version: 1.0.15
   - Purpose: Software architecture design and planning
   - Alternative to: `@squirrelogic/mcp-architect` (not found on npm)

4. **PostgreSQL MCP Server** (`mcp-server-postgresql`)
   - Status: ✅ Installed globally via npm
   - Version: 3.0.0
   - Purpose: PostgreSQL database management
   - Alternative to: `@henkdz/postgresql-mcp-server` (not found on npm)

### ❌ Not Found on npm

The following packages listed in MCP_SETUP.md do not exist on npm:

1. **Maven Tools MCP Server** (`@maven-tools/mcp-server`)
   - Alternative installed: `mcp-maven-deps`

2. **Spring Initializr MCP Server** (`@antigravity/spring-initializr-mcp`)
   - Status: ❌ Not found on npm
   - Note: May need to be installed from source or use alternative CLI tools

3. **UML-MCP Server** (`@antoinebou12/uml-mcp`)
   - Status: ❌ Not found on npm
   - Note: May need to be installed from source (GitHub repository exists)

4. **NPM Plus MCP Server** (`@antigravity/npm-plus-mcp`)
   - Status: ❌ Not found on npm

5. **Docker MCP Server** (`@modelcontextprotocol/server-docker`)
   - Alternative installed: `docker-mcp-server`

6. **Architect MCP Server** (`@squirrelogic/mcp-architect`)
   - Alternative installed: `@agiflowai/architect-mcp`

7. **PostgreSQL MCP Server** (`@henkdz/postgresql-mcp-server`)
   - Alternative installed: `mcp-server-postgresql`

## Configuration Template

Add these to Cursor Settings → Features → MCP:

```json
{
  "mcpServers": {
    "maven-deps": {
      "command": "npx",
      "args": ["-y", "mcp-maven-deps"]
    },
    "docker": {
      "command": "npx",
      "args": ["-y", "docker-mcp-server"]
    },
    "architect": {
      "command": "npx",
      "args": ["-y", "@agiflowai/architect-mcp"]
    },
    "postgresql": {
      "command": "npx",
      "args": ["-y", "mcp-server-postgresql"],
      "env": {
        "POSTGRES_HOST": "localhost",
        "POSTGRES_PORT": "5432",
        "POSTGRES_DB": "your_database",
        "POSTGRES_USER": "your_user",
        "POSTGRES_PASSWORD": "your_password"
      }
    }
  }
}
```

## Testing Plan

### Test 1: Maven Dependencies MCP
- **Task**: Check Maven dependency versions in build.gradle.kts
- **Expected**: Query dependency information for Spring Boot, Gradle dependencies

### Test 2: Docker MCP
- **Task**: List running Docker containers
- **Expected**: Show container status, images, etc.

### Test 3: Architect MCP
- **Task**: Generate architecture diagram or analysis
- **Expected**: Create architecture documentation/analysis

### Test 4: PostgreSQL MCP
- **Task**: Query database schema (if database available)
- **Expected**: Show tables, schema information

## Package Availability Issues

Many packages documented in MCP_SETUP.md are not available on npm. This suggests:

1. **Documentation may be outdated** - Package names may have changed
2. **Packages may be private** - Some may require special access
3. **Packages may need to be built from source** - GitHub repositories may exist
4. **Alternative packages exist** - We found working alternatives for most

## Recommendations

1. **Update MCP_SETUP.md** with correct package names
2. **Use alternatives** we've identified where original packages don't exist
3. **Test installed servers** to verify functionality
4. **Consider building from source** for UML-MCP and Spring Initializr if needed

## Next Steps

1. Configure installed servers in Cursor
2. Test each server with trivial tasks
3. Document test results
4. Update MCP_SETUP.md with correct package names

