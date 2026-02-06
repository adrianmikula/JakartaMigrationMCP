# End-to-End (E2E) Tests

## Overview

End-to-end tests verify complete workflows across multiple layers of the application, from service layer through to database persistence. These tests use the same infrastructure as component tests (TestContainers with PostgreSQL) but focus on testing complete user workflows rather than isolated components.

## Test Structure

E2E tests extend `AbstractComponentTest`, which provides:
- Spring Boot test context with full application configuration
- PostgreSQL container via TestContainers
- Transaction management for test isolation
- Component test profile configuration

## Current E2E Tests

### WebhookCommitAnalysisE2ETest

**Location:** `src/test/java/e2e/WebhookCommitAnalysisE2ETest.java`

**Purpose:** Tests the complete repository management workflow from creation through retrieval.

**Test Coverage:**

1. **Repository Creation and Retrieval E2E**
   - **Test Method:** `shouldCreateAndRetrieveRepositoryE2E()`
   - **Description:** Verifies the complete flow of creating a repository via the service layer and retrieving it from the database
   - **Steps:**
     1. Creates a `Repository` domain object with URL, owner, name, and language
     2. Saves it via `RepositoryManagementService.addRepository()`
     3. Verifies the entity was persisted with an ID
     4. Retrieves the entity from the repository
     5. Validates the retrieved data matches the original input

**Test Data:**
- Repository URL: `https://github.com/owner/repo`
- Owner: `owner`
- Name: `repo`
- Language: `Java`

**Assertions:**
- ✅ Repository entity has a non-null ID after creation
- ✅ Repository can be retrieved by ID
- ✅ Retrieved repository URL matches the original input

## Running E2E Tests

### Prerequisites

1. **Docker** - Required for TestContainers
   ```powershell
   # Start Docker Desktop, then:
   .\scripts\start-services.ps1
   ```

2. **Gradle Wrapper** - Must be initialized
   ```powershell
   .\scripts\setup.ps1
   ```

### Run All E2E Tests

```powershell
# Using Gradle wrapper:
.\gradlew.bat test --tests "adrianmikula.jakartamigration.e2e.*"

# Or using mise (if installed):
mise run test-e2e
```

### Run Specific E2E Test

```powershell
.\gradlew.bat test --tests "adrianmikula.jakartamigration.e2e.WebhookCommitAnalysisE2ETest"
```

## Test Isolation

E2E tests use `@Transactional` annotation to ensure:
- Each test runs in its own transaction
- Database changes are rolled back after each test
- Tests don't interfere with each other
- Clean state for each test execution

## Test Infrastructure

### AbstractComponentTest

All E2E tests extend `AbstractComponentTest`, which provides:

- **PostgreSQL Container:** Shared PostgreSQL 15 Alpine container
- **Spring Boot Test Context:** Full application context with all beans
- **Dynamic Properties:** Database connection configured from container
- **Test Profile:** Uses `component-test` profile

### Database Setup

- **Container:** PostgreSQL 15 Alpine
- **Database Name:** `template_test`
- **Username:** `test`
- **Password:** `test`
- **Startup Timeout:** 30 seconds
- **Port:** Dynamically assigned

## Best Practices

1. **Use @Transactional:** Always annotate E2E test methods with `@Transactional` for isolation
2. **Clean Setup:** Use `@BeforeEach` to clean up test data before each test
3. **Realistic Data:** Use realistic test data that matches production scenarios
4. **Complete Workflows:** Test complete user workflows, not just individual operations
5. **Clear Assertions:** Verify both the operation result and the persisted state

## Future E2E Tests

Consider adding E2E tests for:

- **Webhook Processing:** Complete webhook receipt → analysis → bounty creation flow
- **Bounty Workflow:** Bounty discovery → triage → filtering → claim workflow
- **Repository Analysis:** Repository addition → clone → code analysis → findings creation
- **PR Processing:** PR creation → analysis → bounty association workflow

## Notes

- The class name `WebhookCommitAnalysisE2ETest` is currently a template/placeholder
- The actual test focuses on repository management, not webhook commit analysis
- Consider renaming the class to better reflect its actual purpose (e.g., `RepositoryManagementE2ETest`)
- Future webhook commit analysis E2E tests should be added as separate test methods or classes

