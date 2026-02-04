import "@typespec/http";
import "@typespec/rest";
import "@typespec/openapi3";
import "./intellij-plugin-ui.tsp";
import "./mcp-integration.tsp";
import "./plugin-components.tsp";

using TypeSpec.Http;
using TypeSpec.Rest;
using TypeSpec.OpenAPI;

@service({
  title: "Jakarta Migration IntelliJ Plugin API",
  version: "1.0.0",
  description: "Complete API specification for the Jakarta Migration IntelliJ Plugin, including UI components, MCP integration, and data models"
})
@info({
  contact: {
    name: "Jakarta Migration Team",
    email: "support@jakarta-migration.com"
  },
  license: {
    name: "Apache 2.0",
    url: "https://www.apache.org/licenses/LICENSE-2.0"
  }
})
@doc("""
# Jakarta Migration IntelliJ Plugin API Specification

This TypeSpec specification defines the complete API and data models for the Jakarta Migration IntelliJ Plugin.

## Overview

The Jakarta Migration IntelliJ Plugin provides developers with tools to analyze and migrate Java applications from Java EE 8 (`javax.*`) to Jakarta EE 9+ (`jakarta.*`). The plugin integrates with a Model Context Protocol (MCP) server to provide AI-powered migration assistance.

## Key Components

### 1. UI Components (`plugin-components.tsp`)
- **MigrationToolWindow**: Main tool window with tabbed interface
- **DashboardComponent**: Overview of migration status and readiness
- **DependenciesTableComponent**: Table showing affected dependencies
- **DependencyGraphComponent**: Visual graph of module dependencies
- **MigrationPhasesComponent**: Migration phases and progress tracking

### 2. MCP Integration (`mcp-integration.tsp`)
- **McpClientService**: Interface for communicating with MCP server
- **Analysis Operations**: Jakarta readiness, blocker detection, impact analysis
- **Migration Operations**: Plan creation, execution, automatic fixes
- **Version Recommendations**: Jakarta-compatible dependency versions

### 3. Data Models (`intellij-plugin-ui.tsp`)
- **MigrationDashboard**: Dashboard data model
- **DependencyInfo**: Individual dependency information
- **MigrationPhase**: Migration phase and task definitions
- **DependencyGraph**: Module dependency visualization data

## Architecture

```
IntelliJ Plugin
├── UI Components (Swing/IntelliJ Platform)
│   ├── Tool Window with Tabs
│   ├── Tables with Filtering/Sorting
│   ├── Dependency Graph Visualization
│   └── Progress Tracking
├── MCP Client Service
│   ├── HTTP/SSE Communication
│   ├── Request/Response Handling
│   └── Error Management
└── Data Models
    ├── Migration Analysis Results
    ├── Dependency Information
    └── Migration Plans
```

## Usage Flow

1. **Project Analysis**: Plugin analyzes project using MCP server
2. **Dashboard Display**: Shows readiness score and summary
3. **Dependency Review**: User reviews affected dependencies in table
4. **Graph Visualization**: User explores module dependencies
5. **Migration Planning**: System creates phased migration plan
6. **Execution**: User executes migration phases with progress tracking

## Integration Points

- **IntelliJ Platform**: Tool windows, actions, notifications
- **MCP Server**: Analysis and migration operations
- **Project Files**: Reading build files, source code analysis
- **Version Control**: Integration with Git for change tracking
""")
namespace JakartaMigrationPlugin;

// Re-export all models for easy access
alias Dashboard = JakartaMigrationIntellijPlugin.MigrationDashboard;
alias DependencyInfo = JakartaMigrationIntellijPlugin.DependencyInfo;
alias MigrationPhase = JakartaMigrationIntellijPlugin.MigrationPhase;
alias DependencyGraph = JakartaMigrationIntellijPlugin.DependencyGraph;

alias McpClient = JakartaMigrationMcpIntegration.McpClientService;
alias AnalysisRequest = JakartaMigrationMcpIntegration.AnalyzeMigrationImpactRequest;
alias AnalysisResponse = JakartaMigrationMcpIntegration.AnalyzeMigrationImpactResponse;

alias ToolWindow = JakartaMigrationPluginComponents.MigrationToolWindow;
alias DashboardComponent = JakartaMigrationPluginComponents.DashboardComponent;
alias DependenciesTable = JakartaMigrationPluginComponents.DependenciesTableComponent;
alias DependencyGraphComponent = JakartaMigrationPluginComponents.DependencyGraphComponent;
alias MigrationPhasesComponent = JakartaMigrationPluginComponents.MigrationPhasesComponent;
alias PluginSettings = JakartaMigrationPluginComponents.PluginSettings;