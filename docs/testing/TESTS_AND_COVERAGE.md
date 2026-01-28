# Tests & Coverage

## Overview

This project follows **Test-Driven Development (TDD)** principles and uses **JaCoCo** for code coverage analysis. All tests are automatically executed during the build process, and coverage reports are generated after each test run.

## Test Structure

### Test Types

The project uses a three-tier testing strategy:

1. **Unit Tests** (`src/test/java/unit/`)
   - Fast execution (no external dependencies)
   - Mock external dependencies
   - Test single components in isolation
   - No database or network calls

2. **Component Tests** (`src/test/java/component/`)
   - Integration tests with real infrastructure
   - Use TestContainers for databases/services
   - Test integration between components
   - Require Docker to be running

3. **End-to-End Tests** (`src/test/java/e2e/`)
   - Full workflow validation
   - Complete user scenarios
   - Require Docker and database

### Test Organization

```
src/test/java/
├── unit/
│   ├── jakartamigration/
│   │   ├── coderefactoring/        # Migration refactoring tests
│   │   ├── dependencyanalysis/     # Dependency analysis tests
│   │   └── runtimeverification/    # Runtime verification tests
│   └── adrianmikula/projectname/
│       └── unit/
├── component/
│   └── adrianmikula/projectname/
│       └── component/
└── e2e/
    └── adrianmikula/projectname/
        └── e2e/
```

## Running Tests

### Prerequisites

1. **Gradle Wrapper**: Must be initialized
   ```powershell
   # If Gradle is installed:
   gradle wrapper --gradle-version 8.5
   
   # Or run setup script:
   .\scripts\setup.ps1
   ```

2. **Docker** (for component/E2E tests):
   ```powershell
   # Start Docker Desktop, then:
   .\scripts\start-services.ps1
   ```

### Using Mise (Recommended)

```bash
# Run all tests
mise run test

# Run only unit tests
mise run test-unit

# Run only component tests (requires Docker)
mise run test-component

# Run only E2E tests (requires Docker)
mise run test-e2e
```

### Using Gradle Directly

```powershell
# Run all tests
.\gradlew.bat test

# Run only unit tests
.\gradlew.bat test --tests "unit.*" --tests "integration.*" --tests "adrianmikula.projectname.unit.*"

# Run only component tests
.\gradlew.bat test --tests "adrianmikula.jakartamigration.component.*"

# Run only E2E tests
.\gradlew.bat test --tests "adrianmikula.jakartamigration.e2e.*"

# Run specific test class
.\gradlew.bat test --tests "unit.jakartamigration.dependencyanalysis.ArtifactTest"
```

### Using Scripts

```powershell
# Run all tests
.\scripts\gradle-test.ps1

# Run only unit tests
.\scripts\gradle-test.ps1 -UnitOnly

# Run only component tests
.\scripts\gradle-test.ps1 -ComponentOnly

# Run only E2E tests
.\scripts\gradle-test.ps1 -E2EOnly
```

## Code Coverage

### Coverage Configuration

JaCoCo is configured in `build.gradle.kts` with the following settings:

- **Tool Version**: 0.8.11
- **Report Formats**: HTML (interactive) and XML (for CI/CD)
- **Exclusions**: Config classes, entities, DTOs, and application main class
- **Historical Tracking**: Timestamped reports saved for every test run

### Coverage Requirements

| Component Type | Minimum Coverage | Target Coverage |
|---------------|-----------------|-----------------|
| **Overall** | 60% | 80%+ |
| **Domain Models** | 100% | 100% |
| **Services** | 70% | 80%+ |
| **Controllers** | 60% | 70%+ |
| **Mappers** | 100% | 100% |

**Note**: The 60% target is set for the MVP phase. As the project matures, we aim to increase coverage to 80%+ for production readiness.

### Excluded from Coverage

The following are automatically excluded from coverage calculations:

- Configuration classes (`*Config`)
- JPA entities (`*Entity`)
- DTOs (`*DTO`, `*Event`)
- Application main class (`*Application`)
- Lombok-generated code

### Generating Coverage Reports

#### Using Mise

```bash
# Generate coverage report and open in browser
mise run coverage

# Open existing coverage report
mise run coverage-open

# Clean coverage reports
mise run coverage-clean
```

#### Using Gradle

