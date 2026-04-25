# Test Organization Patterns

This document defines the established patterns for test organization, types, and implementation guidelines for the Jakarta Migration MCP project.

## Test Organization Structure

### Module-Based Organization

Tests are organized by module in the standard Maven/Gradle structure:

```
src/test/java/adrianmikula/jakartamigration/
├── community-core-engine/
│   └── unit/
│       └── jakartamigration/
│           ├── dependencyanalysis/
│           │   └── service/impl/
│           │       └── *Test.java
│           └── platforms/
│               └── service/
│                   └── *Test.java
└── premium-core-engine/
    ├── unit/
    │   └── jakartamigration/
    │       ├── advancedscanning/
    │       ├── analytics/
    │       ├── dependencyanalysis/
    │       ├── pdfreporting/
    │       │   ├── service/impl/
    │       │   └── snippet/
    │       └── platforms/
    └── integration/
        └── jakartamigration/
```

### Test Type Classification

#### Unit Tests
- **Location:** `src/test/java/.../unit/...`
- **Purpose:** Test individual components in isolation
- **Naming:** `*Test.java` (e.g., `MavenDependencyGraphBuilderTest.java`)
- **Scope:** Fast, focused, no external dependencies

#### Integration Tests
- **Location:** `src/test/java/.../integration/...`
- **Purpose:** Test component interactions and real-world scenarios
- **Naming:** `*IntegrationTest.java` (e.g., `MavenPropertyResolutionIntegrationTest.java`)
- **Scope:** Slower, may require external resources

#### Performance Tests
- **Location:** `src/test/java/.../performance/...`
- **Purpose:** Benchmark and validate performance characteristics
- **Naming:** `*PerformanceTest.java`

## Test Implementation Patterns

### JUnit 5 Standards

```java
@DisplayName("Descriptive test name")
void shouldDoSomethingWhenCondition() throws Exception {
    // Given - setup test data
    // When - execute action
    // Then - verify results
}
```

### Test Structure Template

```java
package adrianmikula.jakartamigration.{module}.{submodule};

import adrianmikula.jakartamigration.{module}.domain.*;
import adrianmikula.jakartamigration.{module}.service.*;
import adrianmikula.jakartamigration.{module}.service.impl.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.*;

@DisplayName("{Component} Tests")
class {Component}Test {
    
    private {Component} component;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        component = new {Component}();
    }
    
    @Test
    @DisplayName("Should {expected behavior}")
    void should{ExpectedBehavior}() throws Exception {
        // Given
        // Setup test data and conditions
        
        // When
        // Execute the method under test
        
        // Then
        // Verify results using assertions
    }
}
```

### Assertion Patterns

#### Prefer AssertJ over JUnit assertions
```java
// Use AssertJ for fluent, readable assertions
assertThat(result).isNotNull();
assertThat(result).contains("expected");
assertThat(result.size()).isEqualTo(expectedSize);
assertThat(result).hasSize(expectedSize);
assertThat(exception).hasMessage("Expected error message");
```

#### File Operations with @TempDir
```java
@TempDir
Path tempDir;

@Test
void shouldHandleFileOperations() throws IOException {
    // Given
    Path testFile = tempDir.resolve("test.xml");
    Files.writeString(testFile, content);
    
    // When
    Result result = component.processFile(testFile);
    
    // Then
    assertThat(result).isNotNull();
    assertThat(Files.exists(testFile)).isTrue();
}
```

## Test Categories

### By Functionality

#### Platform Detection Tests
- **Location:** `premium-core-engine/src/test/java/.../platforms/service/`
- **Focus:** Server detection, artifact matching, platform-specific indicators
- **Examples:** `SimplifiedPlatformDetectionServiceTest.java`

#### Dependency Analysis Tests
- **Location:** `community-core-engine/src/test/java/.../dependencyanalysis/`
- **Focus:** Maven parsing, property resolution, graph building
- **Examples:** `MavenDependencyGraphBuilderTest.java`

#### PDF Reporting Tests
- **Location:** `premium-core-engine/src/test/java/.../pdfreporting/`
- **Focus:** PDF generation, HTML validation, snippet processing
- **Examples:** `PdfReportServiceTest.java`, `HtmlSnippetValidationTest.java`

#### Analytics Tests
- **Location:** `premium-core-engine/src/test/java/.../analytics/`
- **Focus:** Usage tracking, error reporting, data validation
- **Examples:** `UsageServiceTest.java`, `UserIdentificationServiceTest.java`

#### Advanced Scanning Tests
- **Location:** `premium-core-engine/src/test/java/.../advancedscanning/`
- **Focus:** Transitive dependencies, comprehensive analysis
- **Examples:** `TransitiveDependencyScannerTest.java`

### By Test Type

