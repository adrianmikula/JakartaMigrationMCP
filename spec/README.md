# Jakarta Migration IntelliJ Plugin TypeSpec Specifications

This directory contains TypeSpec specifications that define the requirements, data models, and API interfaces for the Jakarta Migration IntelliJ Plugin UI components.

## 📁 File Structure

```
spec/
├── main.tsp                    # Main entry point with service definition
├── intellij-plugin-ui.tsp     # Core UI data models and enums
├── mcp-integration.tsp         # MCP server integration interfaces
├── plugin-components.tsp       # IntelliJ plugin UI components
├── advanced-scanning.tsp       # Advanced scanning and recipe recommendation services
├── pdf-reporting.tsp           # PDF reporting and analysis services
├── enhanced-test-coverage-analysis.tsp  # Enhanced test coverage analysis (reusing existing scanners)
├── html-to-pdf-reporting.tsp   # HTML-to-PDF report generation (replacing PDFBox)
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

### 5. `advanced-scanning.tsp` - Advanced Scanning Services
Defines advanced scanning and recipe recommendation services:

- **Advanced Scanning Service**:
  - JPA annotation scanning
  - Bean validation scanning
  - Servlet/JSP scanning
  - 18+ specialized scanner types
- **Recipe Recommendation Service**:
  - Confidence-based recipe recommendations
  - Scan-to-recipe mapping
  - File-specific recommendations
- **Data Models**:
  - Scan result types for all scanners
  - Recipe definitions and categories
  - Recommendation confidence scoring

### 6. `pdf-reporting.tsp` - PDF Reporting Services
Defines PDF generation and reporting services:

- **PDF Report Service**:
  - Configurable PDF generation
  - Template-based reporting
  - Validation and error handling
- **Report Content Models**:
  - Dependency tree visualization
  - Maven dependency lists and trees
  - Advanced scan results summaries
  - Support links and metadata
- **Configuration Models**:
  - Report templates and sections
  - Generation options
  - Validation rules

### 7. `enhanced-test-coverage-analysis.tsp` - Enhanced Test Coverage Analysis
Defines enhanced test coverage analysis building on existing file counting infrastructure:

- **Enhanced Test Coverage Analysis Service**:
  - Builds on existing test file counting (src/test/, Test*.java patterns)
  - Correlates test coverage with migration risk
  - Critical risk zone detection (migration issues + low coverage)
  - Enhanced recommendations for test improvement
- **Data Models**:
  - Enhanced module coverage analysis
  - Critical risk zone identification
  - Migration readiness validation
  - Dashboard integration data

### 8. `html-to-pdf-reporting.tsp` - HTML-to-PDF Report Generation
Defines professional HTML-to-PDF report generation replacing PDFBox approach:

- **HTML-to-PDF Report Service**:
  - HTML template-based report generation
  - Professional styling and layout
  - Multiple template options (Professional, Technical, Minimal)
  - Color scheme customization
- **Template System**:
  - Executive summary sections
  - Visual hierarchy with CSS styling
  - Responsive design for PDF output
  - Template validation and preview
- **Conversion Features**:
  - OpenHTMLtoPDF integration
  - Report preview functionality
  - Data validation before generation
  - Generation statistics and metadata

## Implementation Status ✅

### ✅ Enhanced Test Coverage Analysis
- **EnhancedTestCoverageAnalysisService**: Implemented with library-based test type detection
- **Critical Risk Zone Detection**: Identifies modules with migration issues + low coverage
- **Migration-Aware Confidence Scoring**: Prioritizes integration tests over mocked unit tests
- **UI Integration**: Enhanced dashboard with detailed validation confidence breakdown

### ✅ HTML-to-PDF Report Generation
- **HtmlToPdfReportServiceImpl**: Professional HTML templates with CSS styling
- **OpenHTMLtoPDF Integration**: Added dependency and conversion logic
- **Template System**: Multiple professional templates (Professional, Technical, Minimal)
- **Comprehensive Testing**: Full test coverage for new services

### ✅ Architecture Compliance
- **SDD Approach**: Detailed TypeSpec specifications created first
- **TDD Implementation**: Tests written before and during implementation
- **Licensing Compliance**: Premium features properly isolated
- **Code Quality**: KISS principles, SOLID patterns, comprehensive documentation

## 🎯 Requirements Mapping

These TypeSpec files directly address the requirements from `docs/requirements/intellij-plugin-ui.md` and `TASKS.md`:

### ✅ **Side Panel UI Components**
1. **📊 Table of affected dependencies** → `DependenciesTableComponent`
2. **📈 Graph of module migration dependencies** → `DependencyGraphComponent`  
3. **📋 Table of migration phases and status** → `MigrationPhasesComponent`

### ✅ **MCP Server Integration**
- Complete interface definitions in `mcp-integration.tsp`
- All MCP tools mapped to TypeScript interfaces
- Request/response models for all operations

### ✅ **Advanced Scanning & Recipe Recommendations**
- 18+ specialized scanner types in `advanced-scanning.tsp`
- Recipe recommendation service with confidence scoring
- Scan-to-recipe mapping and file-specific recommendations

### ✅ **PDF Reporting**
- Complete PDF generation service in `pdf-reporting.tsp`
- Configurable report templates and sections
- Integration with scan results and dependency analysis

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
├── UI Components (plugin-components.tsp)
│   ├── Tool Window Management
│   ├── Component Definitions
│   ├── User Interactions
│   └── Settings Management
├── Advanced Scanning (advanced-scanning.tsp)
│   ├── Scanner Service Interfaces
│   ├── Recipe Recommendation Service
│   ├── Scan Result Data Models
│   └── Recipe Definition Models
└── PDF Reporting (pdf-reporting.tsp)
    ├── PDF Generation Service
    ├── Report Content Models
    ├── Template Definitions
    └── Configuration Models
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
- **Tasks**: `TASKS.md` - Complete task implementation tracking

## 🚀 Next Steps

1. **Generate Schemas**: Run TypeSpec compiler to generate API specs
2. **Java Code Generation**: Create POJOs from generated schemas
3. **Plugin Implementation**: Use specifications as implementation guide
4. **UI Development**: Build components based on defined models
5. **MCP Integration**: Implement client service using defined interfaces
6. **Advanced Scanning**: Implement scanner and recommendation services
7. **PDF Reporting**: Implement PDF generation with templates

These TypeSpec specifications provide a complete, type-safe foundation for implementing the Jakarta Migration IntelliJ Plugin with all required UI components, MCP server integration, advanced scanning capabilities, and PDF reporting functionality.