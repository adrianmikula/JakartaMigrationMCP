# Testing MCP Integration with IntelliJ AI Assistant

This guide explains how to test the Jakarta Migration MCP server integration with IntelliJ AI Assistant.

## Prerequisites

- IntelliJ IDEA 2023.3 or later
- AI Assistant enabled (JetBrains account required)
- Jakarta Migration plugin installed

## Building the Plugin

```bash
cd /path/to/JakartaMigrationMCP

# Build the plugin
./gradlew :intellij-plugin:buildPlugin

# The plugin ZIP will be at:
# intellij-plugin/build/distributions/jakarta-migration-intellij-*.zip
```

## Installing the Plugin

1. Open IntelliJ IDEA
2. Go to **Settings > Plugins**
3. Click the gear icon ⚙️
4. Select **Install Plugin from Disk...**
5. Navigate to the built ZIP file
6. Click **OK**
7. Restart IntelliJ

## Testing Steps

### 1. Verify Plugin Loaded

1. Open **Settings > Plugins**
2. Confirm "Jakarta Migration" is shown as enabled
3. Check **View > Tool Windows** - "Jakarta Migration" should be available

### 2. Check MCP Status

1. Open the Jakarta Migration tool window
2. Look at the bottom status panel:
   - **Connected** (green) - MCP server is initialized
   - **Initializing** (orange) - MCP is starting
   - **Not Available** (gray) - AI Assistant not active

### 3. Test AI Assistant Integration

1. Open the AI Assistant panel (usually right-side)
2. Try asking:

```
Analyze Jakarta migration readiness for this project
```

3. Expected behavior:
   - AI Assistant should recognize the request
   - Should invoke `analyzeJakartaReadiness` tool
   - Return migration analysis results

### 4. Test Specific Tools

Try these queries:

```
Detect migration blockers in this project
```

```
Recommend Jakarta EE versions for our dependencies
```

```
Scan javax.* imports in our source files
```

```
Generate a phased migration plan
```

### 5. Verify Tool Discovery

1. Open **Help > Diagnostic Tools > Debug Log Settings**
2. Add these loggers:
   ```
   #JakartaMcpServerProvider
   #JakartaMcpRegistrationActivity
   ```
3. Check `idea.log` for tool registration messages

## Expected Log Output

When the plugin loads successfully, you should see:

```
[JakartaMcpRegistrationActivity] Initializing Jakarta Migration MCP for project: MyProject
[JakartaMcpServerProvider] Initializing Jakarta Migration MCP Server Provider
[JakartaMcpServerProvider] Registered 9 MCP tools with IntelliJ AI Assistant
[JakartaMcpServerProvider] Available MCP tools:
[JakartaMcpServerProvider]   - analyzeJakartaReadiness: Analyzes a Java project's readiness...
[JakartaMcpServerProvider]   - analyzeMigrationImpact: Provides detailed analysis...
[JakartaMcpServerProvider]   - detectBlockers: Identifies migration blockers...
...
```

## Troubleshooting

### MCP Status Shows "Not Available"

**Cause**: AI Assistant is not active or not available in your IntelliJ edition.

**Solution**:
- Ensure you're logged into JetBrains Account
- Check that AI Assistant is enabled in **Settings > AI Assistant**
- Note: Some IntelliJ editions may not include AI Assistant

### No Tools Appearing

**Cause**: Plugin not properly loaded.

**Solution**:
- Restart IntelliJ after plugin installation
- Check plugin is enabled in Settings
- Review logs for errors

### Tools Return Errors

**Cause**: MCP server not running or misconfigured.

**Solution**:
- Ensure MCP server endpoint is accessible
- Check network/firewall settings
- Verify server URL configuration

## Testing Without AI Assistant

If AI Assistant isn't available, you can test the MCP server directly:

```bash
# Start the MCP server
java -jar jakarta-migration-mcp-server.jar --transport streamable-http --port 8080

# Test tool listing
curl http://localhost:8080/mcp/tools

# Test a tool
curl -X POST http://localhost:8080/mcp/tools/analyzeJakartaReadiness \
  -H "Content-Type: application/json" \
  -d '{"projectPath": "/path/to/project"}'
```

## CI/Automated Testing

Run the plugin tests:

```bash
./gradlew :intellij-plugin:test
```

Run MCP integration tests:

```bash
./gradlew :intellij-plugin:test --tests "*Mcp*Test"
```
