# Jakarta Migration MCP Server

A Model Context Protocol (MCP) server that provides AI coding assistants with specialized tools for analyzing and migrating Java applications from Java EE 8 (`javax.*`) to Jakarta EE 9+ (`jakarta.*`).

[![MCP Protocol](https://img.shields.io/badge/MCP-Protocol-green)](https://modelcontextprotocol.io)

## 🚀 Quick Start

### Run locally (STDIO)

**Prerequisites**: Java 21+ and Node.js 18+

```bash
# Install via npm (one-time)
npm install -g @jakarta-migration/mcp-server

# Or use with npx (no installation)
npx -y @jakarta-migration/mcp-server
```

See [Local Setup (STDIO)](#local-setup-stdio) below for client configuration.

## 📋 What It Does

The Jakarta Migration MCP Server enables your AI coding assistant to:

- **🔍 Analyze Jakarta Readiness** - Assess Java projects for migration readiness with detailed dependency analysis
- **🚫 Detect Blockers** - Identify dependencies and code patterns that prevent Jakarta migration
- **📦 Recommend Versions** - Suggest Jakarta-compatible versions for existing dependencies
- **📋 Create Migration Plans** - Generate comprehensive, phased migration plans with risk assessment
- **📊 Analyze Migration Impact** - Comprehensive impact analysis combining dependency analysis and source code scanning
- **✅ Verify Runtime** - Test migrated applications to ensure they run correctly after migration

### The Problem It Solves

Migrating from Java EE 8 (`javax.*`) to Jakarta EE 9+ (`jakarta.*`) is complex because:

- **Dependency Hell**: Many libraries haven't migrated, creating transitive conflicts
- **Binary Incompatibility**: Compiled JARs may reference `javax.*` internally
- **Hidden Dependencies**: `javax.*` usage in XML configs, annotations, and dynamic loading
- **Risk Assessment**: Need to understand migration impact before starting

This MCP server provides AI assistants with the specialized knowledge and tools to navigate these challenges effectively.

## 🔒 Security & Privacy

Your code and project data are handled with the utmost care. We understand that Java developers working with enterprise codebases need complete confidence in the security and privacy of their intellectual property.

### Stateless Architecture

✅ **No Data Persistence** - The service is completely stateless. Your project files, source code, and analysis results are never stored, logged, or persisted on our servers.

✅ **No Data Collection** - We don't collect, track, or analyze your code. Each request is processed independently with no memory of previous requests.

✅ **Local Execution Option** - For maximum privacy, you can run the entire service locally using the [Local Setup](#local-setup-stdio) option. Your code never leaves your machine.

### Privacy Guarantees

- **Zero Code Storage**: Project files are only read during analysis and immediately discarded
- **No Telemetry**: No usage tracking, analytics, or code scanning for any purpose other than migration analysis
- **Open Source**: The core service is open source, so you can audit exactly what it does
- **Enterprise Ready**: Safe for use with proprietary and sensitive codebases

### Local Service

When running locally via STDIO:
- **100% Local** - Everything runs on your machine
- **No Network Calls** - No external requests are made
- **Complete Control** - You have full visibility and control over the process

**For maximum security and privacy, we recommend using the local STDIO setup for sensitive projects.**

## 🔧 Setup Instructions

### Local Setup (STDIO)

For local development, use STDIO transport which works with **Cursor, Claude Code, and Antigravity**. This is the recommended approach for maximum privacy and performance.

#### Prerequisites

- **Java 21+** - [Download from Adoptium](https://adoptium.net/)
  - Verify installation: `java -version`
  - Should show Java 21 or higher
- **Node.js 18+** - [Download from nodejs.org](https://nodejs.org/)
  - Verify installation: `node --version`
  - Should show v18.0.0 or higher

#### Installation Methods

**Option 1: Global Install (Recommended)**

Install the package globally for system-wide access:

```bash
npm install -g @jakarta-migration/mcp-server
```

After installation:
- The JAR file will be automatically downloaded from GitHub releases on first use
- JAR is cached in your home directory for faster subsequent runs
- You can use the command directly: `jakarta-migration-mcp`

**Option 2: npx (No Installation)**

Use `npx` to run without installing:

```bash
npx -y @jakarta-migration/mcp-server
```

The `-y` flag automatically accepts the package download. The JAR will be downloaded and cached on first use.

**Option 3: Local Development Build**

If you're building from source or want to use a local JAR:

```bash
# Build the JAR
./gradlew bootJar

# Set environment variable to use local JAR
export JAKARTA_MCP_JAR_PATH=/path/to/build/libs/jakarta-migration-mcp-1.0.0-SNAPSHOT.jar

# Run via npm wrapper
npx -y @jakarta-migration/mcp-server
```

**Windows (PowerShell):**
```powershell
# Build the JAR
.\gradlew.bat bootJar

# Set environment variable
$env:JAKARTA_MCP_JAR_PATH = "E:\Source\JakartaMigrationMCP\build\libs\jakarta-migration-mcp-1.0.0-SNAPSHOT.jar"

# Run via npm wrapper
npx -y @jakarta-migration/mcp-server
```

#### How the npm Package Works

The npm package is a lightweight Node.js wrapper that:

1. **Downloads the JAR** from GitHub releases (if not already cached)
2. **Caches the JAR** in your home directory:
   - **Windows**: `%USERPROFILE%\AppData\.cache\jakarta-migration-mcp\`
   - **Linux/macOS**: `~/.cache/jakarta-migration-mcp/`
3. **Starts the Java process** with correct MCP arguments
4. **Handles stdio communication** for MCP protocol

**Pre-download the JAR:**

You can pre-download the JAR without starting the server:

```bash
npx -y @jakarta-migration/mcp-server --download-only
```

This is useful for:
- Testing the download process
- Pre-caching the JAR before first use
- Verifying network connectivity

**Verify Installation:**

After installing, verify everything works:

```bash
# Test the wrapper can find Java and download JAR
npx -y @jakarta-migration/mcp-server --download-only

# Check if command is available (if installed globally)
jakarta-migration-mcp --download-only
```

You should see:
- Java version detection
- JAR download or cache confirmation
- No errors

#### Optional configuration

To use a custom JAR path or override transport, see [NPM Installation Configuration](docs/setup/NPM_INSTALLATION_CONFIG.md).

##### Cursor IDE

1. Open Cursor Settings (`Ctrl+,` or `Cmd+,`)
2. Navigate to **Features** → **MCP**
3. Add configuration:

**Windows:**
```json
{
  "mcpServers": {
    "jakarta-migration": {
      "command": "npx",
      "args": ["-y", "@jakarta-migration/mcp-server"]
    }
  }
}
```

**Mac/Linux:**
```json
{
  "mcpServers": {
    "jakarta-migration": {
      "command": "npx",
      "args": ["-y", "@jakarta-migration/mcp-server"]
    }
  }
}
```

4. **Restart Cursor** completely

##### Claude Code (VS Code Extension)

1. Open VS Code Settings
2. Navigate to **Claude Code** → **MCP Settings**
3. Add the same configuration as Cursor
4. Restart VS Code

##### Antigravity

1. Open Antigravity Settings
2. Navigate to **MCP Configuration**
3. Add:

```json
{
  "name": "jakarta-migration",
  "command": "npx",
  "args": ["-y", "@jakarta-migration/mcp-server"]
}
```

#### Alternative: Run from JAR Directly

If you've built the project locally and want to bypass the npm wrapper:

**Windows:**
```json
{
  "mcpServers": {
    "jakarta-migration": {
      "command": "java",
      "args": [
        "-jar",
        "C:\\path\\to\\jakarta-migration-mcp-1.0.0-SNAPSHOT.jar",
        "--spring.profiles.active=mcp-stdio",
        "--spring.ai.mcp.server.transport=stdio",
        "--spring.main.web-application-type=none"
      ]
    }
  }
}
```

**Mac/Linux:**
```json
{
  "mcpServers": {
    "jakarta-migration": {
      "command": "java",
      "args": [
        "-jar",
        "/path/to/jakarta-migration-mcp-1.0.0-SNAPSHOT.jar",
        "--spring.profiles.active=mcp-stdio",
        "--spring.ai.mcp.server.transport=stdio",
        "--spring.main.web-application-type=none"
      ]
    }
  }
}
```

> **Note**: Using the npm wrapper is recommended as it handles JAR downloads, caching, and argument configuration automatically.

### Local HTTP Setup (Streamable HTTP or SSE)

For local HTTP-based testing or development:

1. **Build the project:**
   ```bash
   ./gradlew bootJar
   ```

2. **Start server with Streamable HTTP:**

   **Windows (PowerShell):**
   ```powershell
   java -jar build\libs\jakarta-migration-mcp-1.0.0-SNAPSHOT.jar --spring.profiles.active=mcp-streamable-http
   ```

   **Mac/Linux:**
   ```bash
   java -jar build/libs/jakarta-migration-mcp-1.0.0-SNAPSHOT.jar \
     --spring.profiles.active=mcp-streamable-http
   ```

   **Or with SSE (legacy):**

   **Windows (PowerShell):**
   ```powershell
   java -jar build\libs\jakarta-migration-mcp-1.0.0-SNAPSHOT.jar --spring.profiles.active=mcp-sse
   ```

   **Mac/Linux:**
   ```bash
   java -jar build/libs/jakarta-migration-mcp-1.0.0-SNAPSHOT.jar \
     --spring.profiles.active=mcp-sse
   ```

3. **Test the endpoint:**

   **Windows (PowerShell):**
   ```powershell
   # Streamable HTTP (recommended)
   curl.exe -X POST http://localhost:8080/mcp/streamable-http `
     -H "Content-Type: application/json" `
     -d '{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\",\"params\":{}}'
   
   # Or SSE (legacy)
   curl.exe -X POST http://localhost:8080/mcp/sse `
     -H "Content-Type: application/json" `
     -d '{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\",\"params\":{}}'
   ```

   **Mac/Linux:**
   ```bash
   # Streamable HTTP (recommended)
   curl -X POST http://localhost:8080/mcp/streamable-http \
     -H "Content-Type: application/json" \
     -d '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}'
   
   # Or SSE (legacy)
   curl -X POST http://localhost:8080/mcp/sse \
     -H "Content-Type: application/json" \
     -d '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}'
   ```

4. **Configure MCP client** to use `http://localhost:8080/mcp/streamable-http` (or `/mcp/sse` for SSE)

## 💬 Usage Examples

Once configured, you can use the MCP tools in your AI assistant:

### Analyze Project Readiness

```
Analyze the Jakarta readiness of my project at /path/to/my-project
```

### Detect Migration Blockers

```
Detect any blockers for Jakarta migration in my project
```

### Get Version Recommendations

```
Recommend Jakarta-compatible versions for my dependencies
```

### Create Migration Plan

```
Create a migration plan for migrating my project to Jakarta EE
```

### Verify Runtime

```
Verify the runtime of my migrated application at /path/to/app.jar
```

## 🛠️ Available Tools

| Tool | Description |
|------|-------------|
| `analyzeJakartaReadiness` | Comprehensive project analysis with readiness score |
| `detectBlockers` | Find dependencies and patterns that prevent migration |
| `recommendVersions` | Get Jakarta-compatible version recommendations |
| `createMigrationPlan` | Generate phased migration plan with risk assessment |
| `analyzeMigrationImpact` | Analyze full migration impact combining dependency analysis and source code scanning |
| `verifyRuntime` | Test migrated application execution |

See [MCP Tools Documentation](docs/mcp/MCP_TOOLS_IMPLEMENTATION.md) for detailed tool descriptions and parameters.

## 🐛 Troubleshooting

### Tools Not Appearing

1. **Restart your IDE completely** - MCP servers load on startup
2. **Check MCP server status** - Look for errors in IDE logs
3. **Verify configuration** - Ensure JSON syntax is correct
4. **Check prerequisites** - Java 21+ and Node.js 18+ must be installed

### Connection Issues

**For Local (STDIO):**
- Verify Java is installed: `java -version` (should show Java 21+)
- Verify Node.js is installed: `node --version` (should show v18+)
- Try running manually: `npx -y @jakarta-migration/mcp-server`
- Check JAR download: `npx -y @jakarta-migration/mcp-server --download-only`
- Verify JAR cache location:
  - **Windows**: `%USERPROFILE%\AppData\.cache\jakarta-migration-mcp\`
  - **Linux/macOS**: `~/.cache/jakarta-migration-mcp/`
- If JAR download fails, check:
  - Internet connectivity
  - GitHub releases are accessible
  - Version matches package.json version
- For local development, set `JAKARTA_MCP_JAR_PATH` environment variable to point to your local JAR file

### Platform-Specific Issues

**Windows:**
- Use forward slashes in paths: `C:/path/to/file.jar`
- Ensure Java is in your PATH

**Mac/Linux:**
- Ensure execute permissions: `chmod +x gradlew`
- Use absolute paths in configuration

## 📚 Documentation

### For Users

- **[Installation Guide](docs/setup/INSTALLATION.md)** - Build and installation
- **[MCP Tools Reference](docs/mcp/MCP_TOOLS_IMPLEMENTATION.md)** - Complete tool documentation
- **[Transport Configuration](docs/setup/MCP_TRANSPORT_CONFIGURATION.md)** - STDIO vs SSE explained

### For Developers

- **[Development Setup](docs/setup/INSTALLATION.md)** - Build and development environment
- **[Architecture](docs/architecture/core-modules-design.md)** - System design and modules
- **[Testing Guide](docs/testing/README.md)** - Testing standards and practices
- **[Contributing](CONTRIBUTING.md)** - How to contribute to the project

## 🔗 Resources

- **MCP Documentation**: [modelcontextprotocol.io](https://modelcontextprotocol.io)
- **Spring AI**: [docs.spring.io/spring-ai](https://docs.spring.io/spring-ai/reference/)
- **Jakarta EE**: [jakarta.ee](https://jakarta.ee/)

## 📄 License

Apache License 2.0 - See [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

Built with ❤️ for the Java community. Special thanks to:
- Spring AI team for MCP framework
- OpenRewrite for migration recipes

---

**Need help?** [Open an issue](https://github.com/your-repo/issues) or check our [documentation](docs/).
