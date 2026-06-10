# PremiumMigrationTools.java Cleanup

## Problem

`PremiumMigrationTools.java` in `premium-mcp-server` has grown to ~835 lines, well beyond the project's 500-line source file limit (see `AgentRules/CODING.md`). This was a deliberate trade-off to quickly close the MCP tool gaps identified during the review of deep scan and HTML report functionality, but the class now violates the KISS principle and should be refactored.

## Current State

The class contains 15+ `@McpTool` methods across unrelated domains:

- **Recipe tools**: `listRefactorRecipes`, `listRefactorRecipesByCategory`, `applyRefactorRecipe`, `undoRefactorRecipe`, `getRefactorHistory`, `applyJakartaRecipe`
- **Analysis tools**: `detectBlockers`, `analyzeJakartaReadiness`, `analyzeMigrationImpact`, `recommendVersions`, `scanBinaryDependency`
- **Planning tools**: `generateMigrationPlan`, `validateMigration`
- **Scanning tools**: `runAdvancedScan`
- **Reporting tools**: `createReport`, `generateHtmlReport`

All of these share a single constructor that instantiates `AdvancedScanningModule`, `HtmlToPdfReportServiceImpl`, and `DefaultJarCompatibilityScanner` regardless of which tool is actually used.

## Proposed Refactoring

Split `PremiumMigrationTools` into focused tool classes under `adrianmikula.jakartamigration.mcp.tools`:

| New Class | Tools | Dependencies |
|-----------|-------|--------------|
| `RecipeTools` | `listRefactorRecipes`, `listRefactorRecipesByCategory`, `applyRefactorRecipe`, `undoRefactorRecipe`, `getRefactorHistory`, `applyJakartaRecipe` | `RecipeService` |
| `AdvancedScanningTools` | `runAdvancedScan` | `AdvancedScanningModule` |
| `ReportingTools` | `createReport`, `generateHtmlReport` | `DependencyAnalysisModule`, `PdfReportService` |
| `MigrationAnalysisTools` | `detectBlockers`, `analyzeJakartaReadiness`, `analyzeMigrationImpact`, `recommendVersions`, `generateMigrationPlan`, `validateMigration` | `DependencyAnalysisModule` |
| `BinaryScanningTools` | `scanBinaryDependency` | `JarCompatibilityScanner` |

## Benefits

- Each class stays under 500 lines
- Easier to test in isolation
- Clearer dependency injection (no unnecessary service instantiation)
- Better alignment with IntelliJ plugin's `McpToolRegistry` which already groups tools by category

## Acceptance Criteria

- [ ] Create `adrianmikula.jakartamigration.mcp.tools` package
- [ ] Move tools into focused classes as outlined above
- [ ] Update `PremiumMigrationTools` to delegate or remove entirely
- [ ] Ensure all existing `@McpTool` annotations remain discoverable by Spring AI MCP scanner
- [ ] All existing tests pass without regression
- [ ] Add new package-level unit tests for each split class

## Related

- Issue discovered during: MCP tools gap review for deep scan and HTML reports (May 2026)
- See also: `AgentRules/CODING.md` (500-line limit), `docs/adr/0002-consolidate-platform-result-objects.md`
