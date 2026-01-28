# Test Organization Review: Free vs Pro Features

This document summarizes how tests are split between the **free** (root) project and the **premium** (`jakarta-migration-mcp-premium`) project, and confirms that pro features are tested in the premium project and free features in the free project.

## Feature Tiers (from FEATURE_FLAGS.md)

| Tier | Features |
|------|----------|
| **Free (COMMUNITY)** | Basic scanning, dependency analysis, migration planning (read-only), analyzeJakartaReadiness, detectBlockers, recommendVersions, analyzeMigrationImpact (basic) |
| **Pro (PREMIUM)** | Auto-fixes, one-click refactor, createMigrationPlan, validateRefactoring, executeRefactor, verifyRuntime (BINARY_FIXES), credits, License API, Stripe, Apify, FeatureFlags, local storage |

## Free Project Tests (`src/test`)

### Location
- **Root project** only: `src/test/java` (component, integration, unit).

### What is tested (free features only)

1. **MCP tools – free behavior**
   - **Unit:** `unit/jakartamigration/mcp/JakartaMigrationToolsTest.java`
     - `analyzeJakartaReadiness`, `detectBlockers`, `recommendVersions`, `analyzeMigrationImpact` (all free tools).
   - **Integration:** `component/jakartamigration/mcp/JakartaMigrationToolsIntegrationTest.java`
     - Free tools: `analyzeJakartaReadiness` (non-existent path, empty dir, file path).
     - Pro tool gating: `verifyRuntime` returns `upgrade_required` when unlicensed (asserts free tier correctly gates pro tools).

2. **MCP server**
   - `integration/mcp/McpServerSseIntegrationTest.java`, `McpServerStreamableHttpIntegrationTest.java`, `McpServerStdioIntegrationTest.java`
     - List tools (including pro tool names); call free tools; call `createMigrationPlan` and assert response (in community context this is `upgrade_required`).

3. **Dependency analysis (free)**
   - `unit/jakartamigration/dependencyanalysis/` – ArtifactTest, DependencyGraphTest, DependencyGraphBuilderTest, JakartaMappingServiceTest, GradleDependencyGraphBuilderTest, BinaryCompatibilityIntegrationTest.
   - Excluded from compilation (errors): MavenDependencyGraphBuilderTest, NamespaceClassifierTest, DependencyAnalysisModuleTest.

4. **Source code scanning (free)**
   - `unit/jakartamigration/sourcecodescanning/` – SourceCodeScannerTest, SourceCodeScannerIntegrationTest, XmlScanningTest.

5. **Excluded from free project**
   - `**/mcp/JakartaMigrationToolsBootstrapTest.java`, `JakartaMigrationToolsPerformanceTest.java` (premium-only; referenced in `build.gradle.kts`).
   - `**/projectname/**` (template/example tests).

### Summary
- Free project tests cover **free tools** (readiness, blockers, versions, impact) and **pro tool gating** (e.g. `verifyRuntime` and `createMigrationPlan` return `upgrade_required` in community context).
- No tests in the free project assert **success** of pro-only tools (createMigrationPlan, validateRefactoring, executeRefactor, verifyRuntime with license).

---

## Premium Project Tests (`jakarta-migration-mcp-premium/src/test`)

### Location
- **Premium module** only: `jakarta-migration-mcp-premium/src/test/java`.

### What is tested (pro features and shared behavior)

1. **Licensing and billing (pro)**
   - **Config:** StripeLicenseServiceTest, StripeEmailValidationTest, ApifyLicenseServiceTest, LicenseServiceTest, FeatureFlagsServiceTest, PlatformBasedLicensingConfig(Integration)Test, PlatformBasedLicensingPostProcessor(Integration)Test, YamlConfigurationTest.
   - **API:** LicenseApiControllerTest (validation, balance, consume credits, createMigrationPlan credit consumption), StripeWebhookControllerTest.
   - **Services:** CreditServiceTest, StripePaymentLinkServiceTest.
   - **Storage:** LocalLicenseStorageServiceTest.

2. **Pro MCP tools and refactoring**
   - **Unit:** `unit/jakartamigration/mcp/JakartaMigrationToolsTest.java` – same free-tool unit tests as root (mocked).
   - **Unit:** `unit/jakartamigration/mcp/JakartaMigrationToolsPerformanceTest.java` – performance of free tools and **createMigrationPlan** (with mocked PREMIUM tier).
   - **Unit:** `unit/jakartamigration/mcp/JakartaMigrationToolsBootstrapTest.java` – bootstrap and readiness.

3. **Code refactoring (pro)**
   - MigrationPlannerTest, MigrationPlanTest, MigrationProgressTest, MigrationImpactSummaryTest.
   - CodeRefactoringModuleTest (createMigrationPlan, validateRefactoring).
   - RefactoringEngineTest, RecipeLibraryTest, ChangeTrackerTest, ProgressTrackerTest.
   - RefactoringChangesTest, RefactoringOptionsTest, RefactoringPhaseTest, RefactoringResultTest, RollbackResultTest, ValidationResultTest, RecipeTest.
   - ApacheTomcatMigrationToolTest, ApacheTomcatMigrationToolIntegrationTest.

4. **Runtime verification (pro tool: verifyRuntime)**
   - RuntimeVerificationModuleTest (verifyRuntime), BytecodeAnalyzerTest, ErrorAnalysisTest, RuntimeErrorTest, VerificationOptionsTest, VerificationResultTest.

5. **Configuration**
   - JakartaMigrationConfigTest (beans including RefactoringEngine, RecipeLibrary, etc.).

### Summary
- Premium project tests cover **licensing, Stripe, Apify, credits, feature flags, platform config**, **createMigrationPlan / validateRefactoring / refactoring stack**, and **verifyRuntime / RuntimeVerificationModule**.
- Pro tools are tested with mocks (e.g. PREMIUM tier in performance test) and via service-level tests (CodeRefactoringModule, RuntimeVerificationModule).

---

## Checklist

| Requirement | Status |
|-------------|--------|
| Free tools (readiness, blockers, versions, impact) tested in free project | Yes – unit and integration in `src/test` |
| Pro tool gating (upgrade_required) tested in free project | Yes – verifyRuntime in JakartaMigrationToolsIntegrationTest; createMigrationPlan in MCP server integration tests (see below) |
| Pro licensing/billing/credits tested in premium project | Yes – config, API, services, storage tests |
| Pro refactoring (createMigrationPlan, validateRefactoring, etc.) tested in premium project | Yes – CodeRefactoringModule, MigrationPlanner, RefactoringEngine, etc. |
| Pro verifyRuntime tested in premium project | Yes – RuntimeVerificationModuleTest, BytecodeAnalyzerTest |
| No pro success-path tests in free project | Yes – free project only asserts upgrade_required for pro tools |
| Premium-only tests excluded from free build | Yes – Bootstrap and Performance tests excluded in root `build.gradle.kts` |

---

## Free-tier gating assertions

The free project MCP server integration tests (`McpServerSseIntegrationTest`, `McpServerStreamableHttpIntegrationTest`, `McpServerStdioIntegrationTest`) explicitly assert in `testCreateMigrationPlanTool()` that the response contains `upgrade_required` and "PREMIUM" when running with default COMMUNITY tier, so that free-tier gating of the pro tool `createMigrationPlan` is clearly tested.
