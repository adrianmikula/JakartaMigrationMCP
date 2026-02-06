# Testing Standards

## Overview

This project follows **Test-Driven Development (TDD)** principles. All code must be written with tests first, ensuring high quality and maintainability.

## Test Types

### 1. Unit Tests

**Purpose**: Test individual components in isolation

**Location**: `src/test/java/unit/`

**Characteristics**:
- Fast execution (no external dependencies)
- Mock external dependencies
- Test single responsibility
- No database or network calls

**Example**:
```java
@ExtendWith(MockitoExtension.class)
@DisplayName("DataPollingService Tests")
class DataPollingServiceTest {
    
    @Mock
    private ExternalApiClient apiClient;
    
    @Mock
    private DataRepository repository;
    
    @InjectMocks
    private DataPollingService service;
    
    @Test
    @DisplayName("Should filter out duplicate items")
    void shouldFilterDuplicates() {
        // Given
        Data data = createTestData("data-123");
        when(repository.existsByIdAndType("data-123", "external"))
                .thenReturn(true);
        when(apiClient.fetchData()).thenReturn(Flux.just(data));
        
        // When
        Flux<Data> result = service.pollExternalApi();
        
        // Then
        StepVerifier.create(result)
                .verifyComplete();
    }
}
```

### 2. Component Tests

**Purpose**: Test major features with real infrastructure

**Location**: `src/test/java/component/`

**Characteristics**:
- Use TestContainers for real databases/services
- Test integration between components
- Verify end-to-end workflows
- Slower execution (requires Docker)

**Example**:
```java
@SpringBootTest
@Testcontainers
@ActiveProfiles("component-test")
class DataPollingComponentTest extends AbstractComponentTest {
    
    @Autowired
    private DataPollingService pollingService;
    
    @Autowired
    private DataRepository repository;
    
    @Test
    @DisplayName("Should poll, save, and enqueue data")
    void shouldPollSaveAndEnqueue() {
        // Given
        // Mock external API responses
        
        // When
        Flux<Data> result = pollingService.pollAllSources();
        
        // Then
        StepVerifier.create(result)
                .expectNextCount(2)
                .verifyComplete();
        
        // Verify database
        assertEquals(2, repository.count());
    }
}
```

## Test Structure

### Given-When-Then Pattern

All tests should follow the **Given-When-Then** structure:

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

### Test Naming

- **Format**: `should*` or `when*`
- **Descriptive**: Clearly state what is being tested
- **Use `@DisplayName`**: For human-readable test descriptions

**Examples**:
```java
@Test
@DisplayName("Should filter items below minimum value")
void shouldFilterItemsBelowMinimumValue() { }

@Test
@DisplayName("Should enqueue item with correct priority")
void shouldEnqueueItemWithCorrectPriority() { }
```

## Testing Reactive Code

### Project Reactor Testing

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

### Testing Error Scenarios

```java
@Test
@DisplayName("Should handle API errors gracefully")
void shouldHandleApiErrors() {
    // Given
    when(apiClient.fetchData())
            .thenReturn(Flux.error(new RuntimeException("API Error")));
    
    // When
    Flux<Data> result = service.pollExternalApi();
    
    // Then
    StepVerifier.create(result)
            .expectError(RuntimeException.class)
            .verify();
}
```

## Mocking Guidelines

### When to Mock

- ✅ External APIs
- ✅ Database repositories (in unit tests)
- ✅ File system operations
- ✅ Network calls
- ✅ Complex dependencies

### When NOT to Mock

- ❌ Domain models (use real instances)
- ❌ Value objects
- ❌ Simple utility classes
- ❌ The class under test

### Mockito Best Practices

```java
// ✅ Good: Use @Mock and @InjectMocks
@ExtendWith(MockitoExtension.class)
class ServiceTest {
    @Mock
    private Dependency dependency;
    
    @InjectMocks
    private ServiceUnderTest service;
}

// ✅ Good: Verify interactions
verify(dependency, times(1)).methodCall();

// ✅ Good: Stub return values
when(dependency.getValue()).thenReturn("expected");

// ❌ Bad: Over-mocking
when(domainObject.getSimpleProperty()).thenReturn("value");
// Use real domain objects instead
```

## Test Coverage Requirements

### Minimum Coverage

- **Overall**: 60% line coverage (target for MVP phase)
- **Domain Models**: 100% coverage
- **Services**: 70% coverage
- **Controllers**: 60% coverage
- **Mappers**: 100% coverage

**Note**: The 60% target is set for the MVP phase. As the project matures, we aim to increase coverage to 80%+ for production readiness.

### Exclusions

The following are excluded from coverage:
- Configuration classes (`*Config`)
- JPA entities (`*Entity`)
- DTOs (`*DTO`, `*Event`)
- Application main class
- Lombok-generated code

