# Dependency Tree Performance Tech Debt

## Current State

The deep transitive dependency scanner builds a complete in-memory graph of all dependencies across all build files in a project. For typical Java projects:

- **Direct dependencies**: 10-100
- **Transitive dependencies**: 100-1,000 (large monorepos: up to 5,000)
- **Memory footprint**: ~1-5MB per scan (Artifact ≈ 80-120 bytes, Dependency ≈ 100-150 bytes)
- **Graph construction**: Sub-millisecond to a few milliseconds

This is well within IntelliJ plugin memory budgets (100-500MB typical heap).

## Known Performance Risks

### 1. Late Deduplication (CRITICAL BUG)
- **Location**: `TransitiveDependencyScannerImpl.convertTreeResult()` line 325
- **Issue**: Edges are captured after `deduplicationService.deduplicate(usages)`, causing parent-child relationships to be lost when multiple versions of the same artifact are merged
- **Fix**: Capture parent→child edges from `parentMap` **before** deduplication, include in `TransitiveDependencyScanResult`
- **Priority**: HIGH - causes incorrect dependency tree visualization

### 2. Missing Edge Data (CRITICAL BUG)
- **Location**: `TransitiveDependencyScannerImpl.convertTreeResult()` - builds `parentMap` but never exposes it
- **Issue**: UI graph cannot reconstruct tree relationships without explicit edge data
- **Fix**: Add `List<TransitiveDependencyEdge>` to `TransitiveDependencyScanResult`, populate before deduplication
- **Priority**: HIGH - breaks dependency graph

### 3. Transitive Flag Mislabeling
- **Location**: `MavenDependencyGraphBuilder` line 106 - direct dependencies incorrectly marked `transitive=true`
- **Issue**: Direct deps appear as transitive in UI; counts inflated
- **Fix**: Change `new Artifact(..., true)` → `false` for direct deps; Gradle parser has same issue at line 277
- **Priority**: HIGH - UI misinformation

### 4. All-Dependencies Capture (ALREADY FIXED)
- Previously only javax dependencies were captured; now all are included
- Remaining: Ensure `MavenDependencyGraphBuilder` and Gradle parser also capture all (not filtered)

## Potential Optimizations (Future Work)

### A. Streaming / Cursor-based Processing
**When needed**: Projects with >5,000 dependencies or memory-constrained environments

**Approach**:
- Process `DependencyTreeResult.DependencyNode` stream in depth-first order
- Use `Iterator<Dependency>` pattern with `Spliterator` for parallel streams
- Emit edges incrementally to a `Collector` that builds `DependencyGraph` on-the-fly
- Avoid intermediate `List<TransitiveDependencyUsage>` holding all nodes in memory simultaneously

**Implementation**:
```java
// Instead of: List<TransitiveDependencyUsage> usages = new ArrayList<>(totalNodes);
// Use: Stream<TransitiveDependencyUsage> usageStream = sortedNodes.stream().map(this::enrich);
// Then: List<TransitiveDependencyUsage> usages = usageStream.collect(Collectors.toList());
// Already streaming, but parentMap building could be integrated
```

### B. Depth Limiting (Configurable)
**When needed**: Extremely deep dependency trees (>15 levels) rare in Java but possible with multi-module aggregators

**Approach**: Add `MaxDepth` config to `TransitiveDependencyScanner` (default 10-15). Discard deeper nodes with warning log.

**Trade-off**: Loses visibility into very deep transitive chains, but most migration-critical deps appear at depth ≤3.

### C. Compressed Artifact Representation
**When needed**: Memory profiling shows artifact metadata dominating heap

**Approach**:
- Intern `groupId` and `artifactId` strings via `String.intern()` or custom pool
- Store `scope` as enum/index instead of String
- Use `record ArtifactKey(String gid, String aid)` as map key, then store version separately in `ArtifactDetails`
- Consider `short` for depth field (max depth < 32767)

**Trade-off**: Adds complexity for ~30-50% memory reduction on large graphs

### D. Lazy Graph Construction
**When needed**: UI graph component only needs subset of edges for current zoom level

**Approach**:
- Store raw `parentMap` and `usages` without building `DependencyGraph`
- Build `DependencyGraph` lazily when `DependencyGraphComponent` requests it
- Cache built graph with `SoftReference`

**Trade-off**: Slightly slower first render, but reduces memory if graph never displayed

### E. Parallelism Tuning
**Current**: `MAX_PARALLELISM = 4` (system property override)
**Risk**: Large projects with many modules (20+) can cause OOM if all scanned in parallel

**Already mitigated**: `scanExecutor` is fixed thread pool (size 2-4); memory checks in `AdvancedScanningService` reduce parallelism if heap low

### F. Edge Deduplication Before Graph Build
**Current**: Edges collected pre-deduplication may contain duplicates (same parent-child pair from different versions)
**Fix**: Deduplicate edges by `parentArtifactKey:childArtifactKey` before constructing `DependencyGraph`
**Priority**: MEDIUM - harmless but inflates edge count

## Recommended Action Order

1. **NOW**: Implement critical bug fixes (items 1-3 above) as per current plan
2. **NEXT**: Add metrics/logging to measure:
   - `LOG.info("Dependency tree size: {} nodes, {} edges, {} MB", nodes.size(), edges.size(), Runtime.getRuntime().totalMemory()/1024/1024)`
   - Track max depth encountered
3. **LATER**: If users report OOM on large repos (>50 modules, >2000 dependencies):
   - Implement depth limiting (configurable)
   - Add streaming edge collection
4. **ONGOING**: Profile heap dumps from real-world scans to identify actual pressure points

## Related Issues

- see ADR-2025-05-07: Dependency Graph Edge Tracking (for edge capture design)
- MigrationToolWindow deep scan completion (dashboard integration)
