# Jakarta Migration MCP - Community Edition

**Open-source Jakarta EE migration tools under Apache License 2.0**

---

## Overview

The Jakarta Migration MCP provides AI-powered tools for migrating Java applications from `javax.*` to `jakarta.*` namespaces. This document describes the **Community Edition** features that are free to use.

### License

All Community Edition features are licensed under the **Apache License 2.0**. See [`LICENSE`](LICENSE) for details.

---

## Community Features (Free)

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
  "edition": "community",
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

**Input:**
- `projectPath` (required): Path to the project root directory

**Output:**
- List of blocking dependencies
- Blocker types and reasons
- Mitigation strategies
- Confidence scores

**Example Response:**
```json
{
  "status": "success",
  "edition": "community",
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
  "edition": "community",
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

## IntelliJ Plugin (Community)

The IntelliJ plugin provides a graphical interface for:

- **Project Analysis**: Scan projects for Jakarta migration issues
- **Dependency Visualization**: View dependency graphs and blockers
- **Quick Fixes**: Manual migration guidance
- **Progress Tracking**: Monitor migration status

### Installation

1. Open IntelliJ IDEA
2. Go to Settings → Plugins
3. Search for "Jakarta Migration"
4. Click Install

---

## Premium Features

Premium features require a JetBrains Marketplace subscription.

### Pricing

| Plan | Price | Savings |
|------|-------|---------|
| Monthly | $49/month | - |
| Yearly | $399/year | 17% |

### Premium Tools

| Tool | Description |
|------|-------------|
| `createMigrationPlan` | Generate comprehensive migration roadmaps |
| `analyzeMigrationImpact` | Full impact analysis with effort estimation |
| `verifyRuntime` | Runtime verification of migrated applications |
| `applyAutoFixes` | Automatic refactoring of source code |
| `executeMigrationPlan` | Execute complete migration in phases |

### Upgrade

To unlock premium features:

1. Purchase a subscription at [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/25558)
2. Enter your license key in plugin settings
3. Enjoy unlimited migrations!

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

## Comparison Table

| Feature | Community | Premium |
|---------|-----------|---------|
| `analyzeJakartaReadiness` | ✅ Free | ✅ Free |
| `detectBlockers` | ✅ Free | ✅ Free |
| `recommendVersions` | ✅ Free | ✅ Free |
| `createMigrationPlan` | ❌ | ✅ $49/mo |
| `analyzeMigrationImpact` | ❌ | ✅ $49/mo |
| `verifyRuntime` | ❌ | ✅ $49/mo |
| `applyAutoFixes` | ❌ | ✅ $49/mo |
| `executeMigrationPlan` | ❌ | ✅ $49/mo |
| Support | Community | Priority Support |

---

## Resources

- **GitHub**: https://github.com/adrianmikula/jakarta-migration-mcp
- **JetBrains Plugin**: https://plugins.jetbrains.com/plugin/25558
- **Documentation**: See [`docs/`](docs/) directory
- **Issues**: https://github.com/adrianmikula/jakarta-migration-mcp/issues

---

## License

Copyright 2026 Adrian Mikula

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
