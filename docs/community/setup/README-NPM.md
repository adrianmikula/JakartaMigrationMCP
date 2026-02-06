# @jakarta-migration/mcp-server

MCP (Model Context Protocol) server for Jakarta EE migration analysis and automation. This tool helps analyze Java projects for Jakarta EE migration readiness, detect blockers, recommend compatible versions, and create migration plans.

## Installation

```bash
npm install -g @jakarta-migration/mcp-server
```

Or use with `npx` (no installation needed):

```bash
npx -y @jakarta-migration/mcp-server
```

## Prerequisites

- **Node.js 18+** - Required for the npm wrapper
- **Java 21+** - Required to run the JAR file
  - Download from [Adoptium](https://adoptium.net/) or [OpenJDK](https://openjdk.org/)
  - Verify: `java -version`

## Quick Start

### For Cursor

1. Open Cursor Settings (`Ctrl+,` or `Cmd+,`)
2. Navigate to **Features** → **MCP**
3. Add this configuration:

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

4. Restart Cursor

### For Claude Code

1. Open Claude Code Settings
2. Navigate to **MCP** settings
3. Add the same configuration as above
4. Restart Claude Code

### For Google Antigravity

Add the configuration to your Antigravity MCP settings file (location may vary).

## Features

The Jakarta Migration MCP Server provides the following tools:

- **analyzeJakartaReadiness** - Analyze a Java project for Jakarta migration readiness
- **detectBlockers** - Detect blockers that prevent Jakarta migration
- **recommendVersions** - Recommend Jakarta-compatible versions for dependencies
- **createMigrationPlan** - Create a comprehensive migration plan
- **verifyRuntime** - Verify runtime execution of migrated applications

## Usage

Once configured, you can use the MCP tools in your AI assistant:

```
Analyze the Jakarta readiness of my project at /path/to/project
```

```
Detect any blockers for Jakarta migration in my project
```

```
Create a migration plan for migrating to Jakarta EE
```

## Configuration

### Environment Variables

- `JAKARTA_MCP_VERSION` - Override the JAR version to download
- `GITHUB_REPO` - Override the GitHub repository
- `JAVA_HOME` - Override Java installation path

### Custom JAR Location

If you have a local JAR file, you can configure the MCP client to use it directly:

```json
{
  "mcpServers": {
    "jakarta-migration": {
      "command": "java",
      "args": [
        "-jar",
        "/path/to/jakarta-migration-mcp-1.0.0.jar",
        "--spring.main.web-application-type=none",
        "--spring.profiles.active=mcp"
      ]
    }
  }
}
```

## Troubleshooting

### Java Not Found

If you see `ERROR: Java is not installed or not in PATH`:

1. Install Java 21+ from [Adoptium](https://adoptium.net/)
2. Verify: `java -version`
3. Ensure Java is in your PATH

### JAR Download Failed

If the JAR download fails:

1. Check your internet connection
2. Verify the GitHub release exists
3. Build locally: `./gradlew bootJar`
4. Use the local JAR path in MCP configuration

### Permission Issues (Linux/macOS)

```bash
chmod +x index.js
```

Or use `node index.js` directly.

## Development

### Building from Source

```bash
# Clone the repository
git clone https://github.com/your-org/JakartaMigrationMCP.git
cd JakartaMigrationMCP

# Build the JAR
./gradlew bootJar

# Run locally
java -jar build/libs/jakarta-migration-mcp-1.0.0-SNAPSHOT.jar \
  --spring.main.web-application-type=none \
  --spring.profiles.active=mcp
```

### Testing the npm Package Locally

```bash
# Link the package locally
npm link

# Test in another directory
cd /tmp/test-project
npm link @jakarta-migration/mcp-server
npx jakarta-migration-mcp
```

## Documentation

For detailed documentation, see:
- [Installation Guide](docs/setup/INSTALLATION.md)
- [MCP Setup Guide](docs/setup/MCP_SETUP.md)
- [Architecture Documentation](docs/architecture/README.md)

## License

MIT

## Support

- GitHub Issues: [Create an issue](https://github.com/your-org/JakartaMigrationMCP/issues)
- Documentation: See `docs/` directory

