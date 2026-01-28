# Pro Source Code Audit: Free vs Premium Folder Structure

This document audits the **root (free)** project’s `src/main/java` to find **pro-related** source files that are still present in the free project. The intended model is: **pro code lives only under the premium module** (`jakarta-migration-mcp-premium`); the free project should not contain pro-only implementations.

## Current Architecture

- **Root project** (`:`) = base build; contains `src/main/java` with **both free and pro** code today.
- **Premium module** (`:jakarta-migration-mcp-premium`) = `implementation(project(":"))`; adds OpenRewrite and **overrides** many classes with its own copies (same package names). Premium does **not** contain `dependencyanalysis/` or `sourcecodescanning/`; it gets those from the root dependency.

So today, **pro-related files are duplicated**: they exist in **root** and again in **premium**. The goal of this audit is to identify pro-only files that **should not** remain in the root (so they exist only in the premium folder structure).

---

## Classification

### Pro-only (should exist only in premium)

These implement or support **only** pro features (licensing, Stripe, Apify, credits, refactoring, verifyRuntime, platform-based licensing, local license storage). They are **already present in premium**. They should be **removed from root** so the free build does not ship pro code.

| Category | Path in root (`src/main/java/...`) | In premium? |
|----------|------------------------------------|-------------|
| **API – License & Credits** | `api/controller/LicenseApiController.java` | Yes |
| | `api/controller/StripeWebhookController.java` | Yes |
| | `api/service/CreditService.java` | Yes |
| | `api/service/StripePaymentLinkService.java` | Yes |
| | `api/dto/ConsumeCreditsRequest.java` | Yes |
| | `api/dto/ConsumeCreditsResponse.java` | Yes |
| | `api/dto/CreditBalanceResponse.java` | Yes |
| | `api/dto/LicenseValidationResponse.java` | Yes |
| | `api/dto/PaymentLinkResponse.java` | Yes |
| | `api/dto/SyncCreditsResponse.java` | Yes |
| **Config – Licensing** | `config/ApifyBillingService.java` | Yes |
| | `config/ApifyLicenseService.java` | Yes |
| | `config/ApifyLicenseProperties.java` | Yes |
| | `config/StripeLicenseService.java` | Yes |
| | `config/StripeLicenseProperties.java` | Yes |
| | `config/LicenseService.java` | Yes |
| | `config/PlatformBasedLicensingPostProcessor.java` | Yes |
| | `config/PlatformBasedLicensingConfig.java` | Yes |
| | `config/PlatformDetectionService.java` | Yes |
| **Storage** | `storage/service/LocalLicenseStorageService.java` | Yes |
| **Code refactoring (pro tools)** | `coderefactoring/` (entire package: domain + service + impl) | Yes |
| **Runtime verification (pro tool)** | `runtimeverification/` (entire package: domain + service + impl) | Yes |

So: **all of the above are pro-related and already exist in the premium module.** They are the ones that have been “moved” into the pro folder structure in the sense that premium has its own copy; the issue is they are **still also present in the free project**.

### Shared (needed in free for gating / tier)

These are required in the **free** project so that free builds can gate pro tools (e.g. return `upgrade_required`) and handle tier/defaults. They should **stay in root** (and can remain in premium for consistency).

- `config/FeatureFlag.java`
- `config/FeatureFlagsProperties.java`
- `config/FeatureFlagsService.java`
- `config/JakartaMigrationConfig.java` (root version; see note below)
- `config/NativeHintsConfig.java` (root-only; AOT/native hints)

### Free-only (correctly only in root)

Premium does **not** have these; it gets them via `implementation(project(":"))`. They are free features only.

- `dependencyanalysis/` (entire package)
- `sourcecodescanning/` (entire package)

### MCP layer (shared, but root may be trimmed)

- `mcp/JakartaMigrationTools.java` – in both. Root’s version should only call free tools and return `upgrade_required` for pro tools (no real refactor/runtime execution).
- `mcp/McpSseController.java`, `McpStreamableHttpController.java`, `McpToolsConfiguration.java`, `McpServerInfoConfiguration.java`, `McpToolExecutionException.java`, `SentinelTools.java` – in both; needed for MCP in both builds.

