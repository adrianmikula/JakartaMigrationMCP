# Jakarta Migration MCP - Premium Edition

**Proprietary Jakarta EE migration tools**

---

## Overview

The Jakarta Migration MCP provides AI-powered tools for migrating Java applications from `javax.*` to `jakarta.*` namespaces.

### License

All features are proprietary. See [`LICENSE`](LICENSE) for details.

---

## Features

### MCP Tools

#### `analyzeJakartaReadiness`
Analyzes a Java project for Jakarta migration readiness.

**Input:**
- `projectPath` (required): Path to the project root directory

**Output:**
- Readiness score (0-1)
- Total dependencies analyzed
- Number of blockers found
- Risk assessment
- Recommendations

**Example Response:**
```json
{
  "status": "success",
  "edition": "premium",
  "readinessScore": 0.65,
  "readinessMessage": "Mostly ready, some issues to resolve",
  "totalDependencies": 42,
  "blockers": 3,
  "recommendations": 5,
  "riskScore": 0.3
}
```

---

#### `detectBlockers`
Detects blockers that prevent Jakarta migration.

**projectPath` (Input:**
- `required): Path to the project root directory

**Output:**
- List of blocking dependencies
- Blocker types and reasons
- Mitigation strategies
- Confidence scores

**Example Response:**
```json
{
  "status": "success",
  "edition": "premium",
  "blockerCount": 2,
  "blockers": [
    {
      "artifact": "javax.servlet:javax.servlet-api:3.1.0",
      "type": "NO_JAKARTA_EQUIVALENT",
      "reason": "No Jakarta equivalent found",
      "confidence": 0.95,
      "mitigationStrategies": [
        "Replace with jakarta.servlet:jakarta.servlet-api:6.0.0"
      ]
    }
  ]
}
```

---

#### `recommendVersions`
Recommends Jakarta-compatible versions for project dependencies.

**Input:**
- `projectPath` (required): Path to the project root directory

**Output:**
- Current artifact coordinates
- Recommended Jakarta artifact coordinates
- Migration path
- Compatibility scores
- Breaking changes

**Example Response:**
```json
{
  "status": "success",
  "edition": "premium",
  "recommendationCount": 5,
  "recommendations": [
    {
      "current": "javax.servlet:javax.servlet-api:3.1.0",
      "recommended": "jakarta.servlet:jakarta.servlet-api:6.0.0",
      "migrationPath": "Update dependency coordinates and imports",
      "compatibilityScore": 0.95,
      "breakingChanges": []
    }
  ]
}
```

---

#### `createMigrationPlan`
Generate comprehensive migration roadmaps with phased execution.

**Input:**
- `projectPath` (required): Path to the project root directory
- `targetVersion` (required): Jakarta EE version (e.g., "9", "10", "11")

**Output:**
- Phased migration plan
- Estimated duration
- Risk assessment
- Safety levels per phase

---

#### `analyzeMigrationImpact`
Full impact analysis with effort estimation and change tracking.

**Input:**
- `projectPath` (required): Path to the project root directory
- `includeTests` (optional): Include test files in analysis

**Output:**
- Impact summary
- Files affected
- Estimated effort (hours)
- Risk score

---

#### `verifyRuntime`
Runtime verification of migrated applications.

**Input:**
- `jarPath` (required): Path to compiled JAR file
- `timeoutSeconds` (optional): Timeout for verification

**Output:**
- Verification status
- Jakarta EE references found
- Execution metrics
- Error analysis

---

#### `applyAutoFixes`
Automatic refactoring of source code using OpenRewrite.

**Input:**
- `projectPath` (required): Path to the project root directory
- `recipes` (optional): Specific recipes to apply

**Output:**
- Files modified
- Changes applied
- Summary of refactorings

---

#### `executeMigrationPlan`
Execute complete migration in phases.

**Input:**
- `projectPath` (required): Path to the project root directory
- `planId` (required): Migration plan ID
- `phase` (optional): Specific phase to execute

**Output:**
- Execution status
- Changes applied
- Verification results

---

## IntelliJ Plugin

The IntelliJ plugin provides a graphical interface for:

- **Project Analysis**: Scan projects for Jakarta migration issues
- **Dependency Visualization**: View dependency graphs and blockers
- **Quick Fixes**: Manual and automated migration guidance
- **Progress Tracking**: Monitor migration status
- **Premium Features**: Full access to all 8 migration tools

### Installation

1. Open IntelliJ IDEA
2. Go to Settings → Plugins
3. Search for "Jakarta Migration"
4. Click Install
5. Enter license key (purchased separately)

---

## Pricing

| Plan | Price |
|------|-------|
| Monthly | $49/month |
| Yearly | $399/year |

---

## System Requirements

- Java 17 or later
- Maven 3.6+ or Gradle 7+
- 512MB RAM minimum

---

## Getting Started

### Quick Start

```bash
# Clone the repository
git clone https://github.com/adrianmikula/jakarta-migration-mcp.git
cd jakarta-migration-mcp

# Build the project
./gradlew build

# Run the MCP server
./gradlew :mcp-server:run
```

### Configure in AI Assistant

Add to your `cursor.json` or `claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "jakarta-migration": {
      "command": "java",
      "args": ["-jar", "jakarta-migration-mcp.jar"],
      "env": {}
    }
  }
}
```

---

## Purchase

- **JetBrains Marketplace**: https://plugins.jetbrains.com/plugin/25558
- **Contact**: adrian.mikula@outlook.com

---

## License

Copyright 2026 Adrian Kozala

This software is proprietary and may not be used, copied, modified, or distributed
except under the terms of a separate commercial license agreement.

No open-source license applies to any part of this project.
