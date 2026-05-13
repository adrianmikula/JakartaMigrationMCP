# Plan: Correct Dependency Scanning Logical Flow

## Current State Analysis

### Existing Components
1. **CompatibilityConfigLoader** (`premium-core-engine/src/main/java/.../config/`)
   - Loads `compatibility.yaml` with whitelist/blacklist categories (jdk, safe, review, upgrade)
   - Classifies artifacts: `JDK_PROVIDED`, `JAKARTA_REQUIRED`, `CONTEXT_DEPENDENT`, `UNKNOWN`

2. **TransitiveDependencyScannerImpl** (`premium-core-engine/src/main/java/.../impl/`)
   - Builds dependency tree via `mvn dependency:tree` or `gradle dependencies`
   - Uses **hardcoded** `KNOWN_JAVAX_DEPENDENCIES` map (DOES NOT use CompatibilityConfigLoader)
   - Flags javax dependencies but no JAR scanning integration
   - No incompatibility propagation up the tree

3. **ThirdPartyLibScannerImpl** (`premium-core-engine/src/main/java/.../impl/`)
   - Scans build files with separate hardcoded `KNOWN_LIBRARIES` map
   - Independent of transitive scanner

4. **JarCompatibilityScanner** (`premium-core-engine/src/main/java/.../jaranalysis/service/`)
   - Scans JAR bytecode and produces `JarCompatibilityReport` with `JarCompatibilityLevel` (JAKARTA, JAVAX, MIXED, UNKNOWN)
   - Works on file paths or resolved artifacts

5. **ImprovedMavenCentralLookupService** (community-core-engine)
   - Queries Maven Central for Jakarta equivalents as fallback

## Gaps vs. Requirements

| Requirement | Current State | Gap |
|-------------|--------------|-----|
| 1. Use YAML whitelist/blacklist for quick matches | Partial - CompatibilityConfigLoader exists but NOT used in transitive scanner | Need to integrate |
| 2. Process order: top-level → transitive → transitive-of-transitive | Transitive scanner gets full tree but doesn't process in that order for JAR scanning | Need ordered iteration |
| 3. For each dependency: whitelist/blacklist → JAR bytecode scan | No JAR scanning integration in dependency flow | Need integration |
| 4. Incompatibility propagates upward | No propagation logic | Need to implement |
| 5. Scan result metadata with precise reason | `TransitiveDependencyUsage` has severity/recommendation but not structured reason codes | Need to extend |
| 6. 'Unknown' dependencies get Maven lookup fallback | ThirdPartyLibScanner does some lookup but not integrated in main flow | Need integration |

## Proposed Implementation Plan

### Phase 1: Domain Model - Add ScanReason Enum
**New file:**
- `premium-core-engine/src/main/java/adrianmikula/jakartamigration/advancedscanning/domain/ScanReason.java`

Values (with numeric priority for ordering if needed):
```java
public enum ScanReason {
    WHITELISTED,           // in whitelist (safe/jdk) - no migration needed
    BLACKLISTED,           // in blacklist (upgrade) - needs migration
    BYTECODE_SCAN_JAVAX,   // bytecode scan detected javax only
    BYTECODE_SCAN_JAKARTA, // bytecode scan detected jakarta only
    BYTECODE_SCAN_MIXED,   // bytecode scan detected both
    BYTECODE_SCAN_UNKNOWN, // bytecode scan inconclusive
    MAVEN_LOOKUP_FOUND,    // Maven Central found Jakarta equivalent
    MAVEN_LOOKUP_NONE,     // Maven Central found no equivalent
    TRANSITIVE_INCOMPATIBLE, // marked due to child dependency incompatibility
    REVIEW_REQUIRED,       // context-dependent or ambiguous
    UNKNOWN                // no information available
}
```

### Phase 2: Enhance TransitiveDependencyUsage with Metadata
**Modify:** `TransitiveDependencyUsage.java`

Add new fields:
```java
private final ScanReason scanReason;
private final String detailMessage;
private final double confidence;
private final boolean incompatibilityFromTransitive;
```

**Backward compatibility:** Keep existing constructors, add new ones with reason fields. Existing code uses all-args constructor; we'll add parameters at the end to avoid breaking changes.

### Phase 3: Integrate CompatibilityConfigLoader into TransitiveScanner
**Modify:** `TransitiveDependencyScannerImpl.java`

Changes:
- Add `CompatibilityConfigLoader` field (initialize via constructor)
- **Remove** hardcoded `KNOWN_JAVAX_DEPENDENCIES` static block entirely
- In `convertTreeResult()` or new processing method:
  1. Classify each dependency via `compatibilityConfigLoader.classifyArtifact(groupId, artifactId)`
  2. Map classification to `ScanReason`:
     - `JDK_PROVIDED` → `WHITELISTED` (with detail: "JDK-provided package")
     - `JAKARTA_REQUIRED` → `BLACKLISTED` (with detail: "Configured upgrade required")
     - `CONTEXT_DEPENDENT` → `REVIEW_REQUIRED` (with detail: "Context-dependent, review needed")
     - `UNKNOWN` → `UNKNOWN` (will be processed in next phase)