#### Error Handling Tests
```java
@Test
@DisplayName("Should handle {error condition} gracefully")
void shouldHandleErrorGracefully() {
    // Given
    // Setup conditions that cause error
    
    // When & Then
    assertThrows({ExceptionType}.class, () -> {
        component.methodThatShouldFail();
    }, "Should throw expected exception");
}
```

#### Integration Tests with Real Projects
```java
@Test
@DisplayName("Should process real project from examples")
void shouldProcessRealProject() throws Exception {
    // Given - Path to real project (if available)
    Path projectPath = Path.of("../../../examples/{project-name}");
    
    // Only run if project exists (integration test)
    if (!Files.exists(projectPath)) {
        return; // Skip test if project not available
    }
    
    // When
    Result result = service.processProject(projectPath);
    
    // Then
    assertThat(result).isNotNull();
    // Verify specific expectations for real project
}
```

## Best Practices

### Test Data Management

#### Temporary Files
- Use `@TempDir` for file system operations
- Clean up is automatic
- Avoid hardcoded paths

#### Mock Objects
- Use Mockito for service dependencies when needed
- Mock external services (network, database)
- Keep mocks focused and minimal

#### Test Isolation
- Each test should be independent
- Use `@BeforeEach` and `@AfterEach` for setup/teardown
- Avoid shared state between tests

### Naming Conventions

#### Test Classes
- **Pattern:** `{Component}Test.java`
- **Examples:** `PdfReportServiceTest.java`, `MavenDependencyGraphBuilderTest.java`
- **Avoid:** `Test{Component}.java`, `{Component}Tests.java`

#### Test Methods
- **Pattern:** `should{ExpectedBehavior}When{Condition}()`
- **Examples:** `shouldDetectPayaraViaPayaraMicroDependency()`
- **Avoid:** `test{Method}()`, `check{Condition}()`

#### Display Names
- **Pattern:** `Should {expected behavior} when {condition}`
- **Examples:** `Should generate refactoring action report with minimal request`
- **Avoid:** `Test method`, `Check condition`

### Coverage Requirements

#### Minimum Coverage
- **Unit Tests:** 50% minimum line coverage
- **Critical Paths:** 100% coverage for core business logic
- **Integration Tests:** Cover main user workflows

#### Fast Test Subset
- **Location:** Identified in build configuration
- **Purpose:** Quick feedback during development
- **Criteria:** < 5 seconds execution time

## File Organization Rules

### Root Directory
- **No Java test files** should remain in root directory
- **Exception:** Build scripts, utilities, or documentation files
- **All tests** must be in appropriate `src/test/java/` structure

### Package Structure
- **Mirror source:** Test packages should mirror source packages
- **Consistent naming:** Follow established package structure
- **Proper imports:** Use test-specific imports only when needed

### Resource Management
- **Test resources:** Place in `src/test/resources/`
- **Configuration:** Use test-specific configuration files
- **Isolation:** Don't share resources with production

## Integration with Build System

### Maven Configuration
```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>3.0.0-M9</version>
            <configuration>
                <includes>
                    <include>**/*Test.java</include>
                    <include>**/*IntegrationTest.java</include>
                </includes>
                <excludes>
                    <exclude>**/*IT.java</exclude>
                </excludes>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### Gradle Configuration
```kotlin
test {
    useJUnitPlatform()
    
    testLogging {
        events "passed", "skipped", "failed"
    }
    
    // Configure test includes/excludes
    include '**/*Test.java'
    include '**/*IntegrationTest.java'
}
```

## Quality Gates

### Pre-commit Requirements
- All tests must pass
- No compilation warnings
- Code coverage thresholds met
- No dependency vulnerabilities

### CI/CD Integration
- Tests run on every push
- Parallel execution for speed
- Test results published
- Coverage reports generated

## Common Anti-patterns to Avoid

### Test Structure
- **Don't** use `main()` methods in test files
- **Don't** use `System.out.println()` for assertions
- **Don't** hardcode file paths
- **Don't** share state between tests

### Assertions
- **Don't** use JUnit `assertEquals()` without context
- **Don't** ignore exception handling in tests
- **Don't** use complex boolean logic in assertions

### Organization
- **Don't** place tests in source directories
- **Don't** mix unit and integration tests in same class
- **Don't** use inconsistent naming conventions

## Migration Guidelines

### Converting Legacy Tests
1. **Move** from root to appropriate test directory
2. **Rename** to follow `*Test.java` pattern
3. **Convert** `main()` to `@Test` methods
4. **Add** proper imports and annotations
5. **Replace** `System.out.println()` with assertions
6. **Add** `@DisplayName` for readability
7. **Use** `@TempDir` for file operations
8. **Remove** duplicate functionality

### Validation Checklist
- [ ] Test follows package structure
- [ ] Uses JUnit 5 annotations
- [ ] Has descriptive display names
- [ ] Uses AssertJ assertions
- [ ] Handles errors appropriately
- [ ] Isolated and independent
- [ ] Follows naming conventions
- [ ] Has adequate coverage
