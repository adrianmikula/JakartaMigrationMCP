# Changelog

All notable changes to the Jakarta Migration IntelliJ plugin will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.12] - 2026-04-09

### Licensing Changes
- Removed free trial period
- Limited scan results to 10 for free verison
- Limited refactor actions to 10 for free verison

## [1.0.11] - 2026-04-08

### Added
- Added license expiry notification

### Fixed
- Fixed memory leaks when scanning large projects
- Re-organized main dashboard scan results summary
- Major code refactor and simplification

## [1.0.10] - 2026-04-06

### Added
- Added risk score dial and estimated migration time

### Fixed
- Fixed memory leaks when scanning large projects
- Fixed recommendation of jakarta compatible upgrades
- Major platform detection and integration testing overhaul

## [1.0.9] - 2026-04-02

### Added
- Advanced scan for reflection uses of javax packages - detects and analyzes dynamic class loading and reflection calls that reference javax.* packages
- Much more robust testing of all scans and refactor recipes - comprehensive test coverage ensuring reliability of migration analysis and refactoring functionality

### Fixed
- Corrected Jetbrains marketplace monetisation properties in plugin.xml

### Added
- Complete JetBrains Marketplace validation test suite with JUnit 5
- Enhanced error handling and test coverage

### Enhanced
- Updated AI assistant MCP tools to support all scan types, risk analysis, refactor history, and PDF reports

## [1.0.8] - 2026-03-28

### Added
- **Major Feature: PDF Reports** - Generate comprehensive migration analysis reports in PDF format with detailed findings, recommendations, and visual charts for stakeholder presentations
- **Reports Tab** - New dedicated tab in the migration tool window for generating and managing PDF reports with customizable options
- **Report Templates** - Multiple report templates including Executive Summary, Technical Analysis, and Migration Roadmap with different detail levels
- **Visual Charts** - Interactive charts and graphs showing migration complexity, risk assessment, and dependency analysis in PDF reports
- **Export Options** - Save reports to custom locations with automatic timestamping and version control integration
- **Report Preview** - Live preview of PDF reports before generation with real-time updates as analysis data changes

### Enhanced
- Dashboard UI - Improved dashboard layout with better visual hierarchy and enhanced data presentation
- Migration Phases - More detailed phase descriptions with clear action items and timeline estimates
- Dependency Visualization - Improved dependency graph rendering with better layout algorithms and interactive features
- Advanced Scans UI - Refined advanced scanning interface with better categorization and filtering options

### Improved
- Tool Window Layout - Reorganized tool window tabs for better workflow and navigation
- Status Indicators - Clear visual indicators for analysis progress and completion status

### Fixed
- UI Responsiveness - Resolved performance issues with large project analysis and UI updates
- Tab Switching - Improved tab switching performance and state management

### Documentation
- Updated user guide with PDF reporting workflow and best practices

## [1.0.7] - 2026-03-21

### Added
- **Dynamic Recipe Loading** - Advanced Scans tab now dynamically creates UI tabs based on recipe categories from recipes.yaml, eliminating hardcoded tabs and ensuring automatic UI synchronization when new refactoring recipes are added
- **100+ Jakarta Migration Recipes** - Comprehensive collection of javax → jakarta OpenRewrite recipes covering all major Jakarta EE APIs (Servlet, JPA, Bean Validation, CDI, EJB, JSF, JAX-RS, JAX-WS, JMS, JSON-P, JSON-B, Security, XML, Enterprise, Configuration Files, Annotations)
- **Risk Scoring Tests** - Comprehensive test suite with 19 test cases for dashboard risk score calculation with weighted inputs from YAML configuration

### Enhanced
- Recipe Service Integration - Improved recipe loading mechanism with better category filtering and dynamic table management
- Premium Feature Architecture - Maintained premium license enforcement while implementing dynamic UI structure

### Fixed
- Gradle Configuration - Resolved malformed Java home path causing build failures
- Build System - Resolved Java 21/Gradle compatibility issues for reliable builds

## [1.0.6] - 2026-03-15

### Fixed
- OpenRewrite recipe class name fix - JavaxValidationToJakartaValidation now correctly maps to JavaxValidationMigrationToJakartaValidation
- RecipeSeeder now loads all 33 recipes from recipes.yaml
- Recipe database now uses recipe name as PRIMARY KEY for stable identity
- Support tab now shows dynamic build timestamp instead of hardcoded old date

### Added
- Recipe validation tests verify all recipes exist and are correctly configured
- Dynamic build timestamp generation in IntelliJ plugin
- Support URLs now loaded from support-urls.properties

### Changed
- RecipeSeeder now loads from recipes.yaml for centralized configuration

## [1.0.5] - 2026-03-10

### Fixed
- Resolved critical Jackson ClassCastException affecting plugin stability
- Implemented complete recipe isolation for reliable execution
- Enhanced fallback mechanisms for non-Maven/Gradle projects
- Complete OpenRewrite execution isolation
- Safely isolated recipe execution environment
- Reflection-based isolated recipe execution

## [1.0.4] - 2026-03-05

### Fixed
- Reimplemented core refactoring system with cross-platform fixes and SQLite persistence improvements

## [1.0.3] - 2026-03-01

### Fixed
- Strategy tab - Replaced HYBRID with STRANGLER strategy, now shows 6 strategies
- Dependency graph colors - Better visual consistency with dependency list
- Advanced Scans tab - Proper display when trial starts
- Bullet list characters - Used proper dash characters in migration strategy tab

### Added
- Progress bar - Advanced scans progress indicator in dashboard
- History tab - Premium feature for tracking recipe execution history

## [1.0.2] - 2026-02-25

### Fixed
- 'Total Advanced Issues' now at bottom of scan counts
- Removed 'Jakarta Status Indicator' from dashboard
- 'No Jakarta Support' count now updates correctly after scans
- Advanced scans now hidden until premium/trial activated

### Added
- Premium upgrade banner in Support tab for better visibility

### Note
- Freemium licensing implementation pending - requires JetBrains product parameters and license verification

## [1.0.1] - 2026-02-20

### Fixed
- Refactor tab now shows actual files changed after applying recipes
- Refactor recipes now properly applied using RefactoringEngine
- Migration strategy phases now have longer, more descriptive text
- JSON deserialization error now shows warning instead of blocking
- Advanced scans tab no longer disappears when switching tabs
- HYBRID strategy replaced with STRANGLER/MICROSERVICES strategy

### Added
- Premium upgrade banner in Support tab for better visibility

### Improved
- Detailed refactoring results showing files processed and changed

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