- Process dependencies **in breadth-first order** (by increasing depth). This ensures top-level dependencies are classified before their transitive children.
- For each `UNKNOWN` dependency (and also for `CONTEXT_DEPENDENT` ones), schedule for JAR scanning (Phase 4).

### Phase 4: JAR Scanning Integration with Caching
**New service: `DependencyJarScanner.java` (interface) + `DefaultDependencyJarScanner.java` (impl)**
Location: `premium-core-engine/src/main/java/.../advancedscanning/service/impl/`

Responsibilities:
- Accept `Artifact` (groupId, artifactId, version) and `TransitiveDependencyUsage` builder
- Use `JarResolver` to locate JAR in local caches
- If JAR found: call `JarCompatibilityScanner.analyzeArtifact()` and get `JarCompatibilityReport`
- Map `JarCompatibilityLevel` to `ScanReason`:
  - `JAVAX` → `BYTECODE_SCAN_JAVAX`
  - `JAKARTA` → `BYTECODE_SCAN_JAKARTA`
  - `MIXED` → `BYTECODE_SCAN_MIXED`
  - `UNKNOWN` → `BYTECODE_SCAN_UNKNOWN`
- Update the `TransitiveDependencyUsage` with enhanced metadata (reason, confidence from report, detail message)
- If JAR not found or scan fails: return `UNKNOWN` (to be handled in Phase 5)

**Modify `TransitiveDependencyScannerImpl`:**
- After dependency tree extraction, batch `UNKNOWN`/`CONTEXT_DEPENDENT` artifacts
- Call new service to scan them (respecting max parallelism config)
- Update corresponding `TransitiveDependencyUsage` objects with scan results

### Phase 5: Maven Central Fallback for Remaining Unknowns
**In `TransitiveDependencyScannerImpl` (after JAR scanning):**
- For any usage still with `scanReason == UNKNOWN` or `BYTECODE_SCAN_UNKNOWN`:
  - Call `ImprovedMavenCentralLookupService.findJakartaEquivalents(groupId, artifactId)`
  - If results non-empty:
    - Set reason = `MAVEN_LOOKUP_FOUND`
    - Set `recommendation` to first Jakarta equivalent coordinate
    - Set `confidence` = 0.7 (heuristic - Maven lookup found something)
  - If results empty:
    - Set reason = `MAVEN_LOOKUP_NONE`
    - Set `severity` = "low" (nothing found, likely safe but unclassified)

