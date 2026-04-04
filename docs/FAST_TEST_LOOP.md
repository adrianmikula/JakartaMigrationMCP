# Fast Test Loop Documentation

## Overview
The Fast Test Loop is designed to provide quick agent feedback during development by running only essential tests and optimizing build performance.

## Configuration

### Gradle Properties (gradle.properties)
```properties
# Fast Test Loop Configuration
org.gradle.test.parallel=true
org.gradle.test.maxParallelForks=4
org.gradle.configuration-cache=true
org.gradle.build-cache=true
org.gradle.jvmargs=-Xmx1024m -XX:+UseG1GC -XX:+UseStringDeduplication
```

### Build Tasks (premium-core-engine/build.gradle.kts)
- `fastTest`: Run tests tagged with @Tag("fast")
- `compileCheck`: Ultra-fast compilation check
- `pdfTest`: PDF generation tests only
- `coreTest`: Core functionality tests

## Usage

### 1. Compilation Check (Fastest)
```bash
./gradlew :premium-core-engine:compileJava --no-daemon --configuration-cache
```
**Time**: ~20-30 seconds
**Purpose**: Quick syntax and compilation verification

### 2. PDF Tests Only
```bash
./gradlew :premium-core-engine:test --tests "*PdfReportServiceImplTest*" --parallel --no-daemon --configuration-cache
```
**Time**: ~45-60 seconds
**Purpose**: Test PDFBox PDF generation functionality

### 3. Fast Tests (Tag-based)
```bash
./gradlew :premium-core-engine:test --tests "*fast*" --parallel --no-daemon --configuration-cache
```
**Time**: ~30-45 seconds
**Purpose**: Run only tests tagged with @Tag("fast")

### 4. Core Functionality Tests
```bash
./gradlew :premium-core-engine:test --tests "*PdfReportServiceTest*" --tests "*RecipeServiceImplTest*" --tests "*ListRecipesTest*" --parallel --no-daemon --configuration-cache
```
**Time**: ~60-90 seconds
**Purpose**: Test core features (PDF, recipes, validation)

### 5. PowerShell Script
```powershell
# Fast compilation
./scripts/fast-test.ps1 compile

# Fast tests only
./scripts/fast-test.ps1 fast

# Core functionality
./scripts/fast-test.ps1 core

# PDF tests only
./scripts/fast-test.ps1 pdf

# All tests (slower)
./scripts/fast-test.ps1 all
```

## Test Categories

### Fast Tests (@Tag("fast"))
- Unit tests that run quickly
- No external dependencies
- No network calls
- No heavy file I/O

### Core Tests
- PDF generation tests
- Recipe service tests
- Validation tests
- Essential business logic

### Slow Tests (Excluded from fast loop)
- Integration tests
- End-to-end tests
- Performance tests
- Recipe validation tests (slow YAML processing)

## Performance Optimizations

### 1. Parallel Execution
- `maxParallelForks = 4` in test configuration
- Parallel test execution with `--parallel` flag
- Multiple worker processes

### 2. Configuration Cache
- `--configuration-cache` flag
- Caches task graph and configuration
- Significant speed improvement on subsequent runs

### 3. Build Cache
- `--build-cache` flag
- Caches build outputs
- Reuses compiled classes and resources

### 4. JVM Optimization
- G1GC for better memory management
- String deduplication for reduced memory usage
- Optimized heap size (1GB)

### 5. Daemon Management
- `--no-daemon` for clean runs
- Prevents daemon issues in CI/agent environments
- Consistent performance across runs

## Recommended Agent Workflow

### For Code Changes:
1. **Quick Check**: `./scripts/fast-test.ps1 compile` (30s)
2. **Unit Tests**: `./scripts/fast-test.ps1 fast` (45s)
3. **Core Tests**: `./scripts/fast-test.ps1 core` (90s)

### For PDF Changes:
1. **Compilation**: `./scripts/fast-test.ps1 compile` (30s)
2. **PDF Tests**: `./scripts/fast-test.ps1 pdf` (60s)

### Before Commit:
1. **Core Tests**: `./scripts/fast-test.ps1 core` (90s)
2. **Full Tests**: `./scripts/fast-test.ps1 all` (2-3 minutes)

## Adding Fast Tests

To tag a test as fast:

```java
@Tag("fast")
class YourFastTest {
    @Test
    void yourQuickTest() {
        // Fast unit test logic
    }
}
```

## Monitoring Performance

### Key Metrics:
- **Compilation Time**: Should be < 30 seconds
- **Fast Test Time**: Should be < 60 seconds
- **Core Test Time**: Should be < 90 seconds
- **Full Test Time**: 2-3 minutes (acceptable)

### Performance Issues:
- If tests are slow, check for:
  - External dependencies
  - File I/O operations
  - Network calls
  - Heavy object creation
  - Inefficient test setup

## Troubleshooting

### Common Issues:
1. **Configuration Cache Issues**: Run with `--no-configuration-cache` once
2. **Daemon Issues**: Run with `--no-daemon`
3. **Memory Issues**: Increase `org.gradle.jvmargs`
4. **Parallel Test Issues**: Reduce `maxParallelForks`

### Clean Build:
```bash
./gradlew clean --no-daemon
./gradlew :premium-core-engine:compileJava --no-daemon --configuration-cache
```

## Integration with IDE

### IntelliJ IDEA:
1. Create run configurations for fast test tasks
2. Use Gradle panel for quick task execution
3. Enable configuration cache in IDE settings

### VS Code:
1. Add tasks to `.vscode/tasks.json`
2. Use integrated terminal for script execution
3. Configure Gradle extension for better integration

## Future Improvements

### Potential Enhancements:
1. **Test Sharding**: Split tests across multiple machines
2. **Incremental Testing**: Only run tests affected by changes
3. **Smart Caching**: More granular build cache usage
4. **Parallel Compilation**: Compile modules in parallel
5. **Dockerized Testing**: Isolated test environments

### Monitoring:
1. **Test Timing Metrics**: Track test execution times
2. **Performance Regression Detection**: Alert on slow tests
3. **Cache Hit Rates**: Monitor cache effectiveness
4. **Resource Usage**: Track CPU and memory usage
