# Functionality Gap Analysis

**Date:** 2026-02-04  
**Based On:** [`docs/research/market-analysis.md`](../research/market-analysis.md)  
**Purpose:** Review current functionality and identify gaps/weaknesses vs. market requirements

---

## Executive Summary

| Status | Count | Description |
|--------|-------|-------------|
| ✅ Implemented | 6/10 | Core MCP tools matching market requirements |
| ⚠️ Partial | 2/10 | Tools exist but need enhancement |
| ❌ Missing | 4/10 | Tools not yet implemented |

**Overall Readiness:** ~60% of market-researched functionality implemented

---

## 1. Market Research Requirements vs. Current Implementation

### 1.1 Core MCP Tools (from market analysis)

| Required Tool | Current Status | Gap Analysis |
|---------------|----------------|--------------|
| `analyze_jakarta_readiness` | ✅ `analyzeJakartaReadiness` | Fully implemented in [`CommunityMigrationTools.java`](../../mcp-server/src/main/java/adrianmikula/jakartamigration/mcp/CommunityMigrationTools.java) |
| `get_migration_blueprint` | ✅ `createMigrationPlan` | Implemented in [`JakartaMigrationTools.java`](../../mcp-server/src/main/java/adrianmikula/jakartamigration/mcp/JakartaMigrationTools.java) (PREMIUM) |
| `refactor_namespace` | ⚠️ `applyAutoFixes` | Basic implementation exists. **Gap:** No support for XML namespace refactoring (persistence.xml, web.xml) |
| `resolve_jakarta_coordinates` | ⚠️ `recommendVersions` | Partial implementation. **Gap:** No direct artifact lookup; relies on static mapping |
| `explain_runtime_failure` | ❌ Missing | **Gap:** No stack trace analyzer MCP tool |

### 1.2 Technical Requirements (from market analysis)

| Requirement | Current Status | Gap Analysis |
|-------------|----------------|--------------|
| Bytecode Analysis | ⚠️ Partial | [`AsmBytecodeAnalyzer.java`](../../migration-core/src/main/java/adrianmikula/jakartamigration/runtimeverification/service/impl/AsmBytecodeAnalyzer.java) exists but **no MCP tool wrapper** |
| Maven/Gradle Graph Awareness | ⚠️ Partial | [`MavenDependencyGraphBuilder.java`](../../migration-core/src/main/java/adrianmikula/jakartamigration/dependencyanalysis/service/impl/MavenDependencyGraphBuilder.java) exists but **limited transitive analysis** |
| Reflection Detection | ❌ Missing | Source code scanner finds imports but **misses string-based `javax` references** (e.g., `Class.forName("javax.servlet.Filter")`) |

---

## 2. Detailed Gap Analysis

### 2.1 Missing MCP Tools (Priority: High)

#### Gap 1: `explainRuntimeFailure` / Stack Trace Analyzer
**Why it matters:** Market research emphasizes this as a key differentiator vs. OpenRewrite. The tool should:
- Parse stack traces
- Identify missing `javax` classes
- Recommend specific dependency additions

**Current state:** [`ErrorAnalyzer.java`](../../migration-core/src/main/java/adrianmikula/jakartamigration/runtimeverification/service/ErrorAnalyzer.java) exists but **not exposed as MCP tool**

**Implementation estimate:** 2-3 days

#### Gap 2: Direct Artifact Lookup (`resolveJakartaCoordinate`)
**Why it matters:** Users want to ask "What is the Jakarta equivalent of `javax.servlet:javax.servlet-api:4.0.1`?"

**Current state:** [`JakartaMappingService.java`](../../migration-core/src/main/java/adrianmikula/jakartamigration/dependencyanalysis/service/JakartaMappingService.java) exists but:
- Only supports known artifacts
- No Maven Central lookup capability
- No version range mapping

**Implementation estimate:** 1-2 days

#### Gap 3: Transitive Dependency Analyzer
**Why it matters:** Market research identifies "dependency hell" as a major pain point. The tool should:
- Find hidden `javax` dependencies in transitive JARs
- Suggest exclude statements
- Recommend alternative artifacts

