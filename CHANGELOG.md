# Changelog

All notable changes to the Jakarta Migration IntelliJ plugin will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-02-05

### Added
- Initial release to IntelliJ Marketplace
- Migration readiness analysis for Jakarta EE 8 to Jakarta EE 9+ migration
- Dependency analysis and version recommendations
- Module dependency graph visualization with multiple layout strategies (hierarchical, force-directed, circular, tree)
- Phased migration planning with task tracking
- MCP server integration for AI Assistant tool discovery
- 9 MCP tools available to AI Assistant:
  - `analyzeJakartaReadiness` - Analyzes project readiness for Jakarta EE migration
  - `analyzeMigrationImpact` - Provides detailed migration impact analysis
  - `detectBlockers` - Identifies migration blockers
  - `recommendVersions` - Recommends compatible dependency versions
  - `applyOpenRewriteRefactoring` - Applies automated refactoring
  - `scanBinaryDependency` - Scans JAR dependencies for compatibility
  - `updateDependency` - Updates dependencies to recommended versions
  - `generateMigrationPlan` - Creates phased migration plans
  - `validateMigration` - Validates migration results
- Tool window integration for visual migration dashboard
- OpenRewrite refactoring support
- Support for IntelliJ IDEA 2023.3+ and other JetBrains IDEs

### Features
- Interactive dependency graph with zoom and pan
- Detailed dependency table with migration status
- Migration phase tracking and progress visualization
- Risk level assessment for migration tasks
- Seamless integration with AI Assistant for automated migration assistance
