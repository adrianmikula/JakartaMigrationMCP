# Jakarta Migration MCP Server

A Model Context Protocol (MCP) server that provides AI coding assistants with specialized tools for analyzing and migrating Java applications from Java EE 8 (`javax.*`) to Jakarta EE 9+ (`jakarta.*`).

[![Apify Store](https://img.shields.io/badge/Apify-Store-blue)](https://apify.com/adrian_m/jakartamigrationmcp)
[![MCP Protocol](https://img.shields.io/badge/MCP-Protocol-green)](https://modelcontextprotocol.io)

## 🚀 Quick Start

### Option 1: Use Apify Hosted Server (Recommended)

The easiest way to get started is using our **hosted MCP server on Apify** - no installation required!

**Free Tier Available**: Basic features are free. [Get started →](https://apify.com/adrian_m/jakartamigrationmcp)

**MCP Server URL**: `https://mcp.apify.com/?tools=actors,docs,adrian_m/JakartaMigrationMCP`

**Configuration Options**:

**Option A: Streamable HTTP (Recommended)** - Simpler, more reliable:
```json
{
  "mcpServers": {
    "jakarta-migration": {
      "type": "streamable-http",
      "url": "https://mcp.apify.com/?tools=actors,docs,adrian_m/JakartaMigrationMCP"
    }
  }
}
```

**Option B: SSE (Legacy)** - For clients that don't support Streamable HTTP yet:
```json
{
  "mcpServers": {
    "jakarta-migration": {
      "type": "sse",
      "url": "https://mcp.apify.com/?tools=actors,docs,adrian_m/JakartaMigrationMCP"
    }
  }
}
```

> **💡 Why Streamable HTTP?** Streamable HTTP is simpler, more reliable, and recommended by the MCP spec. SSE is deprecated but still supported for backward compatibility.

> **🔑 Authentication**: You'll need an Apify API token. Get it from [Apify Console → Integrations](https://console.apify.com/account#/integrations). Some MCP clients may prompt you to authenticate via OAuth.

See [Apify Setup](#apify-hosted-server-setup) below for detailed instructions.

### Option 2: Run Locally (STDIO)

For local development or when you need full control:

**Prerequisites**: Java 21+ and Node.js 18+

```bash
# Install via npm (one-time)
npm install -g @jakarta-migration/mcp-server

# Or use with npx (no installation)
npx -y @jakarta-migration/mcp-server
```

See [Local Setup](#local-setup-stdio) below for client configuration.

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

### Hosted Service (Apify)

When using the Apify-hosted service:
- Analysis is performed in isolated, ephemeral containers
- All containers are destroyed immediately after processing
- No persistent storage is used
- Your API token is only used for authentication and billing

### Local Service

When running locally via STDIO:
- **100% Local** - Everything runs on your machine
- **No Network Calls** - No external requests are made
- **Complete Control** - You have full visibility and control over the process

**For maximum security and privacy, we recommend using the local STDIO setup for sensitive projects.**

## 💰 Pricing & Features

### Free Tier (Community)

✅ **All core features included:**
- Dependency analysis
- Blocker detection
- Version recommendations
- Migration planning
- Runtime verification

### Premium Features (Paid)

🚀 **Advanced capabilities (coming soon):**
- **Auto-Fixes** - Automatically fix detected issues without manual intervention
- **One-Click Refactor** - Execute complete Jakarta migration with a single command
- **Binary Fixes** - Fix issues in compiled binaries and JAR files
- **Advanced Analysis** - Deep transitive conflict detection and resolution
- **Batch Operations** - Process multiple projects simultaneously
- **Custom Recipes** - Create and use custom migration recipes
- **API Access** - Programmatic API for CI/CD integrations

**Pricing**: Starting from $0.01 / 1,000 results. [View pricing →](https://apify.com/adrian_m/jakartamigrationmcp#pricing)

## 🔧 Setup Instructions

### Apify Hosted Server Setup

The Apify-hosted server supports **both Streamable HTTP and SSE transports**.

**MCP Server URL**: `https://mcp.apify.com/?tools=actors,docs,adrian_m/JakartaMigrationMCP`  
**Actor Page**: [apify.com/adrian_m/jakartamigrationmcp](https://apify.com/adrian_m/jakartamigrationmcp)

> **🔑 Authentication Required**: You'll need an Apify API token. Get it from [Apify Console → Integrations](https://console.apify.com/account#/integrations). Some MCP clients support OAuth authentication which will prompt you to sign in.

> **💡 Transport Recommendation**: Use **Streamable HTTP** (recommended) for better reliability and simplicity. SSE is available for backward compatibility but is deprecated in the MCP spec.

#### For Cursor IDE

> **⚠️ Important**: Cursor primarily supports **stdio transport**, not SSE. For Cursor, use the [Local Setup (STDIO)](#local-setup-stdio) instructions below instead.

If your Cursor version supports HTTP transports:
1. Open Cursor Settings (`Ctrl+,` on Windows/Linux or `Cmd+,` on Mac)
2. Navigate to **Features** → **MCP**
3. Click **"+ Add New MCP Server"**
4. Add configuration (prefer Streamable HTTP):

**Streamable HTTP (Recommended):**
```json
{
  "mcpServers": {
    "jakarta-migration": {
      "type": "streamable-http",
      "url": "https://mcp.apify.com/?tools=actors,docs,adrian_m/JakartaMigrationMCP",
      "headers": {
        "Authorization": "Bearer YOUR_APIFY_API_TOKEN"
      }
    }
  }
}
```

**Or SSE (Legacy):**
```json
{
  "mcpServers": {
    "jakarta-migration": {
      "type": "sse",
      "url": "https://mcp.apify.com/?tools=actors,docs,adrian_m/JakartaMigrationMCP",
      "headers": {
        "Authorization": "Bearer YOUR_APIFY_API_TOKEN"
      }
    }
  }
}
```

> **🔑 Get API Token**: Replace `YOUR_APIFY_API_TOKEN` with your token from [Apify Console → Integrations](https://console.apify.com/account#/integrations)

5. **Restart Cursor** completely for changes to take effect

#### For Claude Code (VS Code Extension)

1. Open VS Code Settings (`Ctrl+,` or `Cmd+,`)
2. Search for "MCP" or navigate to **Extensions** → **Claude Code** → **Settings**
3. Add configuration (prefer Streamable HTTP):

**Streamable HTTP (Recommended):**
```json
{
  "mcpServers": {
    "jakarta-migration": {
      "type": "streamable-http",
      "url": "https://mcp.apify.com/?tools=actors,docs,adrian_m/JakartaMigrationMCP",
      "headers": {
        "Authorization": "Bearer YOUR_APIFY_API_TOKEN"
      }
    }
  }
}
```

**Or SSE (Legacy):**
```json
{
  "mcpServers": {
    "jakarta-migration": {
      "type": "sse",
      "url": "https://mcp.apify.com/?tools=actors,docs,adrian_m/JakartaMigrationMCP",
      "headers": {
        "Authorization": "Bearer YOUR_APIFY_API_TOKEN"
      }
    }
  }
}
```

> **🔑 Get API Token**: Replace `YOUR_APIFY_API_TOKEN` with your token from [Apify Console → Integrations](https://console.apify.com/account#/integrations)

4. Restart VS Code

#### For GitHub Copilot

GitHub Copilot supports MCP through the Copilot Chat interface:

1. Open Copilot Chat (`Ctrl+L` or `Cmd+L`)
2. Navigate to **Settings** → **MCP Servers**
3. Add server:
   - **URL**: `https://mcp.apify.com/?tools=actors,docs,adrian_m/JakartaMigrationMCP`
   - **Transport Type**: **Streamable HTTP** (recommended) or **SSE** (legacy)
   - **Authorization**: Add your Apify API token in the headers section

> **🔑 Get API Token**: Get your token from [Apify Console → Integrations](https://console.apify.com/account#/integrations)

#### For Antigravity

1. Open Antigravity Settings
2. Navigate to **MCP Configuration**
3. Add server (prefer Streamable HTTP):

**Streamable HTTP (Recommended):**
```json
{
  "name": "jakarta-migration",
  "type": "streamable-http",
  "url": "https://mcp.apify.com/?tools=actors,docs,adrian_m/JakartaMigrationMCP",
  "headers": {
    "Authorization": "Bearer YOUR_APIFY_API_TOKEN"
  }
}
```

**Or SSE (Legacy):**
```json
{
  "name": "jakarta-migration",
  "type": "sse",
  "url": "https://mcp.apify.com/?tools=actors,docs,adrian_m/JakartaMigrationMCP",
  "headers": {
    "Authorization": "Bearer YOUR_APIFY_API_TOKEN"
  }
}
```

> **🔑 Get API Token**: Replace `YOUR_APIFY_API_TOKEN` with your token from [Apify Console → Integrations](https://console.apify.com/account#/integrations)

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

#### Configuration

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

**For Apify (SSE):**
- Verify your internet connection
- Check if Apify service is available
- Ensure you're using the correct URL

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

- **[MCP Setup Guide](docs/setup/MCP_SETUP.md)** - Detailed MCP configuration instructions
- **[MCP Tools Reference](docs/mcp/MCP_TOOLS_IMPLEMENTATION.md)** - Complete tool documentation
- **[Transport Configuration](docs/setup/MCP_TRANSPORT_CONFIGURATION.md)** - STDIO vs SSE explained

### For Developers

- **[Development Setup](docs/setup/INSTALLATION.md)** - Build and development environment
- **[Architecture](docs/architecture/core-modules-design.md)** - System design and modules
- **[Testing Guide](docs/testing/README.md)** - Testing standards and practices
- **[Contributing](CONTRIBUTING.md)** - How to contribute to the project

## 🔗 Resources

- **Apify Store**: [jakartamigrationmcp](https://apify.com/adrian_m/jakartamigrationmcp)
- **MCP Documentation**: [modelcontextprotocol.io](https://modelcontextprotocol.io)
- **Spring AI**: [docs.spring.io/spring-ai](https://docs.spring.io/spring-ai/reference/)
- **Jakarta EE**: [jakarta.ee](https://jakarta.ee/)

## 📄 License

MIT License - See [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

Built with ❤️ for the Java community. Special thanks to:
- Spring AI team for MCP framework
- OpenRewrite for migration recipes
- Apify for hosting infrastructure

---

**Need help?** [Open an issue](https://github.com/your-repo/issues) or check our [documentation](docs/).