**Current state:** [`DependencyGraphBuilder.java`](../../migration-core/src/main/java/adrianmikula/jakartamigration/dependencyanalysis/service/DependencyGraphBuilder.java) exists but:
- Limited to direct dependencies
- No bytecode scanning of JARs
- No exclude recommendation engine

**Implementation estimate:** 3-4 days

#### Gap 4: Reflection-based `javax` Detector
**Why it matters:** Many legacy apps use `Class.forName("javax.servlet.http.HttpServlet")` which regex-based scanners miss.

**Current state:** [`SourceCodeScannerImpl.java`](../../migration-core/src/main/java/adrianmikula/jakartamigration/sourcecodescanning/service/impl/SourceCodeScannerImpl.java) exists but:
- Only scans import statements
- Ignores string literals
- Misses XML descriptors

**Implementation estimate:** 2-3 days

---

### 2.2 Enhanced Functionality Needed (Priority: Medium)

#### Gap 5: XML Namespace Refactoring
**Current state:** `applyAutoFixes` handles Java imports but **not XML namespaces**.

**Affected files:**
- `persistence.xml` - `xmlns="http://xmlns.jcp.org/xml/ns/persistence"`
- `web.xml` - `xmlns="http://xmlns.jcp.org/xml/ns/javaee"`
- `faces-config.xml` - Similar patterns

**Implementation estimate:** 1-2 days

#### Gap 6: Gradle Build Script Support
**Current state:** [`MavenDependencyGraphBuilder.java`](../../migration-core/src/main/java/adrianmikula/jakartamigration/dependencyanalysis/service/impl/MavenDependencyGraphBuilder.java) only parses **Maven** `pom.xml`.

**Gap:** No support for:
- `build.gradle` / `build.gradle.kts` dependency blocks
- Version catalog (`gradle/libs.versions.toml`)
- Platform BOMs

**Implementation estimate:** 3-4 days

#### Gap 7: Dry Run Improvements
**Current state:** `applyAutoFixes` supports `dryRun` parameter but:
- Doesn't show backup diff
- Doesn't estimate risk level per change
- No interactive confirmation

**Implementation estimate:** 1 day

---

### 2.3 Quality/Reliability Gaps (Priority: Low)

| Gap | Description | Impact |
|-----|-------------|--------|
| Error recovery | No rollback mechanism if migration fails mid-process | User trust |
| Progress reporting | Long migrations have no real-time progress updates | UX |
| Caching | Dependency analysis repeated on every run | Performance |
| Parallel processing | Single-threaded file scanning | Performance at scale |

---

## 3. Architecture Strengths

The following functionality is **well-implemented** and competitive:

### 3.1 Dependency Analysis
- ✅ [`DependencyGraph.java`](../../migration-core/src/main/java/adrianmikula/jakartamigration/dependencyanalysis/domain/DependencyGraph.java) - Clean domain model
- ✅ [`MavenDependencyGraphBuilder`](../../migration-core/src/main/java/adrianmikula/jakartamigration/dependencyanalysis/service/impl/MavenDependencyGraphBuilder.java) - Working Maven parsing
- ✅ [`Blocker`](../../migration-core/src/main/java/adrianmikula/jakartamigration/dependencyanalysis/domain/Blocker.java) domain - Good classification system

### 3.2 Code Refactoring
- ✅ [`RefactoringEngine`](../../migration-core/src/main/java/adrianmikula/jakartamigration/coderefactoring/service/RefactoringEngine.java) - AST-based approach
- ✅ [`RecipeLibrary`](../../migration-core/src/main/java/adrianmikula/jakartamigration/coderefactoring/service/RecipeLibrary.java) - OpenRewrite integration ready
- ✅ [`MigrationPlan`](../../migration-core/src/main/java/adrianmikula/jakartamigration/coderefactoring/domain/MigrationPlan.java) domain - Comprehensive planning