### Coverage Reports

Coverage is automatically generated after tests:
```bash
# View HTML report
./gradlew jacocoTestReport
open build/reports/jacoco/test/html/index.html

# View summary
./gradlew jacocoCoverageSummary
```

## Test Data Management

### Test Fixtures

Create reusable test data builders:

```java
class TestDataBuilder {
    static Data createData(String id) {
        return Data.builder()
                .id(id)
                .type("external")
                .value(new BigDecimal("100.00"))
                .status(Status.ACTIVE)
                .build();
    }
    
    static Data createHighValueData() {
        return createData("data-123")
                .toBuilder()
                .value(new BigDecimal("500.00"))
                .build();
    }
}
```

### Test Isolation

- Each test should be independent
- Use `@BeforeEach` for setup
- Clean up after tests
- Don't rely on test execution order

## Component Test Best Practices

### TestContainers Usage

```java
@SpringBootTest
@Testcontainers
@ActiveProfiles("component-test")
class ComponentTest extends AbstractComponentTest {
    
    // Containers are managed by AbstractComponentTest
    // PostgreSQL and Redis are automatically started
    
    @Test
    void shouldTestWithRealDatabase() {
        // Test with real PostgreSQL
    }
}
```

### Test Configuration

- Use `application-component-test.yml` for test-specific config
- Mock external services (APIs, LLM)
- Use in-memory or containerized services

### Performance Considerations

- Component tests are slower - use sparingly
- Test critical paths end-to-end
- Use container reuse when possible
- Run component tests separately from unit tests

## Assertions

### Prefer AssertJ

```java
// ✅ Good: AssertJ fluent assertions
assertThat(data)
        .isNotNull()
        .extracting(Data::getId)
        .isEqualTo("data-123");

assertThat(dataList)
        .hasSize(2)
        .extracting(Data::getValue)
        .containsExactly(
                new BigDecimal("100.00"),
                new BigDecimal("200.00")
        );

// ❌ Avoid: JUnit assertions (less readable)
assertEquals("data-123", data.getId());
```

### Custom Assertions

For complex domain objects, create custom assertions:

```java
class DataAssert {
    static DataAssert assertThat(Data actual) {
        return new DataAssert(actual);
    }
    
    DataAssert hasId(String expected) {
        assertThat(actual.getId()).isEqualTo(expected);
        return this;
    }
    
    DataAssert isEligible() {
        assertThat(actual.isEligibleForProcessing()).isTrue();
        return this;
    }
}
```

## TDD Workflow

### Red-Green-Refactor

1. **Red**: Write a failing test
   ```java
   @Test
   void shouldCalculatePriority() {
       Data data = createData();
       double priority = service.calculatePriority(data);
       assertThat(priority).isGreaterThan(0);
   }
   ```

2. **Green**: Write minimal code to pass
   ```java
   public double calculatePriority(Bounty bounty) {
       return 1000.0; // Minimal implementation
   }
   ```

3. **Refactor**: Improve while keeping tests green
   ```java
   public double calculatePriority(Data data) {
       return BASE_PRIORITY + data.getValue().doubleValue();
   }
   ```

### Test First Benefits

- ✅ Clear requirements before implementation
- ✅ Better design (testable = better design)
- ✅ Documentation through tests
- ✅ Confidence in refactoring
- ✅ Prevents over-engineering

## Common Testing Patterns

### Testing Async Operations

```java
@Test
void shouldProcessAsync() {
    CompletableFuture<Data> future = service.processAsync(data);
    
    Data result = future.get(5, TimeUnit.SECONDS);
    
    assertThat(result).isNotNull();
}
```

### Testing Scheduled Tasks

```java
@Test
void shouldPollOnSchedule() {
    // Use @TestPropertySource to override schedule
    // Or use Awaitility for async verification
    await().atMost(10, SECONDS)
            .until(() -> repository.count() > 0);
}
```

### Testing Error Handling

```java
@Test
void shouldHandleErrorsGracefully() {
    when(apiClient.fetchData())
            .thenReturn(Flux.error(new ApiException()));
    
    Flux<Data> result = service.pollExternalApi();
    
    StepVerifier.create(result)
            .expectError(ApiException.class)
            .verify();
    
    // Verify fallback behavior
    assertThat(queue.size()).isEqualTo(0);
}
```

## Test Maintenance

### Keep Tests Simple

- One assertion per test (when possible)
- Test one behavior at a time
- Use descriptive test names
- Remove obsolete tests

### Test Documentation

- Use `@DisplayName` for clarity
- Add comments for complex test scenarios
- Document test data requirements
- Explain test setup when non-obvious

