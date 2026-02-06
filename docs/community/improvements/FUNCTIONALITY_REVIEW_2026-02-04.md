# Jakarta Migration MCP - Functionality Review & Implementation Gaps

**Date:** 2026-02-04

Based on analysis of [`docs/research/market-analysis.md`](../research/market-analysis.md) and current codebase implementation.

---

## Executive Summary

The Jakarta Migration MCP project has strong foundations with AST-based source code scanning, bytecode analysis, and OpenRewrite refactoring. However, critical gaps exist in reflection detection and AI-guided runtime failure analysis that limit the "Migration Architect in a Box" vision.

---

## Current Functionality Status

### ✅ Fully Implemented Features

| Feature | Location | Status |
|---------|----------|--------|
| `analyzeJakartaReadiness` | [`JakartaMigrationTools.java:88`](../mcp-server/src/main/java/adrianmikula/jakartamigration/mcp/JakartaMigrationTools.java:88) | Complete - AST-based scanning |
| `recommendVersions` | [`JakartaMigrationTools.java:162`](../mcp-server/src/main/java/adrianmikula/jakartamigration/mcp/JakartaMigrationTools.java:162) | Complete - YAML mappings |
| `applyAutoFixes` | [`JakartaMigrationTools.java:344`](../mcp-server/src/main/java/adrianmikula/jakartamigration/mcp/JakartaMigrationTools.java:344) | Complete - OpenRewrite |
| Bytecode Analysis | [`AsmBytecodeAnalyzer.java`](../migration-core/src/main/java/adrianmikula/jakartamigration/runtimeverification/service/impl/AsmBytecodeAnalyzer.java) | Complete - ASM library |
| Dependency Graph | [`MavenDependencyGraphBuilder.java`](../migration-core/src/main/java/adrianmikula/jakartamigration/dependencyanalysis/service/impl/MavenDependencyGraphBuilder.java) | Complete - Maven focus |
| Runtime Verification | [`RuntimeVerificationModuleImpl.java`](../migration-core/src/main/java/adrianmikula/jakartamigration/runtimeverification/service/impl/RuntimeVerificationModuleImpl.java:54) | Complete - Process + Bytecode |

### ⚠️ Partially Implemented Features

| Feature | Gap |
|---------|-----|
| `createMigrationPlan` | Requires PREMIUM license only; lacks granular phase customization |
| Gradle Support | Maven-focused; Gradle projects underserved |
| Recipe Library | Basic Jakarta recipes; custom recipes not supported |
| XML Scanning | Basic regex patterns; full schema validation missing |

### ❌ Missing Features (Critical)

| Feature | Research Requirement | Impact |
|---------|---------------------|--------|
| **Reflection Detection** | String-based javax references (e.g., `Class.forName("javax.servlet.Filter")`) | HIGH |
| **explain_runtime_failure** | AI-guided stack trace analysis | HIGH |
| **Gradle Build Analysis** | `build.gradle` parsing and dependency trees | MEDIUM |
| **Binary JAR Deep Scan** | Transitive dependency javax detection | MEDIUM |

---

## Critical Gaps Analysis

### 1. Reflection and Dynamic Loading Detection

**Research Requirement:** Finding string-based references to `javax` (common in old Spring/Hibernate configs)

**Current State:** [`SourceCodeScannerImpl`](../migration-core/src/main/java/adrianmikula/jakartamigration/sourcecodescanning/service/impl/SourceCodeScannerImpl.java) only detects explicit `import javax.*` statements.

**Missing Patterns:**
```java
Class.forName("javax.servlet.http.HttpServlet")
method.invoke("javax.mail.Message")
"javax." in XML configuration files
Annotation processors with hardcoded class names
```

**Implementation Recommendation:**
```java
// Add to SourceCodeScannerImpl
public List<StringRefUsage> detectStringReferences(Path projectPath) {
    // Scan for patterns like:
    // Class.forName("javax.*")
    // "javax." in configuration files
    // Method handles and reflection API usage
}
```