### 3.3 Runtime Verification
- ✅ [`BytecodeAnalyzer`](../../migration-core/src/main/java/adrianmikula/jakartamigration/runtimeverification/service/BytecodeAnalyzer.java) - ASM-based scanning
- ✅ [`ErrorPatternMatcher`](../../migration-core/src/main/java/adrianmikula/jakartamigration/runtimeverification/service/ErrorPatternMatcher.java) - Pattern matching for errors

### 3.4 Persistence
- ✅ [`SqliteMigrationAnalysisStore`](../../migration-core/src/main/java/adrianmikula/jakartamigration/analysis/persistence/SqliteMigrationAnalysisStore.java) - Persistent analysis history
- ✅ [`MigrationAnalysisPersistenceService`](../../migration-core/src/main/java/adrianmikula/jakartamigration/analysis/persistence/MigrationAnalysisPersistenceService.java) - Service layer

---

## 4. Implementation Priority Matrix

| Priority | Tool/Feature | Effort | Value | Recommendation |
|----------|--------------|--------|-------|----------------|
| P0 | `explainRuntimeFailure` (stack trace analyzer) | 2-3 days | High | Next sprint |
| P0 | Reflection-based `javax` detector | 2-3 days | High | Next sprint |
| P1 | Direct artifact lookup | 1-2 days | Medium | Month 2 |
| P1 | XML namespace refactoring | 1-2 days | Medium | Month 2 |
| P1 | Transitive dependency analyzer | 3-4 days | High | Month 2 |
| P2 | Gradle support | 3-4 days | Medium | Month 3 |
| P2 | Dry run improvements | 1 day | Low | Month 3 |
| P2 | Progress reporting | 2 days | Low | Month 3 |

---

## 5. Competitive Positioning

### vs. OpenRewrite (Primary Competitor)

| Feature | OpenRewrite | This Project | Advantage |
|---------|-------------|--------------|-----------|
| CLI usage | ✅ | ✅ | Neutral |
| MCP/AI integration | ❌ | ✅ | **Win** |
| Stack trace analysis | ❌ | ⚠️ Partial | Gap |
| IDE integration | Plugin | IntelliJ plugin | Neutral |
| Gradle support | ✅ | ❌ Gap | Loss |
| Open source | ✅ | ✅ (Core) | Neutral |

**Key Differentiation:** AI agentic workflow (Think-Apply-Verify cycle) - this is what OpenRewrite cannot do.

---

## 6. Recommended Next Steps

1. **Immediate (Week 1-2):**
   - Wrap [`ErrorAnalyzer`](../../migration-core/src/main/java/adrianmikula/jakartamigration/runtimeverification/service/ErrorAnalyzer.java) in `explainRuntimeFailure` MCP tool
   - Add string-literal scanning to [`SourceCodeScannerImpl`](../../migration-core/src/main/java/adrianmikula/jakartamigration/sourcecodescanning/service/impl/SourceCodeScannerImpl.java)

2. **Month 2:**
   - Implement direct artifact lookup with Maven Central API
   - Add XML namespace refactoring
   - Build transitive dependency analyzer

3. **Month 3:**
   - Gradle build script support
   - Progress reporting for long migrations
   - Error recovery/rollback mechanism

---

## 7. Files Requiring Changes

| File | Change Type |
|------|-------------|
| `mcp-server/.../JakartaMigrationTools.java` | Add new MCP tool methods |
| `migration-core/.../sourcecodescanning/service/impl/SourceCodeScannerImpl.java` | Add string literal detection |
| `migration-core/.../runtimeverification/service/ErrorAnalyzer.java` | Expose for MCP tool |
| `migration-core/.../dependencyanalysis/service/JakartaMappingService.java` | Add Maven Central lookup |
| `migration-core/.../coderefactoring/service/RefactoringEngine.java` | Add XML refactoring |

---

## Related Documentation

- [`docs/research/market-analysis.md`](../research/market-analysis.md) - Original market research
- [`docs/improvements/OPENCORE_LICENSING_PLAN_2026-02-04.md`](OPENCORE_LICENSING_PLAN_2026-02-04.md) - Licensing roadmap
- [`docs/architecture/README.md`](../architecture/README.md) - Architecture overview
