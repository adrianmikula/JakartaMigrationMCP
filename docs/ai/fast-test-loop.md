# Fast Test Loop

**Goal**: Provide agentic AI with <2s compilation and <2s test feedback for rapid iteration.

## Quick Commands

```bash
# Fastest: compilation only (target: <1s, current: ~0.8s ✅)
mise run build

# Fast unit tests - 3 modules (target: <1.5s, current: ~1.0s ✅)
mise run fast-test

# Single module fast test (fastest option)
./gradlew :community-core-engine:fastTest

# All tests (pre-commit)
mise run test
```

## Benchmark Results (2026-07-21)

| Scenario | Time | Status |
|----------|------|--------|
| Warm compile (community-core-engine) | ~0.8s | ✅ |
| Warm fast-test (single module) | ~0.76s | ✅ |
| Warm fast-test (3 modules + --configure-on-demand) | ~1.2s | ✅ |
| Cold config cache fast-test (3 modules) | ~1.0s | ✅ |
| Incremental no-op fast-test | ~0.7s | ✅ |

## Configuration

### Gradle Properties
```properties
org.gradle.test.parallel=true
org.gradle.test.maxParallelForks=4
org.gradle.configuration-cache=true
org.gradle.build-cache=true
org.gradle.jvmargs=-Xmx2048m -XX:+UseG1GC -XX:+UseStringDeduplication
test.jvmargs=-Xmx1024m -XX:+UseG1GC -XX:+UseStringDeduplication -XX:+TieredCompilation
```

### Build Tasks (premium-core-engine/build.gradle.kts)
```kotlin
tasks.register<Test>("fastTest") {
    useJUnitPlatform { excludeTags("slow") }
    maxParallelForks = 4
    systemProperty("jakarta.migration.mode", "dev")
}

tasks.register("compileCheck") {
    dependsOn("compileJava", "compileTestJava")
    doLast { println("✅ Compilation successful") }
}

tasks.register<Test>("coreTest") {
    useJUnitPlatform { excludeTags("slow") }
    maxParallelForks = 4
}
```

## Test Tagging

**@Tag("slow")** - Exclude from fast loop:
- Integration tests (StorageIntegrationTest, UsageServiceIntegrationTest, TransitiveDependencyScannerIntegrationTest, PdfReportMemoryIntegrationTest)
- Network-dependent tests
- Performance/memory tests
- Heavy file I/O

**Untagged or @Tag("fast")** - Include in fast loop:
- Unit tests with mocks
- Domain model tests
- Utility functions
- Configuration validation

## Performance Targets

- **Compilation**: <1s ✅ (currently ~0.8s with daemon)
- **Fast Tests**: <1.5s ✅ (currently ~1.0s with 3 modules)
- **Core Tests**: <30s
- **Full Tests**: 1-2min (pre-commit)

## Agentic AI Workflow

**Code Changes**: compile → fast → core
**PDF Changes**: compile → pdf
**Pre-commit**: core → all

## Optimizations

1. **Configuration Cache**: `--configuration-cache` (caches task graph)
2. **Build Cache**: `--build-cache` (reuses compiled classes)
3. **Parallel Execution**: `maxParallelForks=4` with `--parallel`
4. **JVM**: G1GC, string deduplication, tiered compilation
5. **Daemon**: `--no-daemon` for clean agent runs

## Troubleshooting

- **Cache issues**: Run with `--no-configuration-cache` once
- **Daemon issues**: Run with `--no-daemon` for clean builds, or `./gradlew --stop` to restart
- **Memory issues**: Increase `org.gradle.jvmargs`
- **Parallel issues**: Reduce `maxParallelForks` to 2

## Direct Gradle Commands

```bash
# Compile (daemon enabled)
./gradlew :community-core-engine:compileJava

# Fast tests (single module - fastest: ~0.76s)
./gradlew :community-core-engine:fastTest

# Fast tests (3 modules with configure-on-demand: ~1.0s)
./gradlew :community-core-engine:fastTest :premium-core-engine:fastTest :premium-experiment-engine:fastTest --configure-on-demand

# Profile build overhead
./gradlew :community-core-engine:fastTest --profile --no-configuration-cache
```

## Profile Breakdown (community-core-engine:fastTest)

| Phase | Time |
|-------|------|
| Startup (JVM daemon) | ~594ms |
| Configuration (all 5 projects) | ~281ms |
| Task execution | ~41ms |
| **Total** | **~916ms** |

The `--configure-on-demand` flag skips configuring unused projects, saving ~100ms on cold config cache.
