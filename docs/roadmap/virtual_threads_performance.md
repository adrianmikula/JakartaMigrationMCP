# Virtual Threads for Deep JAR Scanning Performance

## Overview

This document tracks the implementation of virtual threads (Project Loom) to improve the performance and scalability of the deep JAR scanning system for Jakarta EE migration analysis.

## Background

### Current Implementation

The deep JAR scanning system in `DefaultJarCompatibilityScanner` currently uses a traditional `ExecutorService` with a fixed thread pool:

```java
private ExecutorService createExecutor() {
    int parallelism = config.getMaxParallelism();
    ThreadFactory factory = r -> { 
        Thread t = new Thread(r);
        t.setName("jar-scanner-" + t.getId()); 
        t.setDaemon(true); 
        return t; 
    };
    return Executors.newFixedThreadPool(parallelism, factory);
}
```

**Limitations:**
- Fixed thread pool limits concurrency (default: 4 threads)
- Each thread consumes significant memory (~1MB stack)
- Not scalable for projects with hundreds of dependencies
- Thread creation overhead for large JAR sets

### Why Virtual Threads?

Virtual threads (introduced in Java 21) are lightweight threads managed by the JVM:
- **Memory efficient**: ~KB vs MB per thread
- **Scalable**: Can create millions of virtual threads
- **I/O-bound perfect fit**: JAR scanning is I/O-bound (reading files, network)
- **Simple API**: Drop-in replacement for `ExecutorService`

## Implementation Status

### ✅ Completed

#### 1. Test Infrastructure
- Added `isJava21Plus()` helper method to skip virtual thread tests on Java 17
- Updated `virtualThreadsEnabledWithParallelScan()` test to check Java version
- **File**: `premium-core-engine/src/test/java/.../DefaultJarCompatibilityScannerTest.java`

### ⚠️ In Progress

#### 2. Virtual Threads Implementation
**Status**: Blocked by Java 21 adoption limitations

**Planned Changes:**
```java
private ExecutorService createExecutor() {
    if (!config.isParallelScanEnabled()) {
        return Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r); 
            t.setName("jar-scanner-sequential"); 
            t.setDaemon(true);
            return t;
        });
    }
    
    // Use virtual threads when available and enabled
    if (config.isUseVirtualThreads() && isJava21Plus()) {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
    
    // Fallback to fixed pool for Java 17 or when virtual threads disabled
    int parallelism = config.getMaxParallelism();
    ThreadFactory factory = r -> { 
        Thread t = new Thread(r);
        t.setName("jar-scanner-" + t.getId()); 
        t.setDaemon(true); 
        return t; 
    };
    return Executors.newFixedThreadPool(parallelism, factory);
}
```

### ❌ Blocked

#### 3. Java 21 Adoption

**Issue**: The project cannot fully adopt Java 21 due to IntelliJ Platform constraints.

**Root Cause**:
- `premium-intellij-plugin` targets IntelliJ 2023.3.4
- IntelliJ Platform 2023.3.x requires Java 17
- Attempts to upgrade to IntelliJ 2024.1.4 and 2024.2.5 failed:
  - 2024.1.4: Plugin verification failed (still requires Java 17)
  - 2024.2.5: Gradle cache corruption, documentation suggests still requires Java 17

**Impact**:
- Virtual threads (Java 21+) cannot be used in `premium-intellij-plugin`
- `premium-core-engine` could theoretically use Java 21, but:
  - Gradle multi-project constraints may apply
  - Consistency across modules is preferred
  - Test infrastructure needs Java 21 for virtual thread testing

**Documentation**: See `docs/techdebt/java-21-adoption-limitations.md` for detailed analysis.

## Proposed Solutions

### Option 1: Modular Java Version (Recommended)

**Approach**: Allow different modules to use different Java versions

**Implementation**:
- Set `premium-core-engine` to Java 21
- Keep `premium-intellij-plugin` at Java 17
- Add conditional logic in `DefaultJarCompatibilityScanner`:
  ```java
  if (isJava21Plus() && config.isUseVirtualThreads()) {
      return Executors.newVirtualThreadPerTaskExecutor();
  } else {
      // Fallback to fixed pool
  }
  ```

**Pros**:
- Enables virtual threads where possible (standalone tools, MCP servers)
- Maintains IntelliJ plugin compatibility
- Progressive adoption path

**Cons**:
- Adds complexity to build configuration
- Requires careful dependency management
- Tests need to handle both Java 17 and 21

### Option 2: Wait for IntelliJ Platform Java 21 Support

**Approach**: Delay virtual threads until IntelliJ Platform fully supports Java 21

**Implementation**:
- Monitor IntelliJ Platform releases for Java 21 support
- Upgrade IntelliJ plugin when available
- Implement virtual threads after upgrade

**Pros**:
- Simpler build configuration
- Consistent Java version across all modules
- No conditional logic needed

**Cons**:
- Delays performance improvements
- Timeline uncertain (IntelliJ 2025.x?)
- Misses out on virtual thread benefits for other modules