```powershell
# Generate coverage report (automatically runs after tests)
.\gradlew.bat jacocoTestReport

# Generate coverage report with summary
.\gradlew.bat jacocoTestReport jacocoCoverageSummary

# View summary only (requires report to exist)
.\gradlew.bat jacocoCoverageSummary
```

#### Using Scripts

```powershell
# Generate coverage report and open in browser
.\scripts\gradle-coverage.ps1
```

### Coverage Report Locations

- **HTML Report**: `build/reports/jacoco/test/html/index.html`
- **XML Report**: `build/reports/jacoco/test/jacocoTestReport.xml`
- **Historical Reports**: `build/reports/jacoco-html-YYYY-MM-DD_HH-mm-ss/`

### Viewing Coverage Reports

1. **HTML Report** (Interactive):
   - Open `build/reports/jacoco/test/html/index.html` in a browser
   - Navigate through packages and classes
   - See line-by-line coverage highlighting
   - View coverage metrics per package/class

2. **XML Report** (CI/CD Integration):
   - Located at `build/reports/jacoco/test/jacocoTestReport.xml`
   - Can be parsed by CI/CD tools
   - Used for coverage thresholds and badges

3. **Coverage Summary** (Console):
   - Run `.\gradlew.bat jacocoCoverageSummary`
   - Displays coverage percentage in console
   - Shows covered/missed instruction counts

## Test Status

### Current Test Suite

#### ✅ Unit Tests (Ready to Run)

**Jakarta Migration Module:**
- `ArtifactTest` - Domain model validation
- `DependencyGraphTest` - Graph operations
- `RecipeTest` - Refactoring recipes
- `RefactoringPhaseTest` - Migration phases
- `RefactoringResultTest` - Migration results
- `MigrationProgressTest` - Progress tracking
- `RecipeLibraryTest` - Recipe management
- `DependencyGraphBuilderTest` - Dependency graph building
- `MavenDependencyGraphBuilderTest` - Maven-specific parsing
- `NamespaceClassifierTest` - Namespace classification
- `ErrorAnalysisTest` - Error analysis
- `RuntimeErrorTest` - Runtime error handling
- `VerificationOptionsTest` - Verification configuration
- `VerificationResultTest` - Verification results
- `RuntimeVerificationModuleTest` - Runtime verification

**Example Tests:**
- `ExampleServiceTest` - Service layer examples

#### ✅ Component Tests (Requires Docker)

- `ExampleServiceComponentTest` - Component integration examples
- `AbstractComponentTest` - Base class for component tests

#### ✅ E2E Tests (Requires Docker)

- `ExampleE2ETest` - End-to-end workflow examples

### Test Execution Status

Some tests are currently excluded from compilation due to compilation errors:

- `MavenDependencyGraphBuilderTest.java`
- `NamespaceClassifierTest.java`
- `DependencyAnalysisModuleTest.java`
- `MigrationPlannerTest.java`
- `ChangeTrackerTest.java`
- `ProgressTrackerTest.java`
- `MigrationPlanTest.java`

These will be re-enabled once the corresponding implementations are complete.

## Test Standards

### Test Naming

- **Format**: `should*` or `when*`
- **Descriptive**: Clearly state what is being tested
- **Use `@DisplayName`**: For human-readable test descriptions

**Examples:**
```java
@Test
@DisplayName("Should filter items below minimum value")
void shouldFilterItemsBelowMinimumValue() { }

@Test
@DisplayName("Should enqueue item with correct priority")
void shouldEnqueueItemWithCorrectPriority() { }
```

### Test Structure (Given-When-Then)

All tests should follow the **Given-When-Then** pattern:

```java
@Test
@DisplayName("Should create a valid entity")
void shouldCreateValidEntity() {
    // Given - Setup test data and mocks
    String id = "entity-123";
    BigDecimal value = new BigDecimal("150.00");
    
    // When - Execute the code under test
    Entity entity = Entity.builder()
            .id(id)
            .value(value)
            .build();
    
    // Then - Verify the results
    assertNotNull(entity.getId());
    assertEquals(id, entity.getId());
    assertEquals(value, entity.getValue());
}
```

### Testing Reactive Code

Use `StepVerifier` for testing `Flux` and `Mono`:

