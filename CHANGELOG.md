# Changelog

All notable changes to the Jakarta Migration IntelliJ plugin will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.6] - 2026-03-21

### Fixed
- Bug: OpenRewrite recipe class name fix - JavaxValidationToJakartaValidation now correctly maps to JavaxValidationMigrationToJakartaValidation (missing "Migration" suffix)
- Bug: RecipeSeeder now loads all 33 recipes from recipes.yaml (previously only seeded a subset)
- Bug: Recipe database now uses recipe name as PRIMARY KEY for stable identity
- Bug: Support tab now shows dynamic build timestamp instead of hardcoded old date

### Added
- Recipe validation tests: RecipeLoopValidationTest and OpenRewriteRecipeExistenceTest verify all recipes exist and are correctly configured
- Dynamic build timestamp generation in IntelliJ plugin - shows actual build time in Support tab
- Support URLs now loaded from support-urls.properties for easier configuration

### Changed
- RecipeSeeder now loads from recipes.yaml for centralized recipe configuration

## [Unreleased]

### Fixed
- Bug: Dependencies tab now shows upgrade version recommendations from Maven Central (dynamic lookup wired to DependencyAnalysisModule)
- Bug: OpenRewrite recipe execution improved with classpath from project build output and robust path resolution for writing changes
- Bug: Status text now updates when clicking different openrewrite recipes in Refactor tab
- Bug: OpenRewrite recipes now correctly apply changes to files (expanded recipe coverage)
- Bug: RefactoringEngine now loads recipe replacements from YAML configuration (no hardcoded recipe names)
- Regression: Runtime UI tab now hidden by default
- Regression: Strategy tab now shows all 6 strategies (added Hybrid strategy)
- Regression: Refactor tab now displays recipe description and execution results
- Dashboard positioning fixed - scan counts no longer overlap with status/timestamp
- Scan count highlighting: >0 shown in red, =0 shown in green
- Fixed: History and Refactor tabs now visible as premium feature (previously missing locked placeholders)
- Plugin version now uses semver format from gradle.properties (removed timestamp generation)
- Fixed gradle build errors with version loading from properties

### Added
- Comprehensive test coverage for RefactoringEngine covering all Jakarta migration recipes

### Added
- Color legend to dependencies graph tab showing Jakarta compatibility status
- Progress bar to advanced scans showing completion status
- Hover info popup in dependencies graph showing maven coordinates and jakarta compatibility status
- Strategy boxes now shorter vertically to provide more space for description boxes
- History tab (Premium) - tracks all code changes made via the plugin with undo support
- Recipe execution logging to SQLite database for history tracking
- Database-backed undo state for Refactor tab recipes
- CI/CD workflow for IntelliJ plugin build and publish

### Changed
- Dependencies graph now colors dependencies by jakarta compatibility (green=compatible, yellow=needs upgrade, red=no jakarta version)
- Organisational dependencies now have thicker border and larger font instead of different color
- Dependencies graph tooltips now show maven coordinates and jakarta compatibility status
- Publish to JetBrains Marketplace only on main branch commits (not PRs)

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
