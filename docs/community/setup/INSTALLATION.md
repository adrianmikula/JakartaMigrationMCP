# Jakarta Migration MCP Server - Installation Guide

**Purpose**: This guide is for **end users** who want to install and use the Jakarta Migration MCP Server with their MCP clients (Cursor, Claude Desktop, etc.).

**For Developers**: If you're developing or contributing to this project, see [MANUAL_SETUP.md](MANUAL_SETUP.md) for development environment setup.

This guide covers installing and configuring the Jakarta Migration MCP Server for different MCP clients.

## Quick Install

```bash
# Install via npm (recommended)
npm install -g @jakarta-migration/mcp-server

# Or use npx (no installation needed)
npx -y @jakarta-migration/mcp-server
```

## Prerequisites

- **Node.js 18+** - Required for the npm wrapper
- **Java 21+** - Required to run the JAR file
  - Download from [Adoptium](https://adoptium.net/) or [OpenJDK](https://openjdk.org/)
  - Verify installation: `java -version`

## Installation Methods

### Method 1: npm Global Install (Recommended)

```bash
npm install -g @jakarta-migration/mcp-server
```

The package will be installed globally and can be used directly.

### Method 2: npx (No Installation)

Use `npx` to run the server without installing:

```bash
npx -y @jakarta-migration/mcp-server
```

The `-y` flag automatically accepts the package download.

### Method 3: Local Development

If you're building from source:

```bash
# Build the JAR
./gradlew bootJar

# Run directly
java -jar build/libs/jakarta-migration-mcp-1.0.0-SNAPSHOT.jar --spring.main.web-application-type=none
```

## Configuration for MCP Clients

### Cursor

1. Open Cursor Settings (`Ctrl+,` or `Cmd+,`)
2. Navigate to **Features** → **MCP**
3. Click **"+ Add New MCP Server"**
4. Add this configuration:

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

**Alternative (if installed globally):**

```json
{
  "mcpServers": {
    "jakarta-migration": {
      "command": "jakarta-migration-mcp"
    }
  }
}
```

5. **Restart Cursor** for changes to take effect

### Claude Code (Anthropic)

1. Open Claude Code Settings
2. Navigate to **MCP** or **Model Context Protocol**
3. Add server configuration:

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

4. Restart Claude Code

### Google Antigravity

Antigravity uses a similar configuration format. Add to your Antigravity MCP configuration:

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

**Note:** Antigravity may have specific configuration file locations. Check the Antigravity documentation for the exact path.

## Environment Variables

You can customize the behavior using environment variables:

- `JAKARTA_MCP_VERSION` - Override the JAR version to download (default: from package.json)
- `GITHUB_REPO` - Override the GitHub repository (default: `your-org/JakartaMigrationMCP`)
- `JAVA_HOME` - Override Java installation path

## Troubleshooting

### Java Not Found

**Error:** `ERROR: Java is not installed or not in PATH.`

**Solution:**
1. Install Java 21+ from [Adoptium](https://adoptium.net/)
2. Verify installation: `java -version`
3. Ensure Java is in your PATH environment variable

### JAR Download Failed

**Error:** `Failed to download JAR: HTTP 404`

**Solution:**
1. Check that the GitHub release exists for the version
2. Verify the repository name in `GITHUB_REPO` environment variable
3. Build locally: `./gradlew bootJar` and use the local JAR

### Permission Denied (Linux/macOS)

**Error:** `Permission denied` when running the script

**Solution:**
```bash
chmod +x index.js
```

Or use `node index.js` directly.

### Windows Execution Policy

**Error:** PowerShell execution policy prevents running scripts

**Solution:**
```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

Or use `node index.js` directly.

## Manual JAR Download

If you prefer to download and run the JAR manually:

1. Download the JAR from GitHub Releases:
   ```
   https://github.com/your-org/JakartaMigrationMCP/releases
   ```

2. Run the JAR:
   ```bash
   java -jar jakarta-migration-mcp-1.0.0.jar --spring.main.web-application-type=none --spring.profiles.active=mcp
   ```

3. Configure MCP client to use:
   ```json
   {
     "command": "java",
     "args": ["-jar", "/path/to/jakarta-migration-mcp-1.0.0.jar", "--spring.main.web-application-type=none", "--spring.profiles.active=mcp"]
   }
   ```

## Verification

After installation, verify the server is working:

1. Check that the server starts without errors
2. In your MCP client, look for the Jakarta Migration tools:
   - `analyzeJakartaReadiness`
   - `detectBlockers`
   - `recommendVersions`
   - `createMigrationPlan`
   - `verifyRuntime`

3. Test with a simple command in your MCP client

## Updating

To update to the latest version:

```bash
# If installed globally
npm update -g @jakarta-migration/mcp-server

# If using npx, it will automatically use the latest version
```

## Uninstallation

```bash
npm uninstall -g @jakarta-migration/mcp-server
```

The cached JAR file will remain in:
- **Windows:** `%USERPROFILE%\AppData\jakarta-migration-mcp\`
- **Linux/macOS:** `~/.cache/jakarta-migration-mcp/`

You can manually delete this directory to free up space.

## Support

For issues, questions, or contributions:
- GitHub Issues: [Create an issue](https://github.com/your-org/JakartaMigrationMCP/issues)
- Documentation: See `README.md` and other docs in the `docs/` directory

