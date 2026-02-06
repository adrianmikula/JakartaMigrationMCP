# Coding Standards

This document outlines the coding standards and best practices for this project. These standards are based on 2026 industry best practices and ensure maximum code quality, maintainability, and team productivity.

## Core Principles

### 1. Test-Driven Development (TDD)

**Red-Green-Refactor Cycle:**
1. **Red**: Write a failing test first
2. **Green**: Write minimal code to make it pass
3. **Refactor**: Improve code while keeping tests green

**Rules:**
- ✅ Write tests before implementation
- ✅ Tests must fail for the right reason
- ✅ Write minimal code to pass tests
- ✅ Refactor only when tests are green
- ✅ Maintain test coverage (target: 60% for MVP, 80%+ for production)

**Test Structure:**
```java
@Test
@DisplayName("Should create a valid entity with all required fields")
void shouldCreateValidEntity() {
    // Given
    String id = "entity-123";
    BigDecimal value = new BigDecimal("150.00");
    
    // When
    Entity entity = Entity.builder()
            .id(id)
            .value(value)
            .build();
    
    // Then
    assertNotNull(entity.getId());
    assertEquals(id, entity.getId());
}
```

### 2. DRY (Don't Repeat Yourself)

**Principles:**
- Extract common logic into reusable methods/classes
- Use inheritance and composition appropriately
- Create utility classes for shared functionality
- Avoid copy-paste code

**Examples:**
- ✅ Shared test base classes (`AbstractComponentTest`)
- ✅ Mapper classes for domain-entity conversion
- ✅ Common configuration classes
- ❌ Duplicate validation logic across services

### 3. KISS (Keep It Simple, Stupid)

**Guidelines:**
- Prefer simple solutions over complex ones
- Avoid premature optimization
- Use language features appropriately (Java 21)
- Write code that is easy to understand

**Examples:**
- ✅ Use `record` for immutable data structures
- ✅ Use `var` for local variables when type is obvious
- ✅ Prefer explicit over implicit
- ❌ Over-engineered abstractions

### 4. SOLID Principles

#### Single Responsibility Principle (SRP)
- Each class should have one reason to change
- Services should have focused responsibilities
- Domain models should represent business concepts only

**Example:**
```java
// ✅ Good: Single responsibility
@Service
public class DataPollingService {
    // Only handles polling logic
}

// ❌ Bad: Multiple responsibilities
@Service
public class DataService {
    // Polling, filtering, queueing, persistence...
}
```

#### Open/Closed Principle (OCP)
- Open for extension, closed for modification
- Use interfaces and abstractions
- Prefer composition over inheritance

**Example:**
```java
// ✅ Good: Interface allows different implementations
public interface ApiClient {
    Flux<Data> fetchData();
}

// ✅ Good: Easy to add new implementations
@Component
public class ExternalApiClientImpl implements ApiClient { }
@Component
public class InternalApiClientImpl implements ApiClient { }
```

#### Liskov Substitution Principle (LSP)
- Subtypes must be substitutable for their base types
- Maintain behavioral contracts
- Don't violate interface expectations

#### Interface Segregation Principle (ISP)
- Clients shouldn't depend on interfaces they don't use
- Create focused, specific interfaces
- Avoid "fat" interfaces

**Example:**
```java
// ✅ Good: Focused interface
public interface GitOperations {
    void cloneRepository(String url, String path);
    void pull(Repository repository);
}

// ❌ Bad: Too many responsibilities
public interface GitOperations {
    void cloneRepository(...);
    void pull(...);
    void createBranch(...);
    void merge(...);
    void rebase(...);
    // ... 20 more methods
}
```

#### Dependency Inversion Principle (DIP)
- Depend on abstractions, not concretions
- High-level modules shouldn't depend on low-level modules
- Both should depend on abstractions

**Example:**
```java
// ✅ Good: Depends on interface
@Service
public class RepositoryService {
    private final GitOperations gitOperations; // Interface
    
    public RepositoryService(GitOperations gitOperations) {
        this.gitOperations = gitOperations;
    }
}

// ❌ Bad: Depends on concrete class
@Service
public class RepositoryService {
    private final JGitOperations gitOperations; // Concrete class
}
```

## Code Organization

### Package Structure
```
com.yourproject/
├── domain1/
│   ├── domain/          # Domain models
│   ├── entity/          # JPA entities
│   ├── repository/      # Data access
│   ├── service/         # Business logic
│   ├── mapper/          # Domain-entity mapping
│   └── config/          # Configuration
├── domain2/
│   └── [same structure]
└── shared/
    └── [shared components]
```

### Domain-Driven Design (DDD)
- **Domain Models**: Pure business logic, no JPA annotations
- **Entities**: JPA persistence layer, separate from domain
- **Mappers**: Convert between domain and entity layers
- **Services**: Orchestrate domain operations

## Naming Conventions

### Classes
- **Services**: `*Service` (e.g., `DataPollingService`)
- **Repositories**: `*Repository` (e.g., `DataRepository`)
- **Entities**: `*Entity` (e.g., `DataEntity`)
- **DTOs**: `*DTO` or descriptive names (e.g., `ApiResponse`)
- **Mappers**: `*Mapper` (e.g., `DataMapper`)
- **Config**: `*Config` (e.g., `RedisConfig`)