### Phase 6: Incompatibility Propagation Up the Tree
**In `TransitiveDependencyScannerImpl`, after all scanning complete:**
- Build parent→children adjacency list from `DependencyTreeResult.DependencyNode` list (original Maven/Gradle output)
  - Parent key = artifactKey (groupId:artifactId)
  - Children = list of nodes that have this as parent (deduced from tree structure: a child's depth = parent depth + 1, and the tree order)
  - Actually the tree is already hierarchical; the `DependencyNode` objects come from the parsed JSON output in order. We need to track parent references during parsing or reconstruct from depth.

  *Simpler approach:* During initial processing, build a map `childKey → parentKey` as we iterate the tree.
  - In `parseMavenJsonNode()` or in `convertTreeResult()`, as we traverse children recursively, we can record `parentKey` in a map.
  - Store this map in the scanner result for propagation step.

- After scanning, identify all nodes with `compatibilityStatus` indicating incompatibility:
  - `ScanReason.BLACKLISTED`, `BYTECODE_SCAN_JAVAX`, `BYTECODE_SCAN_MIXED`
  - Also include `TRANSITIVE_INCOMPATIBLE` ones already marked

- For each incompatible node:
  - Walk up the parent chain using parent map
  - Mark each ancestor's `TransitiveDependencyUsage` as:
    - `incompatibilityFromTransitive = true`
    - `scanReason = TRANSITIVE_INCOMPATIBLE` (if not already more severe)
    - `severity = "high"` (upgrade from medium/low if needed)
    - Add detail message: "Incompatible due to transitive dependency: [child coordinate]"

**Note:** Top-level dependencies (depth=0) cannot be transitively incompatible since they have no parents.

### Phase 7: Order of Operations Summary
The complete flow per build file:

1. **Extract raw dependency tree** (already done via command + JSON)
2. **Classify via whitelist/blacklist** (using CompatibilityConfigLoader)
   - Set `ScanReason` accordingly
3. **Batch UNKNOWN/CONTEXT_DEPENDENT for JAR scanning**
   - For each, resolve and scan JAR
   - Update `ScanReason` and confidence
4. **Batch remaining UNKNOWN for Maven lookup**
   - Query ImprovedMavenCentralLookupService
   - Update `ScanReason` and recommendation
5. **Build parent map** from tree structure
6. **Propagate incompatibility upward** from any node with incompatible reason
7. **Deduplicate** (already done via `DependencyDeduplicationService`)
8. **Return final `TransitiveDependencyScanResult`** with enriched `TransitiveDependencyUsage` list

### Phase 8: Enhanced Scan Result Metadata
Each `TransitiveDependencyUsage` will contain:
- `scanReason` (enum) - primary reason code
- `detailMessage` (String) - human-readable explanation
- `confidence` (double) - 0.0-1.0 from bytecode scan if available, else heuristic
- `incompatibilityFromTransitive` (boolean) - true if marked due to child
- Existing fields preserved: `groupId`, `artifactId`, `version`, `scope`, `transitive`, `depth`, `severity`, `recommendation`, `javaxPackage`, `alternativeVersions`

### Phase 9: Update `DependencyInfo` Mapping
**Modify:** `AdvancedScanningService.convertToDependencyInfo()`
- Add new fields from `TransitiveDependencyUsage` to `DependencyInfo`:
  - `scanReason`
  - `detailMessage`
  - `confidence`
  - `incompatibilityFromTransitive`
- Extend `DependencyInfo` class with these new properties (keep backward compatibility via setters)

**Modify:** `DependencyInfo.java` (premium-intellij-plugin)
- Add fields + getters/setters

### Phase 10: Update ThirdPartyLibScanner (Optional)
ThirdPartyLibScanner uses separate hardcoded map; consider merging logic or making it reuse `CompatibilityConfigLoader`. For minimal changes, leave as-is or modify to also use config loader.

### Phase 11: Configuration & Testing
1. **Unit Tests** for `TransitiveDependencyScannerImpl`:
   - Test classification → reason mapping
   - Test JAR scanning integration (mock scanner)
   - Test Maven lookup fallback (mock service)
   - Test propagation: child incompatible → ancestors marked `TRANSITIVE_INCOMPATIBLE`
   - Test breadth-first processing order verification
   - Test that whitelist/blacklist takes precedence over other checks

2. **Integration Test** with real sample project having known javax dependencies at various depths.

3. **Ensure backward compatibility** with existing tests; update failing ones to include new fields.

### Implementation Order (by risk/priority)
1. Phase 1 & 2: Domain model changes (ScanReason, enhanced Usage) [low risk]
2. Phase 3: Integrate CompatibilityConfigLoader [low risk]
3. Phase 4: New JAR scanning service [medium risk - needs artifact resolution]
4. Phase 5: Maven lookup fallback [low risk]
5. Phase 6: Propagation logic [medium risk - tree walking]
6. Phase 7: Cleanup and deduplication integration [low]
7. Phase 8: DependencyInfo update [low]
8. Phase 9: Testing & verification [essential]

## Key Implementation Notes

- **Artifact Resolution:** `JarResolver` only checks local caches. If not found, bytecode scan is skipped → falls back to Maven lookup. This is acceptable; we don't want to download large JARs on the fly.

- **Caching:** `JarCompatibilityScanner` already has Guava cache. Reuse it; don't add another layer.

- **Parallelism:** JAR scanning already supports parallel execution via `DefaultJarCompatibilityScanner` config. Limit concurrency via `JarScanningConfig` (already has `maxParallelism`).

- **Scope Handling:** Respect Maven/Gradle scopes. Test dependencies may be excluded from deep scanning based on config (already have `MAVEN_SCOPES`/`GRADLE_SCOPES`).

- **Performance:** Only scan UNKNOWN dependencies with JAR scanning, not all. Use existing deduplication to avoid duplicate scans.

- **Backward Compatibility:** Add new fields to `TransitiveDependencyUsage` as final fields with builder-like constructor pattern (last parameters). Existing tests that use all-args constructor may need updates; add overloaded constructors or use builder pattern if acceptable.

## Files to Modify (Summary)

**Domain:**
- ScanReason.java (new)
- TransitiveDependencyUsage.java (enhanced)
- DependencyInfo.java (intellij-plugin, enhanced)

**Core Scanning:**
- TransitiveDependencyScannerImpl.java (major refactor)
- DependencyJarScanner.java + DefaultDependencyJarScanner.java (new)

**Configuration:**
- (none new; reuse existing)

**Testing:**
- TransitiveDependencyScannerImplTest.java (add tests)
- TransitiveDependencyScannerIntegrationTest.java (add tests)
- Update any tests constructing TransitiveDependencyUsage with new constructor signature

**UI (if time/compatibility allows):**
- DependencyInfo → UI table display of reason (tooltip or column)

## Success Criteria
- [ ] Whitelist/blacklist checked first for ALL dependencies
- [ ] Unknown dependencies undergo JAR bytecode scanning
- [ ] Incompatible transitive deps propagate incompatibility to ancestors
- [ ] Each dependency record includes structured `ScanReason`
- [ ] Maven Central lookup used as final fallback
- [ ] Existing unit tests pass
- [ ] New tests cover propagation and reason codes

## Detailed Step Sequence

### Step 1: Create `ScanReason` enum
Location: `premium-core-engine/src/main/java/.../advancedscanning/domain/ScanReason.java`

Values:
- `WHITELISTED` (in whitelist - safe, no migration)
- `BLACKLISTED` (in blacklist/upgrade list - needs migration)
- `BYTECODE_SCAN_JAVAX` (bytecode scan detected javax)
- `BYTECODE_SCAN_JAKARTA` (bytecode scan detected jakarta)
- `BYTECODE_SCAN_MIXED` (bytecode scan detected both)
- `BYTECODE_SCAN_UNKNOWN` (bytecode scan inconclusive)
- `MAVEN_LOOKUP_FOUND` (Maven Central found Jakarta equivalent)
- `MAVEN_LOOKUP_NONE` (Maven Central found no equivalent)
- `TRANSITIVE_INCOMPATIBLE` (dependency incompatible due to child)
- `REVIEW_REQUIRED` (context-dependent or no data)
- `UNKNOWN` (no information available)
- `JDK_PROVIDED` (JDK internal, no migration needed)

### Step 2: Enhance `TransitiveDependencyUsage`
Add fields:
```java
private final ScanReason scanReason;
private final String scanDetailMessage;
private final double confidenceScore;
private final boolean incompatibilityFromTransitive;
```

### Step 3: Modify `CompatibilityConfigLoader`
Ensure it exposes classification as `ScanReason` values directly.

### Step 4: Refactor `TransitiveDependencyScannerImpl`
- Add `CompatibilityConfigLoader` field
- Add `JarCompatibilityScanner` field (or a wrapper service)
- Add `ImprovedMavenCentralLookupService` field
- Implement ordered processing (by depth: top-level then breadth-first through transitive)
- For each dependency:
  1. Query `CompatibilityConfigLoader.classifyArtifact()`
  2. Map classification to `ScanReason` (WHITELISTED, BLACKLISTED, REVIEW_REQUIRED, UNKNOWN)
  3. If `UNKNOWN` and has javax in coordinates (or depth <= 2 for priority):
     - Resolve and scan JAR via `JarCompatibilityScanner`
     - Update reason based on `JarCompatibilityLevel`
  4. If still `UNKNOWN`:
     - Query `ImprovedMavenCentralLookupService`
     - Update reason accordingly
- After all scanning:
  - Build dependency graph (parentId→children)
  - Propagate incompatibility upward
  - Set `incompatibilityFromTransitive` on affected usages

### Step 5: Update `ThirdPartyLibScannerImpl`
Consider merging with or using same `CompatibilityConfigLoader` to avoid duplication.

### Step 6: Update `DependenciesTableComponent`
Display new `scanReason` in UI (tooltip or status column).

### Step 7: Test Coverage
- Unit tests for classification → reason mapping
- Unit tests for JAR scanning integration (mocked)
- Unit tests for propagation logic
- Integration test with sample project

## Configuration Integration
Uses existing `compatibility.yaml` with categories mapped to reasons:
- `jdk` → `JDK_PROVIDED`
- `safe` → `WHITELISTED`
- `upgrade` → `BLACKLISTED`
- `review` → `REVIEW_REQUIRED`

## Dependencies
- `JarCompatibilityScanner` (premium-core-engine)
- `CompatibilityConfigLoader` (premium-core-engine)
- `ImprovedMavenCentralLookupService` (community-core-engine)
- Artifact resolution mechanism (may need to resolve JARs from Maven repository)

## Risks & Mitigations
1. **JAR resolution may fail if artifact not in local repo** → Resolve on-demand from Maven Central (needs download capability)
2. **Scanning many JARs could be slow** → Process only UNKNOWN ones, use caching (`JarCompatibilityScanner` already caches)
3. **Memory usage with large dependency trees** → Process in batches, limit concurrent JAR scans
4. **Backward compatibility with existing UI** → Keep existing fields, add new ones as optional

## Success Criteria
- [ ] Whitelist/blacklist checked first for ALL dependencies
- [ ] Unknown dependencies undergo JAR bytecode scanning
- [ ] Incompatible transitive deps propagate incompatibility to ancestors
- [ ] Each dependency record includes structured `ScanReason`
- [ ] Maven Central lookup used as final fallback
- [ ] Existing unit tests pass
- [ ] New tests cover propagation and reason codes