---

## Summary: Pro-Related Files Still in Root

**Pro-only files that are still in the free project** (and already exist in premium) are:

1. **api/** – all 10 files (2 controllers, 2 services, 6 DTOs)
2. **config/** – 9 files: `ApifyBillingService`, `ApifyLicenseService`, `ApifyLicenseProperties`, `StripeLicenseService`, `StripeLicenseProperties`, `LicenseService`, `PlatformBasedLicensingPostProcessor`, `PlatformBasedLicensingConfig`, `PlatformDetectionService`
3. **storage/** – 1 file: `LocalLicenseStorageService`
4. **coderefactoring/** – entire package (domain + service + impl; many files)
5. **runtimeverification/** – entire package (domain + service + impl; many files)

So we **have** left pro-related source files behind in the free project structure; they have not been removed from root, only duplicated in premium.

---

## Recommendation

To align with “all pro source code in the pro folder structure” and “no pro-related files left in the free project”:

1. **Remove from root** the pro-only files listed above (api/, storage/, the 9 config classes, coderefactoring/, runtimeverification/).
2. **Adjust root’s `JakartaMigrationConfig`** so it does not define beans for Apify, Stripe, License, Platform*, RefactoringEngine, MigrationPlanner, RecipeLibrary, RuntimeVerificationModule, ChangeTracker, ProgressTracker. Keep only beans needed for free features (e.g. dependency analysis, source code scanning, feature flags).
3. **Adjust root’s `JakartaMigrationTools`** so it does not depend on MigrationPlanner, RecipeLibrary, RefactoringEngine, RuntimeVerificationModule, ApifyBillingService, StripePaymentLinkService. Keep only DependencyAnalysisModule, DependencyGraphBuilder, SourceCodeScanner, FeatureFlagsService. Implement `createMigrationPlan`, `analyzeMigrationImpact` (if kept in free), and `verifyRuntime` by checking feature flags and returning the `upgrade_required` JSON only (no calls into refactoring or runtime verification).
4. **Optional:** Add a small shared “stub” or interface in root for migration impact (e.g. minimal `MigrationImpactSummary` or a facade) only if the free build still exposes `analyzeMigrationImpact` without full coderefactoring; otherwise remove or gate that tool in root.

After this, the free project would contain only free feature code plus the shared gating/tier config; all pro implementations would live only under the premium folder structure.

---

## File-level checklist (pro-only files in root)

Use this list to verify removal from `src/main/java/adrianmikula/jakartamigration/` (root only). All of these exist in `jakarta-migration-mcp-premium/src/main/java/...` and should not remain in the free project.

**api/** (10 files)  
- `api/controller/LicenseApiController.java`  
- `api/controller/StripeWebhookController.java`  
- `api/service/CreditService.java`  
- `api/service/StripePaymentLinkService.java`  
- `api/dto/ConsumeCreditsRequest.java`  
- `api/dto/ConsumeCreditsResponse.java`  
- `api/dto/CreditBalanceResponse.java`  
- `api/dto/LicenseValidationResponse.java`  
- `api/dto/PaymentLinkResponse.java`  
- `api/dto/SyncCreditsResponse.java`  

**config/** (9 files)  
- `config/ApifyBillingService.java`  
- `config/ApifyLicenseService.java`  
- `config/ApifyLicenseProperties.java`  
- `config/StripeLicenseService.java`  
- `config/StripeLicenseProperties.java`  
- `config/LicenseService.java`  
- `config/PlatformBasedLicensingPostProcessor.java`  
- `config/PlatformBasedLicensingConfig.java`  
- `config/PlatformDetectionService.java`  

**storage/** (1 file)  
- `storage/service/LocalLicenseStorageService.java`  

**coderefactoring/** (33 files – domain + service + impl)  
- All files under `coderefactoring/domain/` and `coderefactoring/service/` (including `impl/CodeRefactoringModuleImpl.java`).  

**runtimeverification/** (28 files – domain + service + impl)  
- All files under `runtimeverification/domain/` and `runtimeverification/service/` (including `impl/`).  

**Total:** 10 + 9 + 1 + 33 + 28 = **81 pro-only source files** still in root that should exist only in the premium folder structure.