**Priority:** HIGH | **Effort:** MEDIUM

---

### 2. Explain Runtime Failure Tool

**Research Requirement:** MCP tool to analyze stack traces and suggest specific fixes

**Current State:** [`verifyRuntime`](../mcp-server/src/main/java/adrianmikula/jakartamigration/mcp/JakartaMigrationTools.java:294) returns raw errors without AI-guided remediation.

**Missing Functionality:**
- Stack trace parsing with error categorization
- Correlation with dependency analysis results
- AI-guided fix suggestions

**Implementation Recommendation:**
```java
@McpTool(name = "explain_runtime_failure", description = "Analyzes stack trace and suggests specific fixes")
public String explainRuntimeFailure(
    @McpToolParam(description = "Stack trace to analyze", required = true) String stackTrace,
    @McpToolParam(description = "Path to project root", required = true) String projectPath
);
```

**Priority:** HIGH | **Effort:** MEDIUM

---

### 3. Gradle Dependency Analysis

**Current State:** Maven-focused with [`MavenDependencyGraphBuilder`](../migration-core/src/main/java/adrianmikula/jakartamigration/dependencyanalysis/service/impl/MavenDependencyGraphBuilder.java).

**Missing:**
- `build.gradle` / `build.gradle.kts` parsing
- Gradle dependency tree extraction
- BOM (Bill of Materials) handling for Gradle
- Version catalog support

**Priority:** HIGH | **Effort:** HIGH

---

### 4. Binary Transitive Analysis

**Current State:** [`BytecodeAnalyzer`](../migration-core/src/main/java/adrianmikula/jakartamigration/runtimeverification/service/impl/AsmBytecodeAnalyzer.java) analyzes JAR contents.

**Missing:**
- Detection of javax classes in **transitive** dependencies
- Identification of "shaded" dependencies
- Binary-incompatible JAR recommendations

**Priority:** MEDIUM | **Effort:** HIGH

---

## Monetization Alignment

| Research Recommendation | Current Implementation |
|------------------------|----------------------|
| Free "Compatibility Checker" | ✅ `analyzeJakartaReadiness` available free |
| Premium "One-Click Refactoring" | ✅ `applyAutoFixes` requires PREMIUM |
| Enterprise "Migration Insurance" | ⚠️ Partial - needs `explain_runtime_failure` |

---

## Recommended Implementation Roadmap

### Phase 1: Critical (High Value, Low Effort)
1. **Reflection Detection** - Add string-based javax reference scanning
2. **explain_runtime_failure** - AI-guided error remediation
3. **Enhanced XML Scanning** - persistence.xml, web.xml deep parsing

### Phase 2: Strategic (High Value, Medium Effort)
1. **Gradle Support** - Complete Gradle dependency analysis
2. **Binary Transitive Analysis** - JAR-in-JAR dependency detection
3. **Custom Recipe Support** - User-defined migration rules

### Phase 3: Differentiators (Market Edge)
1. **Migration Insurance Dashboard** - Enterprise reporting
2. **CI/CD Integration Hooks** - Automated migration gates
3. **Legacy Framework Detection** - Identify deprecated frameworks

---

## Technical Debt Notes

1. **Test Coverage** - Verify all MCP tools have corresponding integration tests
2. **Error Handling** - Some tools return generic JSON instead of structured errors
3. **Documentation** - MCP tool descriptions need examples for AI consumption
4. **Performance** - Large projects may timeout; consider async execution

---

## Conclusion

The codebase demonstrates solid architectural foundations. To achieve the "Migration Architect in a Box" vision from the market research, priority should be given to:

1. Reflection detection for legacy Spring/Hibernate patterns
2. AI-guided runtime failure explanation
3. Complete Gradle ecosystem support

These additions would differentiate the tool from standard OpenRewrite recipes and provide the "Senior Gap" solution that enterprises need.
