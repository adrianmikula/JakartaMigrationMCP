# MCP Tools for IntelliJ AI Assistant

This document describes the MCP (Model Context Protocol) tools exposed by the Jakarta Migration IntelliJ Plugin for use with the IntelliJ AI Assistant.

## Overview

The Jakarta Migration plugin exposes 9 MCP tools that the AI Assistant can discover and invoke to help with Jakarta EE migration tasks. Tools are automatically registered when the plugin is enabled.

## Tool Registration

### Automatic Loading

The MCP server is automatically loaded when:
1. The Jakarta Migration IntelliJ plugin is installed and enabled
2. A project is opened
3. The IntelliJ AI Assistant is available

### Registration Mechanism

The plugin uses two mechanisms for tool registration:

1. **ProjectActivity** (`JakartaMcpRegistrationActivity`): Initializes the MCP server provider when a project opens
2. **McpServerProvider Extension** (`JakartaMcpServerProvider`): Registers tools with the AI Assistant

## Available Tools

### 1. analyzeJakartaReadiness

Analyzes a project's readiness for migration from Java EE 8 (javax.*) to Jakarta EE 9+ (jakarta.*).

**Input:**
```json
{
  "projectPath": "/path/to/project",
  "includeTransitiveDependencies": true,
  "analysisLevel": "detailed"
}
```

**Output:** Readiness score, affected dependencies, blockers, recommendations

### 2. analyzeMigrationImpact

Provides detailed analysis of migration impact including affected dependencies, breaking changes, and estimated effort.

**Input:**
```json
{
  "projectPath": "/path/to/project",
  "scope": "all",
  "includeRiskAssessment": true,
  "outputFormat": "detailed"
}
```

**Output:** Impact report with risk assessment

### 3. detectBlockers

Identifies migration blockers that prevent successful Jakarta EE migration.

**Input:**
```json
{
  "projectPath": "/path/to/project",
  "severityLevel": "error"
}
```

**Output:** List of blocking issues with severity levels

### 4. recommendVersions

Analyzes project dependencies and recommends compatible Jakarta EE versions.

**Input:**
```json
{
  "projectPath": "/path/to/project",
  "includeAlternatives": true,
  "targetJakartaVersion": "10"
}
```

**Output:** Version recommendations with upgrade paths

### 5. applyOpenRewriteRefactoring

Applies OpenRewrite refactoring recipes to automatically migrate javax packages to jakarta equivalents.

**Input:**
```json
{
  "projectPath": "/path/to/project",
  "filePatterns": ["**/*.java"],
  "dryRun": false,
  "skipTests": true
}
```

**Output:** Refactoring results and changed files

### 6. scanBinaryDependency

Scans a compiled JAR dependency for Jakarta EE compatibility issues.

**Input:**
```json
{
  "jarPath": "/path/to/dependency.jar",
  "includeMethods": true,
  "outputDetail": "full"
}
```

**Output:** Compatibility report with problematic classes

### 7. updateDependency

Updates a single dependency to a recommended Jakarta-compatible version.

**Input:**
```json
{
  "projectPath": "/path/to/project",
  "groupId": "org.springframework",
  "artifactId": "spring-web",
  "currentVersion": "5.3.29",
  "recommendedVersion": "6.0.10",
  "updateStrategy": "preview"
}
```

**Output:** Update status and modified files

### 8. generateMigrationPlan

Generates a detailed, phased migration plan for Jakarta EE migration.

**Input:**
```json
{
  "projectPath": "/path/to/project",
  "phases": "multi",
  "includeRollback": true,
  "riskTolerance": "medium"
}
```

**Output:** Phased migration plan with tasks and milestones

### 9. validateMigration

Validates that migration was successful by running compile checks and test suites.

**Input:**
```json
{
  "projectPath": "/path/to/project",
  "validationType": "all",
  "customChecks": ["javax-import-check", "jakarta-class-check"]
}
```

**Output:** Validation report with pass/fail status

## Server Configuration

### Server Metadata

```json
{
  "id": "jakarta-migration-mcp",
  "name": "Jakarta Migration MCP",
  "version": "1.0.0",
  "description": "MCP server for Jakarta EE migration analysis and automation",
  "vendor": "Jakarta Migration Team",
  "url": "https://jakarta-migration.com",
  "autoLoad": true,
  "requiresUserConfirmation": false,
  "minIdeVersion": "2023.3",
  "supportedIdeVersions": ["2023.3", "2024.1", "2024.2", "2024.3"]
}
```

### Connection Configuration

```json
{
  "type": "streamable-http",
  "endpoint": "/mcp",
  "timeoutMs": 30000,
  "reconnectAttempts": 3,
  "reconnectDelayMs": 1000
}
```

### Capabilities

```json
{
  "tools": {
    "listChanged": true
  },
  "logging": {
    "level": ["debug", "info", "warning", "error"]
  }
}
```

## IDE Integration

### Plugin Dependencies

The plugin requires:
- `com.intellij.modules.platform` (core platform)
- `com.intellij.modules.java` (Java support)
- `com.intellij.modules.ai` (optional, for AI Assistant integration)

### Extension Points

1. **projectActivity**: `JakartaMcpRegistrationActivity`
   - Initializes MCP on project open
   - Auto-loads when plugin is enabled

2. **service**: `DefaultMcpClientService`
   - Provides MCP client operations
   - Implements `McpClientService` interface

3. **mcpServerProvider**: `JakartaMcpServerProvider`
   - Registers tools with AI Assistant
   - Provides tool definitions and metadata

### Notification Group

- `JakartaMigration.McpStatus`: Displays MCP server status notifications

## Troubleshooting

### Tools Not Appearing

1. Verify plugin is enabled: **Settings > Plugins > Jakarta Migration**
2. Check IDE version: Requires 2023.3 or later
3. Enable AI Assistant: **Settings > AI Assistant > Enable**
4. Check logs: `idea.log` with tag "JakartaMcpServerProvider"

### Server Connection Issues

1. Verify MCP server is running
2. Check network connectivity
3. Review timeout settings in connection configuration
4. Enable debug logging for detailed diagnostics

### Performance Issues

1. Limit analysis scope with `analysisLevel` parameter
2. Use `dryRun` mode for previews
3. Exclude test files with `skipTests` option

## Support

- Plugin Repository: https://github.com/adrianmikula/JakartaMigrationMCP
- Issue Tracker: https://github.com/adrianmikula/JakartaMigrationMCP/issues
- Documentation: https://jakarta-migration.com/docs
