# Test Improvement Action Plan

Based on the comprehensive test review, this document outlines specific actions to improve test quality, coverage, and alignment with MCP functionality.

## Quick Wins (Can Do Now)

### 1. Fix Compilation Errors in Excluded Tests ⚡

**Files to Fix**:
- `src/test/java/unit/jakartamigration/coderefactoring/service/ChangeTrackerTest.java`
- `src/test/java/unit/jakartamigration/coderefactoring/service/ProgressTrackerTest.java`
- `src/test/java/unit/jakartamigration/coderefactoring/service/MigrationPlannerTest.java`

**Action**: Check if implementations exist, fix imports, or mark tests as `@Disabled` with clear reasons.

**Estimated Time**: 1-2 hours

---

### 2. Add Missing Test for RecipeLibrary.registerRecipe() Null Check

**File**: `src/test/java/unit/jakartamigration/coderefactoring/service/RecipeLibraryTest.java`

**Missing Test**:
```java
@Test
@DisplayName("Should throw exception when registering null recipe")
void shouldThrowExceptionWhenRegisteringNullRecipe() {
    // Given & When & Then
    assertThatThrownBy(() -> recipeLibrary.registerRecipe(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("cannot be null");
}
```

**Estimated Time**: 15 minutes

---

### 3. Add Missing Test for RecipeLibrary.getAllRecipes()

**File**: `src/test/java/unit/jakartamigration/coderefactoring/service/RecipeLibraryTest.java`

**Missing Test**:
```java
@Test
@DisplayName("Should return all registered recipes")
void shouldGetAllRecipes() {
    // When
    List<Recipe> allRecipes = recipeLibrary.getAllRecipes();
    
    // Then
    assertThat(allRecipes).isNotEmpty();
    assertThat(allRecipes.size()).isGreaterThanOrEqualTo(3); // Default recipes
}
```

**Estimated Time**: 15 minutes

---

### 4. Add Missing Test for RecipeLibrary.hasRecipe()

**File**: `src/test/java/unit/jakartamigration/coderefactoring/service/RecipeLibraryTest.java`

**Missing Test**:
```java
@Test
@DisplayName("Should check if recipe exists")
void shouldCheckIfRecipeExists() {
    // Given
    String existingRecipe = "AddJakartaNamespace";
    String nonExistentRecipe = "NonExistent";
    
    // When & Then
    assertThat(recipeLibrary.hasRecipe(existingRecipe)).isTrue();
    assertThat(recipeLibrary.hasRecipe(nonExistentRecipe)).isFalse();
}
```

**Estimated Time**: 15 minutes

---

## High Priority Actions

### 5. Implement MCP Tools and Add Tests 🎯 **CRITICAL**

**Status**: No MCP tool implementations found

**Required Tools** (based on architecture):
1. `analyze_jakarta_readiness` - Analyze project for migration readiness
2. `migrate_to_jakarta` - Execute Jakarta migration using OpenRewrite
3. `detect_blockers` - Identify migration blockers
4. `recommend_versions` - Suggest Jakarta-compatible versions
5. `verify_runtime` - Runtime verification after migration

**Implementation Steps**:
1. Create `JakartaMigrationTools` class with `@Tool` annotations
2. Implement each tool method
3. Add comprehensive unit tests
4. Add component tests for integration

**Test Requirements**:
- Input validation tests
- Output format tests
- Error handling tests
- Integration with core modules tests

**Estimated Time**: 2-3 days

---

### 6. Enable DependencyAnalysisModuleTest

**File**: `src/test/java/unit/jakartamigration/dependencyanalysis/service/DependencyAnalysisModuleTest.java`

**Action**:
1. Implement `DependencyAnalysisModuleImpl`
2. Remove `@Disabled` annotation
3. Fix test setup in `@BeforeEach`
4. Run tests and fix any failures

**Estimated Time**: 1-2 days

---

### 7. Add Component Tests for Integration Workflows

**Missing Component Tests**:

1. **Dependency Analysis → Migration Planning Integration**:
   ```java
   @SpringBootTest
   @Testcontainers
   class DependencyAnalysisToMigrationPlanningTest extends AbstractComponentTest {
       @Test
       void shouldCreateMigrationPlanFromDependencyAnalysis() {
           // Test complete workflow
       }
   }
   ```

2. **OpenRewrite Recipe Execution**:
   ```java
   @SpringBootTest
   class OpenRewriteRecipeExecutionTest {
       @Test
       void shouldExecuteJakartaNamespaceRecipe() {
           // Test OpenRewrite integration
       }
   }
   ```

3. **Runtime Verification After Migration**:
   ```java
   @SpringBootTest
   class RuntimeVerificationAfterMigrationTest {
       @Test
       void shouldVerifyMigratedApplication() {
           // Test runtime verification
       }
   }
   ```

**Estimated Time**: 2-3 days

---

### 8. Add Gradle Dependency Builder Tests

