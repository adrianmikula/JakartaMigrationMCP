# Jakarta Migration IntelliJ Plugin TypeSpec Specifications

This directory contains TypeSpec specifications that define the requirements, data models, and API interfaces for the Jakarta Migration IntelliJ Plugin UI components.

## 📁 File Structure

```
spec/
├── main.tsp                    # Main entry point with service definition
├── intellij-plugin-ui.tsp     # Core UI data models and enums
├── mcp-integration.tsp         # MCP server integration interfaces
├── plugin-components.tsp       # IntelliJ plugin UI components
├── tspconfig.yaml             # TypeSpec compiler configuration
└── README.md                  # This file
```

## 📋 Specifications Overview

### 1. `main.tsp` - Service Definition
- Main entry point for the TypeSpec specification
- Service metadata and documentation
- Re-exports all models for easy access
- Complete API overview and architecture documentation

### 2. `intellij-plugin-ui.tsp` - Core Data Models
Defines the fundamental data structures for the plugin:

- **`MigrationDashboard`** - Dashboard overview data
- **`DependencyInfo`** - Individual dependency information
- **`MigrationPhase`** - Migration phases and tasks
- **`DependencyGraph`** - Module dependency visualization
- **Enums**: `MigrationStatus`, `RiskLevel`, `DependencyMigrationStatus`, etc.

### 3. `mcp-integration.tsp` - MCP Server Integration
Defines interfaces for communicating with the Jakarta Migration MCP server:

- **`McpClientService`** - Main service interface
- **Analysis Operations**:
  - `analyzeJakartaReadiness` - Project readiness assessment
  - `detectBlockers` - Migration blocker identification
  - `analyzeMigrationImpact` - Comprehensive impact analysis
- **Migration Operations**:
  - `createMigrationPlan` - Generate migration plans
  - `executeMigrationPlan` - Execute migration phases
  - `applyAutoFixes` - Apply automatic fixes
- **Request/Response Models** for all operations

### 4. `plugin-components.tsp` - UI Components
Defines IntelliJ plugin UI components and interactions:

- **`MigrationToolWindow`** - Main tool window container
- **`DashboardComponent`** - Migration overview dashboard
- **`DependenciesTableComponent`** - Dependencies table with filtering/sorting
- **`DependencyGraphComponent`** - Visual dependency graph
- **`MigrationPhasesComponent`** - Migration phases and progress
- **`PluginSettings`** - Configuration and preferences
- **User Interaction Models** - Events, actions, state management

## 🎯 Requirements Mapping

These TypeSpec files directly address the requirements from `docs/requirements/intellij-plugin-ui.md`:

### ✅ **Side Panel UI Components**
1. **📊 Table of affected dependencies** → `DependenciesTableComponent`
2. **📈 Graph of module migration dependencies** → `DependencyGraphComponent`  
3. **📋 Table of migration phases and status** → `MigrationPhasesComponent`

### ✅ **MCP Server Integration**
- Complete interface definitions in `mcp-integration.tsp`
- All MCP tools mapped to TypeScript interfaces
- Request/response models for all operations

### ✅ **Data Models**
- Rich data models for all UI components
- Enums for status, risk levels, and categories
- Comprehensive type safety for all operations

## 🛠️ Usage

### Generating API Documentation
```bash
# Install TypeSpec compiler
npm install -g @typespec/compiler @typespec/openapi3 @typespec/json-schema

# Generate OpenAPI spec and JSON schemas
tsp compile spec/main.tsp
```

This will generate:
- `generated/jakarta-migration-plugin-api.yaml` - OpenAPI 3.0 specification
- `generated/jakarta-migration-plugin-schemas.json` - JSON Schema definitions

### Integration with Development

1. **Java Code Generation**: Use the generated schemas to create Java POJOs
2. **API Documentation**: Use OpenAPI spec for documentation generation
3. **Frontend Development**: Use TypeScript definitions for type safety
4. **Testing**: Use schemas for request/response validation

## 🏗️ Architecture Overview

```
TypeSpec Specifications
├── Core Data Models (intellij-plugin-ui.tsp)
│   ├── MigrationDashboard
│   ├── DependencyInfo
│   ├── MigrationPhase
│   └── DependencyGraph
├── MCP Integration (mcp-integration.tsp)
│   ├── McpClientService Interface
│   ├── Analysis Operations
│   ├── Migration Operations
│   └── Request/Response Models
└── UI Components (plugin-components.tsp)
    ├── Tool Window Management
    ├── Component Definitions
    ├── User Interactions
    └── Settings Management
```

## 🔄 Development Workflow

1. **Requirements Analysis** → TypeSpec specifications
2. **Code Generation** → Java POJOs and interfaces
3. **Implementation** → IntelliJ plugin development
4. **Testing** → Schema validation and API testing
5. **Documentation** → Auto-generated from TypeSpec

## 📚 Related Documentation

- **Requirements**: `docs/requirements/intellij-plugin-ui.md`
- **MCP Tools**: `docs/mcp/MCP_TOOLS_IMPLEMENTATION.md`
- **Architecture**: `docs/architecture/core-modules-design.md`
- **IntelliJ Plugin**: `intellij-plugin/` directory

## 🚀 Next Steps

1. **Generate Schemas**: Run TypeSpec compiler to generate API specs
2. **Java Code Generation**: Create POJOs from generated schemas
3. **Plugin Implementation**: Use specifications as implementation guide
4. **UI Development**: Build components based on defined models
5. **MCP Integration**: Implement client service using defined interfaces

These TypeSpec specifications provide a complete, type-safe foundation for implementing the Jakarta Migration IntelliJ Plugin with all required UI components and MCP server integration.