# Fast Test Loop

**Goal**: Provide agentic AI with <10s compilation and <10s test feedback for rapid iteration.

## Quick Commands

```powershell
# Fastest: compilation only (target: <10s, current: ~3s ✅)
.\scripts\fast-test.ps1 compile

# Fast unit tests (excludes @Tag("slow"))
.\scripts\fast-test.ps1 fast

# Core functionality tests
.\scripts\fast-test.ps1 core

# PDF tests only
.\scripts\fast-test.ps1 pdf

# All tests (pre-commit)
.\scripts\fast-test.ps1 all
```

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

- **Compilation**: <10s ✅ (currently ~3s with daemon)
- **Fast Tests**: <10s
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
# Compile (daemon enabled for <10s target)
./gradlew :premium-core-engine:compileJava --configuration-cache

# Fast tests
./gradlew :premium-core-engine:fastTest --configuration-cache --parallel

# Core tests
./gradlew :premium-core-engine:coreTest --configuration-cache --parallel
```