### Option 3: Alternative Performance Optimizations

**Approach**: Implement other performance improvements that work on Java 17

**Options**:
- Increase fixed thread pool size (configurable)
- Implement async I/O with CompletableFuture
- Add more aggressive caching
- Optimize ASM visitor to skip unused class data
- Parallelize at a higher level (per-module instead of per-JAR)

**Pros**:
- Works on Java 17
- Can be implemented immediately
- Still provides performance gains

**Cons**:
- Not as scalable as virtual threads
- More complex implementation
- Limited by OS thread constraints

## Configuration Changes Required

### JarScanningConfig.java

Add new feature flags:

```java
private static class FeatureFlags {
    boolean enableDeepScanning = true;
    boolean enableCaching = true;
    boolean enableParallelScan = true;
    boolean detectShaded = false;
    boolean detectTestScope = true;
    boolean earlyExit = true;
    int earlyExitThreshold = 10;
    
    // New flag for virtual threads
    boolean useVirtualThreads = true;  // Default to true when available
}
```

Deprecate or repurpose `maxParallelism`:
- Keep for Java 17 fallback
- Ignore when virtual threads enabled

### jar-scanning-weights.yaml

```yaml
features:
  enableDeepScanning: true
  enableCaching: true
  enableParallelScan: true
  detectShaded: false
  detectTestScope: true
  earlyExit: true
  earlyExitThreshold: 10
  useVirtualThreads: true  # New flag
```

## Performance Expectations

### Baseline (Current - Fixed Pool)
- Max concurrency: 4 threads (configurable)
- Memory per thread: ~1MB stack
- Scalability: Limited by OS threads
- Best for: Small to medium projects (< 50 JARs)

### With Virtual Threads
- Max concurrency: Unlimited (virtual threads)
- Memory per thread: ~KB
- Scalability: Millions of virtual threads
- Best for: Large projects (100+ JARs)

### Expected Improvements

For a project with 200 JARs:
- **Current**: ~50 seconds (4 threads, ~12.5 JARs/sec)
- **Virtual threads**: ~10-15 seconds (200 concurrent scans, ~13-20 JARs/sec)
- **Improvement**: 3-5x faster for large projects

## Testing Strategy

### Unit Tests
- Test executor creation with `useVirtualThreads = true`
- Test executor creation with `useVirtualThreads = false`
- Test fallback to fixed pool on Java 17
- Test shutdown behavior with virtual thread executor

### Integration Tests
- Scan large JAR sets (100+) with virtual threads
- Compare performance vs fixed pool
- Verify thread naming for debugging
- Verify cache behavior under high concurrency

### Compatibility Tests
- Test on Java 17 (should use fixed pool)
- Test on Java 21 (should use virtual threads)
- Verify feature flag toggles behavior

## Migration Path

### Phase 1: Infrastructure (Current)
- ✅ Add Java version detection
- ✅ Add test skips for Java 17
- ⏳ Add `useVirtualThreads` configuration flag

### Phase 2: Implementation (Blocked)
- ⏳ Implement virtual thread executor creation
- ⏳ Add conditional logic based on Java version
- ⏳ Update shutdown logic for virtual threads

### Phase 3: Testing (Blocked)
- ⏳ Add unit tests for virtual thread path
- ⏳ Add performance benchmarks
- ⏳ Test on both Java 17 and 21

### Phase 4: Rollout (Blocked)
- ⏳ Enable in `premium-core-engine` (if Java 21 adopted)
- ⏳ Enable in `community-core-engine` (if Java 21 adopted)
- ⏳ Document performance improvements
- ⏳ Update user documentation

## Related Documentation

- **Deep JAR Scanning Design**: `docs/roadmap/deep_jar_scanning.md`
- **Java 21 Adoption Limitations**: `docs/techdebt/java-21-adoption-limitations.md`
- **Performance Optimizations ADR**: `docs/adr/0004-deep-bytecode-scanning-performance-optimizations.md`

## Decision Record

### Decision: Pursue Option 1 (Modular Java Version)

**Date**: 2026-05-12
**Status**: Proposed
**Rationale**:
- Enables virtual threads for standalone tools and MCP servers immediately
- Maintains IntelliJ plugin compatibility
- Provides incremental value while waiting for IntelliJ Platform Java 21 support
- Aligns with "efficiency tweaks" principle from development standards

**Next Steps**:
1. Update `premium-core-engine` build.gradle.kts to target Java 21
2. Implement conditional virtual thread logic in `DefaultJarCompatibilityScanner`
3. Add comprehensive testing for both Java 17 and 21
4. Document module-specific Java version requirements
5. Monitor IntelliJ Platform for Java 21 support

**Risks**:
- Build configuration complexity
- Dependency compatibility issues between Java 17 and 21 modules
- Test infrastructure needs to support both versions

**Mitigation**:
- Use Gradle toolchains for per-module Java version
- Clear documentation of version requirements
- Comprehensive CI testing on both Java versions
