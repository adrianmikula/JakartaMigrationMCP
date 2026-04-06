# Jakarta Migration MCP - Codebase Index

Quick reference guide for navigating the codebase, key commands, modules, and services.

## Project Structure

### Modules

| Module | Path | License | Description |
|--------|------|---------|-------------|
| **community-core-engine** | `/community-core-engine/` | Apache 2.0 | Core migration analysis engine, dependency analysis, source code scanning |
| **community-mcp-server** | `/community-mcp-server/` | Apache 2.0 | MCP server exposing community features via Model Context Protocol |
| **premium-core-engine** | `/premium-core-engine/` | Proprietary | Premium features: PDF reporting, advanced scanning, recipe validation |
| **premium-intellij-plugin** | `/premium-intellij-plugin/` | Proprietary | IntelliJ IDEA plugin with UI for migration analysis |
| **premium-mcp-server** | `/premium-mcp-server/` | Proprietary | Premium MCP server with additional capabilities |

### Key Directories

- `/docs/` - Documentation (architecture, requirements, standards)
- `/spec/` - API specifications and contracts
- `/scripts/` - Build and setup scripts
- `/examples/` - Example projects for testing
- `/config/` - Configuration files
- `/AgentRules/` - Agent rule templates

## Development Commands

### Mise Tasks (Recommended)

Use `mise run <task>` or `mr <task>`:

| Task | Command | Description |
|------|---------|-------------|
| **compile** | `mise run compile` | Quick compilation check |
| **compile-check** | `mise run compile-check` | Compile check without tests |
| **build** | `mise run build` | Build all modules (no tests) |
| **build-all** | `mise run build-all` | Build all modules with tests |
| **test** | `mise run test` | Run all tests |
| **test-fast** | `mise run test-fast` | Run fast tests only |
| **test-slow** | `mise run slow-test` | Run slow integration tests |
| **test-core** | `mise run core-test` | Run core functionality tests |
| **clean** | `mise run clean` | Clean build artifacts |

### Gradle Tasks

| Task | Command | Description |
|------|---------|-------------|
| **compileJava** | `./gradlew compileJava` | Compile all Java sources |
| **compileCheck** | `./gradlew :<module>:compileCheck` | Compilation check only |
| **test** | `./gradlew :<module>:test` | Run tests for specific module |
| **fastTest** | `./gradlew :<module>:fastTest` | Run fast tests (excludes @Tag("slow")) |
| **slowTest** | `./gradlew :<module>:slowTest` | Run slow tests (@Tag("slow")) |
| **coreTest** | `./gradlew :<module>:coreTest` | Run core tests |
| **buildPlugin** | `./gradlew :premium-intellij-plugin:buildPlugin` | Build IntelliJ plugin |
| **runIdeDev** | `./gradlew :premium-intellij-plugin:runIdeDev` | Run IntelliJ IDE with plugin (dev mode) |
| **runIdeDemo** | `./gradlew :premium-intellij-plugin:runIdeDemo` | Run IntelliJ IDE with plugin (IntelliJ marketplace demo) |
| **runIdeProd** | `./gradlew :premium-intellij-plugin:runIdeProd` | Run IntelliJ IDE with plugin (prod mode) |

### Blacklisted Commands

Blacklisted cross-platform commands:
- git commit
- git merge
- git rebase

Blacklisted Windows commands:
- cmd
- powershell

Blacklisted Linux commands:
- bash
- sh
- su
- sudo

## Key Services

### Community Core Engine

