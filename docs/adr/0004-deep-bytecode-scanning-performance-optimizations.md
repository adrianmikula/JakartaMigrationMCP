# ADR 0004: Deep/Bytecode Scanning Performance Optimizations

## Status
Accepted

## Context
The deep transitive dependency and bytecode scanning implementation had several performance bottlenecks that significantly impacted scan times, especially for projects with many unknown dependencies:

1. **Sequential JAR scanning**: JAR resolution and bytecode scanning were performed sequentially in the main loop for each UNKNOWN/REVIEW_REQUIRED dependency, causing I/O and ASM parsing to block on each dependency.

2. **Sequential Maven Central lookups**: Network-bound Maven Central lookups were performed sequentially one at a time instead of in parallel.

3. **Inefficient incompatibility propagation**: Multiple intermediate collections and stream passes were used, with duplicate processing of the same parent nodes.

4. **Unnecessary collection resizing**: Collections in BytecodeSignalExtractor were created without initial capacity estimates, causing multiple resize operations for large JARs.

5. **Repeated DOM parser creation**: DocumentBuilderFactory and DocumentBuilder were created fresh for each pom.xml parse, which is expensive.

6. **Repeated artifact classification**: The same artifact could be classified multiple times across different dependency nodes.

7. **No early exit in bytecode scanning**: All classes were scanned even after finding definitive javax/jakarta signals.

8. **O(n*m) version resolution**: Maven version resolution used nested loops through dependencyManagement and dependencies for each version lookup.

9. **Unbounded directory traversal**: Files.walk() had no depth limit when searching for build files.

10. **O(n) cache lookups**: DefaultJarCompatibilityScanner iterated through the entire cache map to find matching artifact coordinates.

11. **Unnecessary sorting**: Dependencies were sorted by depth using O(n log n) sort with minimal benefit.

## Decision
We implemented a comprehensive set of performance optimizations across multiple components:

### High Impact Optimizations

**Pre-size collections in BytecodeSignalExtractor**:
- Estimated collection sizes based on maxClasses parameter to avoid multiple resize operations
- Expected impact: 10-15% faster for large JARs, reduced memory churn

**Reuse DOM parser instances in MavenDependencyGraphBuilder**:
- DocumentBuilderFactory and DocumentBuilder made instance fields initialized once in constructor
- Expected impact: 10-20% faster for projects with many Maven modules

**Add classification caching in TransitiveDependencyScannerImpl**:
- HashMap cache keyed by "groupId:artifactId" to avoid repeated artifact classification calls
- Expected impact: 10-20% faster for projects with many duplicate artifact references

**Batch JAR resolution and scanning**:
- Collect all dependencies requiring JAR scanning into a batch
- Resolve JAR paths and scan in parallel using parallelStream
- Merge results back into usages list
- Expected impact: 3-5x faster for projects with 10+ unknown dependencies

**Batch Maven Central lookups**:
- Collect all dependencies requiring Maven Central lookup
- Perform lookups in parallel using parallelStream
- Merge results back into usages list
- Expected impact: 5-10x faster for projects with 10+ unknown dependencies (network-bound)

### Medium Impact Optimizations

**Optimize incompatibility propagation**:
- Single pass through parentMap instead of building separate incompatibleKeys set
- Track already-marked nodes to avoid duplicate processing
- Expected impact: 20-30% faster propagation for large trees

**Implement early exit in BytecodeSignalExtractor**:
- Track running score based on javax/jakarta class counts
- Stop scanning when score exceeds earlyExitThreshold from JarScanningConfig
- Expected impact: 30-50% faster for JARs with clear javax/jakarta signals in early classes

**Optimize Maven version resolution**:
- Build lookup maps once: Map<String, String> depMgmtVersions keyed by "groupId:artifactId"
- Build properties map once: Map<String, String> propertiesMap
- Use O(1) map lookups instead of O(n*m) nested loops
- Expected impact: 50-70% faster version resolution for large pom.xml files

### Low Impact Optimizations

**Add depth limit to build file search**:
- Files.walk() now limited to maxDepth (configurable via system property `maven.build.search.maxDepth`, default 4)
- Expected impact: Prevents excessive directory traversal in deep project structures

**Optimize cache lookup in DefaultJarCompatibilityScanner**:
- Maintain separate index Map<String artifactCoordinate, String cacheKey>
- Update index when cache entries are added/evicted
- Use index for O(1) lookups instead of O(n) iteration
- Expected impact: Significant for large caches (1000+ entries), negligible for small caches

**Remove unnecessary sorting in convertTreeResult**:
- Removed O(n log n) depth sorting as tree structure from dependency commands is already reasonably ordered
- Expected impact: 5-10% faster for large dependency trees

## Consequences

### Positive
- **Significant performance gains**: 3-10x faster for projects with many unknown dependencies
- **Reduced memory churn**: Pre-sized collections and caching reduce garbage collection pressure
- **Better scalability**: Parallel processing enables efficient handling of large dependency trees
- **Configurable behavior**: Early exit and depth limit are configurable via system properties and JarScanningConfig

### Negative
- **Increased complexity**: Batch processing adds more code paths and merge logic
- **Thread safety considerations**: ConcurrentHashMap and parallel streams require careful handling
- **Cache index maintenance**: Additional complexity to keep cache index in sync with cache
- **Configuration tuning**: Early exit threshold and depth limit may need tuning for different project types

### Neutral
- **Classification cache**: Cache is per-instance and cleared between scans, which is appropriate for the use case
- **DOM parser reuse**: DocumentBuilder is not thread-safe but is used synchronously within the same instance