**Current State**: Only Maven dependency parsing is tested

**Missing Tests**:
- `GradleDependencyGraphBuilderTest.java`
- Tests for `build.gradle` parsing
- Tests for Gradle dependency resolution

**Estimated Time**: 1 day

---

## Medium Priority Actions

### 9. Replace Template Tests with MCP Functionality Tests

**Files to Replace**:
- `ExampleServiceTest.java` → `JakartaMigrationServiceTest.java`
- `ExampleMapperTest.java` → Remove (no longer needed)
- `ExampleControllerTest.java` → `JakartaMigrationControllerTest.java` (if REST API exists)
- `ExampleServiceComponentTest.java` → `JakartaMigrationComponentTest.java`
- `ExampleE2ETest.java` → `JakartaMigrationE2ETest.java`

**Action**: Once MCP tools are implemented, replace template tests with actual functionality tests.

**Estimated Time**: 1 day

---

### 10. Add Error Handling Tests

**Missing Error Scenarios**:

1. **File System Errors**:
   ```java
   @Test
   void shouldHandleMissingProjectFiles() { }
   
   @Test
   void shouldHandlePermissionDenied() { }
   ```

2. **Invalid Input**:
   ```java
   @Test
   void shouldHandleMalformedPomXml() { }
   
   @Test
   void shouldHandleInvalidProjectPath() { }
   ```

3. **Network/External Dependencies**:
   ```java
   @Test
   void shouldHandleMavenRepositoryUnavailable() { }
   
   @Test
   void shouldHandleTimeoutScenarios() { }
   ```

**Estimated Time**: 1-2 days

---

### 11. Add E2E Tests for Complete Workflows

**Missing E2E Tests**:

1. **Complete Jakarta Migration Workflow**:
   ```java
   @Test
   void shouldMigrateSpringBoot2To3WithJakarta() {
       // 1. Analyze dependencies
       // 2. Create migration plan
       // 3. Execute refactoring
       // 4. Verify runtime
   }
   ```

2. **Multi-Module Project Migration**:
   ```java
   @Test
   void shouldMigrateMultiModuleProject() { }
   ```

3. **Migration with Blockers**:
   ```java
   @Test
   void shouldDetectAndReportBlockers() { }
   ```

4. **Migration Rollback**:
   ```java
   @Test
   void shouldRollbackMigration() { }
   ```

**Estimated Time**: 2-3 days

---

## Test Quality Improvements

### 12. Add Test Coverage Thresholds

**Action**: Update `build.gradle.kts` to enforce coverage thresholds:

```kotlin
tasks.jacocoTestReport {
    violationRules {
        rule {
            limit {
                minimum = "0.50".toBigDecimal() // 50% minimum
            }
        }
        rule {
            element = "CLASS"
            excludes = listOf(
                "**.*Config",
                "**.*Entity",
                "**.*DTO",
                "**.*Application"
            )
            limit {
                minimum = "0.80".toBigDecimal() // 80% for classes
            }
        }
    }
}
```

**Estimated Time**: 30 minutes

---

### 13. Add Test Execution Reports

**Action**: Configure test reports to show:
- Test execution time
- Slow tests (> 1s)
- Flaky tests detection

**Estimated Time**: 1 hour

---

## Implementation Priority

### Phase 1: Foundation (Week 1)
1. ✅ Fix compilation errors in excluded tests
2. ✅ Add missing RecipeLibrary tests
3. ✅ Enable DependencyAnalysisModuleTest (if implementation ready)

### Phase 2: Core Functionality (Week 2-3)
4. ✅ Implement MCP tools and add tests
5. ✅ Add component tests for integration workflows
6. ✅ Add Gradle dependency builder tests

### Phase 3: Quality & Coverage (Week 4)
7. ✅ Replace template tests
8. ✅ Add error handling tests
9. ✅ Add E2E tests for complete workflows
10. ✅ Add coverage thresholds

---

## Success Metrics

### Coverage Goals
- **Overall Coverage**: 50%+ (current target)
- **Service Classes**: 80%+
- **Domain Models**: 100%
- **MCP Tools**: 80%+ (once implemented)

### Test Distribution Goals
- **Unit Tests**: 85%+ ✅ (already achieved)
- **Component Tests**: 10-12%
- **E2E Tests**: 3-5%

### Quality Goals
- **All tests pass**: ✅
- **No compilation errors**: ⚠️ (3 files excluded)
- **No disabled tests**: ⚠️ (1 test disabled)
- **Fast execution**: ✅ (unit tests < 1s)

---

## Notes

- **Template Tests**: Keep as reference but clearly mark as templates
- **Disabled Tests**: Document why they're disabled and when they'll be enabled
- **Excluded Tests**: Fix compilation errors or remove if no longer needed
- **MCP Tools**: Critical for project success - prioritize implementation

---

*Last Updated: 2026-01-27*