| Service | Path | Description |
|---------|------|-------------|
| **ImprovedMavenCentralLookupService** | `dependencyanalysis/service/ImprovedMavenCentralLookupService.java` | Maven Central lookup with fuzzy matching for Jakarta equivalents |
| **JakartaMappingService** / **JakartaMappingServiceImpl** | `dependencyanalysis/service/JakartaMappingService.java` / `impl/JakartaMappingServiceImpl.java` | Maps javax dependencies to Jakarta equivalents |
| **DependencyAnalysisModuleImpl** | `dependencyanalysis/service/impl/DependencyAnalysisModuleImpl.java` | Core dependency analysis orchestration |
| **DependencyGraphBuilder** / **MavenDependencyGraphBuilder** | `dependencyanalysis/service/DependencyGraphBuilder.java` / `impl/MavenDependencyGraphBuilder.java` | Builds dependency graphs from Maven/Gradle projects |
| **SourceCodeScannerImpl** | `sourcecodescanning/service/impl/SourceCodeScannerImpl.java` | AST-based source code scanning for javax.* usage |
| **ProjectFileSystemScanner** | `util/ProjectFileSystemScanner.java` | File system scanning with ignore patterns |
| **CentralMigrationAnalysisStore** / **SqliteMigrationAnalysisStore** | `analysis/persistence/CentralMigrationAnalysisStore.java` / `SqliteMigrationAnalysisStore.java` | Database persistence for migration analysis |
| **RecipeChangedFilesStore** | `analysis/persistence/RecipeChangedFilesStore.java` | Stores original file content for recipe undo |
| **NamespaceClassifier** | `dependencyanalysis/service/NamespaceClassifier.java` | Classifies dependencies as javax/jakarta/both/neither |

### Premium Core Engine

| Service | Path | Description |
|---------|------|-------------|
| **RecipeServiceImpl** | `coderefactoring/service/impl/RecipeServiceImpl.java` | OpenRewrite recipe execution and management |
| **RecipeValidator** | `recipevalidation/RecipeValidator.java` | Validates OpenRewrite recipes for correctness |
| **PdfReportServiceImpl** | `pdfreporting/service/impl/PdfReportServiceImpl.java` | PDF report generation using Apache PDFBox |
| **EnhancedPlatformDetectionService** | `platforms/service/EnhancedPlatformDetectionService.java` | Detects target platforms (Spring Boot, Jakarta EE, etc.) |
| **RiskScoringService** | `platforms/service/RiskScoringService.java` | Migration risk scoring and assessment |
| **AdvancedRecipeScanningServiceImpl** | `advancedscanning/service/impl/AdvancedRecipeScanningServiceImpl.java` | Advanced scanning for recipe recommendations |
| **ScanRecipeRecommendationServiceImpl** | `advancedscanning/service/impl/ScanRecipeRecommendationServiceImpl.java` | Recommends recipes based on scan results |
| **ThirdPartyLibScannerImpl** | `advancedscanning/service/impl/ThirdPartyLibScannerImpl.java` | Scans third-party libraries for compatibility |
| **UnitTestScannerImpl** | `advancedscanning/service/impl/UnitTestScannerImpl.java` | Scans unit tests for migration impact |
| **LoggingMetricsScannerImpl** | `advancedscanning/service/impl/LoggingMetricsScannerImpl.java` | Scans for logging and metrics library usage |

### Premium IntelliJ Plugin

| Service | Path | Description |
|---------|------|-------------|
| **MigrationAnalysisService** | `intellij/service/MigrationAnalysisService.java` | Main IDE migration analysis orchestrator |
| **SimplifiedMigrationAnalysisService** | `intellij/service/SimplifiedMigrationAnalysisService.java` | Lightweight analysis for quick scans |
| **RiskScoringService** | `intellij/service/RiskScoringService.java` | IDE risk scoring |
| **RuntimeScanningService** | `intellij/service/RuntimeScanningService.java` | Runtime environment scanning |
| **ScanGeneratorService** | `intellij/service/ScanGeneratorService.java` | Generates scan configurations |

### UI Components (Premium IntelliJ Plugin)