```java
@Test
@DisplayName("Should process data reactively")
void shouldProcessDataReactively() {
    // Given
    Data data1 = createData("data-1");
    Data data2 = createData("data-2");
    when(apiClient.fetchData())
            .thenReturn(Flux.just(data1, data2));
    
    // When
    Flux<Data> result = service.pollExternalApi();
    
    // Then
    StepVerifier.create(result)
            .expectNext(data1)
            .expectNext(data2)
            .verifyComplete();
}
```

### Assertions

Prefer **AssertJ** for fluent assertions:

```java
// ✅ Good: AssertJ fluent assertions
assertThat(data)
        .isNotNull()
        .extracting(Data::getId)
        .isEqualTo("data-123");

// ❌ Avoid: JUnit assertions (less readable)
assertEquals("data-123", data.getId());
```

## Continuous Integration

### CI/CD Integration

Coverage reports can be integrated into CI/CD pipelines:

1. **Generate XML Report**:
   ```bash
   ./gradlew test jacocoTestReport
   ```

2. **Upload to Coverage Service**:
   - Upload `build/reports/jacoco/test/jacocoTestReport.xml` to services like:
     - Codecov
     - Coveralls
     - SonarQube
     - GitHub Actions (with coverage action)

3. **Enforce Coverage Thresholds**:
   ```kotlin
   // In build.gradle.kts (future enhancement)
   tasks.jacocoTestReport {
       executionData.setFrom(fileTree(layout.buildDirectory.dir("jacoco")).include("**/*.exec"))
       violationRules {
           rule {
               limit {
                   minimum = "0.60".toBigDecimal() // 60% minimum
               }
           }
       }
   }
   ```

## Troubleshooting

### Tests Not Running

1. **Check Gradle Wrapper**:
   ```powershell
   # Initialize if missing
   gradle wrapper --gradle-version 8.5
   ```

2. **Check Java Version**:
   ```powershell
   java -version  # Should be Java 21
   ```

3. **Clean and Rebuild**:
   ```powershell
   .\gradlew.bat clean test
   ```

### Coverage Report Not Generated

1. **Run Tests First**:
   ```powershell
   .\gradlew.bat test
   ```

2. **Generate Report Manually**:
   ```powershell
   .\gradlew.bat jacocoTestReport
   ```

3. **Check Report Location**:
   ```powershell
   # Verify report exists
   Test-Path build\reports\jacoco\test\html\index.html
   ```

### Component Tests Failing

1. **Check Docker Status**:
   ```powershell
   docker ps
   ```

2. **Start Services**:
   ```powershell
   .\scripts\start-services.ps1
   ```

3. **Check TestContainers**:
   - Ensure Docker Desktop is running
   - Check Docker daemon is accessible
   - Verify network connectivity

## Best Practices

1. **Write Tests First** (TDD):
   - Red: Write failing test
   - Green: Write minimal code to pass
   - Refactor: Improve while keeping tests green

2. **Keep Tests Simple**:
   - One assertion per test (when possible)
   - Test one behavior at a time
   - Use descriptive test names

3. **Mock External Dependencies**:
   - External APIs
   - Database repositories (in unit tests)
   - File system operations
   - Network calls

4. **Use Real Objects When Appropriate**:
   - Domain models
   - Value objects
   - Simple utility classes

5. **Test Edge Cases**:
   - Null handling
   - Empty collections
   - Error scenarios
   - Boundary conditions

6. **Maintain Test Independence**:
   - Each test should be independent
   - Don't rely on test execution order
   - Clean up after tests

## Additional Resources

- [Testing Standards](standards/testing.md) - Detailed testing guidelines
- [Test Status](TEST_STATUS.md) - Current test suite status
- [Component Tests](COMPONENT_TESTS.md) - Component testing guide
- [E2E Tests](E2E_TESTS.md) - End-to-end testing guide
- [Jakarta Migration Tests](JAKARTA_MIGRATION_TESTS_STATUS.md) - Jakarta module test status

## Quick Reference

| Task | Command |
|------|---------|
| Run all tests | `mise run test` or `.\gradlew.bat test` |
| Run unit tests | `mise run test-unit` |
| Run component tests | `mise run test-component` |
| Run E2E tests | `mise run test-e2e` |
| Generate coverage | `mise run coverage` |
| Open coverage report | `mise run coverage-open` |
| Clean coverage | `mise run coverage-clean` |
| View coverage summary | `.\gradlew.bat jacocoCoverageSummary` |

---

*Last Updated: 2026-01-27*