### Methods
- **Actions**: Verb-based (e.g., `fetchData()`, `processEvent()`)
- **Queries**: Boolean/descriptive (e.g., `isEligible()`, `meetsCriteria()`)
- **Tests**: `should*` or `when*` (e.g., `shouldCreateValidEntity()`)

### Variables
- **Descriptive**: `dataAmount` not `amt`
- **Boolean**: `isEligible`, `hasError`, `canProcess`
- **Collections**: Plural (e.g., `items`, `entities`)

## Java 21 Best Practices

### Modern Language Features
- ✅ **Records**: For immutable data structures
- ✅ **Pattern Matching**: For type checking and extraction
- ✅ **Sealed Classes**: For restricted inheritance
- ✅ **Text Blocks**: For multi-line strings
- ✅ **Virtual Threads**: For high-concurrency I/O

### Code Style
```java
// ✅ Good: Use var when type is obvious
var entity = Entity.builder()
        .id("entity-123")
        .build();

// ✅ Good: Pattern matching
if (result instanceof FilterResult fr && fr.shouldProcess()) {
    processEntity(fr);
}

// ✅ Good: Text blocks for prompts
String prompt = """
        Analyze this data and determine if it should be processed.
        
        Details:
        - ID: {id}
        - Value: {value}
        """;
```

## Testing Standards

### Test Organization
- **Unit Tests**: `src/test/java/unit/`
- **Component Tests**: `src/test/java/component/`
- **Test Naming**: `*Test.java` for unit, `*ComponentTest.java` for component

### Test Structure
```java
@ExtendWith(MockitoExtension.class)
@DisplayName("BountyPollingService Tests")
class BountyPollingServiceTest {
    
    @Mock
    private AlgoraApiClient apiClient;
    
    @InjectMocks
    private BountyPollingService service;
    
    @Test
    @DisplayName("Should poll and save new bounties")
    void shouldPollAndSaveNewBounties() {
        // Given
        Bounty bounty = createTestBounty();
        when(apiClient.fetchBounties()).thenReturn(Flux.just(bounty));
        
        // When
        Flux<Bounty> result = service.pollAlgora();
        
        // Then
        StepVerifier.create(result)
                .expectNext(bounty)
                .verifyComplete();
    }
}
```

### Test Coverage
- **Minimum**: 60% line coverage (MVP target)
- **Target**: 80%+ line coverage (production goal)
- **Critical Paths**: 100% coverage
- **Domain Models**: 100% coverage
- **Exclusions**: Config classes, entities, DTOs, main class

## Error Handling

### Principles
- ✅ Fail fast with clear error messages
- ✅ Use appropriate exception types
- ✅ Log errors with context
- ✅ Handle errors at appropriate levels

### Patterns
```java
// ✅ Good: Specific exception
if (repository == null) {
    throw new IllegalArgumentException("Repository cannot be null");
}

// ✅ Good: Graceful degradation
try {
    return apiClient.fetchBounties();
} catch (Exception e) {
    log.error("Failed to fetch bounties", e);
    return Flux.empty(); // Fallback
}
```

## Documentation

### Code Comments
- **When**: Explain "why", not "what"
- **JavaDoc**: For public APIs
- **Inline**: For complex business logic

### JavaDoc Example
```java
/**
 * Polls external API for new data and processes them.
 * 
 * @param minimumValue Minimum value to consider
 * @return Flux of discovered data items
 * @throws ApiException if API call fails
 */
public Flux<Data> pollExternalApi(BigDecimal minimumValue) {
    // Implementation
}
```

## Reactive Programming

### Project Reactor Patterns
- ✅ Use `Flux` for multiple items
- ✅ Use `Mono` for single items
- ✅ Chain operations with `flatMap`, `filter`, `map`
- ✅ Handle errors with `onErrorResume`, `doOnError`

### Example
```java
return apiClient.fetchData()
        .filter(item -> !existsInDatabase(item))
        .filter(item -> item.meetsMinimumValue(minimum))
        .flatMap(this::saveAndEnqueue)
        .doOnError(error -> log.error("Polling failed", error));
```

## Security

### Best Practices
- ✅ Validate all external inputs
- ✅ Use parameterized queries (JPA handles this)
- ✅ Verify webhook signatures
- ✅ Sanitize user-provided data
- ✅ Use secure defaults

## Performance

### Guidelines
- ✅ Use virtual threads for I/O-bound operations
- ✅ Leverage reactive streams for backpressure
- ✅ Cache expensive operations (Redis)
- ✅ Use connection pooling
- ✅ Profile before optimizing

## Code Review Checklist

- [ ] Tests written and passing
- [ ] Code follows SOLID principles
- [ ] No code duplication (DRY)
- [ ] Simple and readable (KISS)
- [ ] Proper error handling
- [ ] Documentation updated
- [ ] No security vulnerabilities
- [ ] Performance considerations addressed

## Related Documentation

- [Testing Standards](testing.md) - Test structure, patterns, and best practices
- [Code Quality Standards](code-quality.md) - Code review checklist and quality guidelines
- [Common Gotchas and Problems](common-gotchas.md) - Avoid common pitfalls and mistakes