| Component | Path | Description |
|-----------|------|-------------|
| **DependenciesTableComponent** | `intellij/ui/DependenciesTableComponent.java` | Dependency visualization table with Maven Central lookup |
| **DashboardComponent** | `intellij/ui/DashboardComponent.java` | Main dashboard UI with summary metrics |
| **MigrationToolWindow** | `intellij/ui/MigrationToolWindow.java` | Main tool window container |
| **AdvancedScansComponent** | `intellij/ui/AdvancedScansComponent.java` | Advanced scanning configuration UI |
| **PlatformsTabComponent** | `intellij/ui/PlatformsTabComponent.java` | Platform selection and configuration |
| **RuntimeTabComponent** | `intellij/ui/RuntimeTabComponent.java` | Runtime environment UI |
| **ComprehensiveReportsTabComponent** | `intellij/ui/ComprehensiveReportsTabComponent.java` | Comprehensive scan reports UI |
| **RecipeExecutionPanel** | `intellij/ui/RecipeExecutionPanel.java` | Recipe execution and progress UI |
| **ReportsPanel** | `intellij/ui/ReportsPanel.java` | Migration reports display |
| **MigrationStatusPanel** | `intellij/ui/MigrationStatusPanel.java` | Migration status indicators |

## Configuration Files

| File | Purpose |
|------|---------|
| `.mise.toml` | Mise-en-place configuration (tools, tasks, environment) |
| `gradle.properties` | Gradle configuration (memory, caching, parallel execution) |
| `settings.gradle.kts` | Gradle project structure and module inclusion |
| `build.gradle.kts` | Root build configuration |
| `docker-compose.yml` | Docker services (PostgreSQL, Redis) |
| `.env` | Environment variables (see `.env.example` for template) |
| `scans.yaml` | Scan configuration (ignore folders, file patterns) |

## Documentation Index

### Getting Started
- [README.md](../README.md) - Project overview and introduction
- [docs/SETUP.md](SETUP.md) - Development environment setup
- [docs/RELEASING.md](RELEASING.md) - Release process
- [docs/common_issues.md](common_issues.md) - Common issues and solutions

### Specifications
- [spec/](spec/) - API specifications and contracts
- [docs/requirements/](requirements/) - Requirements documentation
- [docs/adr/](adr/) - Architecture Decision Records

### AI Agents
- [AGENTS.md](../AGENTS.md) - AI Agent rules and guidelines
- [docs/FAST_TEST_LOOP.md](FAST_TEST_LOOP.md) - Fast test loop documentation
- [docs/standards/simplicity_and_consistency.md](standards/simplicity_and_consistency.md) - Code simplicity guidelines

## Testing

### Test Categories
- **Fast Tests**: Unit tests without external dependencies (excluded by @Tag("slow"))
- **Slow Tests**: Integration tests with network/file I/O (marked with @Tag("slow"))

### Key Test Files
- `MavenLookupFixTest` - Maven Central lookup integration tests
- `JakartaLookupVerificationTest` - Jakarta artifact lookup verification
- `FeatureFlagTest` - Feature flag and pricing tests
- `XmlScanningTest` - XML file scanning tests
- `SourceCodeScannerTest` - Source code scanning tests

## License Structure

- **Community modules** (`community-*`): Apache 2.0 License
- **Premium modules** (`premium-*`): Proprietary License

Community modules must never reference premium modules (strict open-core licensing).

## Quick Reference

### Common Development Workflow

```bash
# 1. Start services
mise run start-services

# 2. Quick compile check
mise run compile-check

# 3. Run fast tests
mise run test-fast

# 4. Run slow tests (if needed)
mise run test-slow

# 5. Full build
mise run build-all
```

### Module-Specific Commands

```bash
# Community core engine
./gradlew :community-core-engine:fastTest
./gradlew :community-core-engine:slowTest

# Premium core engine  
./gradlew :premium-core-engine:fastTest
./gradlew :premium-core-engine:coreTest

# IntelliJ plugin
./gradlew :premium-intellij-plugin:buildPlugin
./gradlew :premium-intellij-plugin:runIde
```

---

*For detailed information on specific components, see the relevant documentation files or source code.*
