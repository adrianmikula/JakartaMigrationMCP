package unit.jakartamigration.coderefactoring.service;

import com.bugbounty.jakartamigration.coderefactoring.domain.*;
import com.bugbounty.jakartamigration.coderefactoring.service.*;
import com.bugbounty.jakartamigration.coderefactoring.service.impl.CodeRefactoringModuleImpl;
import com.bugbounty.jakartamigration.dependencyanalysis.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CodeRefactoringModule.
 */
@DisplayName("CodeRefactoringModule Tests")
class CodeRefactoringModuleTest {
    
    private CodeRefactoringModule module;
    private MigrationPlanner migrationPlanner;
    private RecipeLibrary recipeLibrary;
    private RefactoringEngine refactoringEngine;
    private ChangeTracker changeTracker;
    private ProgressTracker progressTracker;
    
    @BeforeEach
    void setUp() {
        migrationPlanner = mock(MigrationPlanner.class);
        recipeLibrary = mock(RecipeLibrary.class);
        refactoringEngine = mock(RefactoringEngine.class);
        changeTracker = mock(ChangeTracker.class);
        progressTracker = mock(ProgressTracker.class);
        
        module = new CodeRefactoringModuleImpl(
            migrationPlanner,
            recipeLibrary,
            refactoringEngine,
            changeTracker,
            progressTracker
        );
    }
    
    @Test
    @DisplayName("Should create migration plan successfully")
    void shouldCreateMigrationPlanSuccessfully() {
        // Given
        String projectPath = "/test/project";
        DependencyAnalysisReport report = createTestReport();
        MigrationPlan expectedPlan = createTestPlan();
        
        when(migrationPlanner.createPlan(projectPath, report)).thenReturn(expectedPlan);
        
        // When
        MigrationPlan result = module.createMigrationPlan(projectPath, report);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(expectedPlan);
        verify(migrationPlanner).createPlan(projectPath, report);
        verify(progressTracker).initialize(projectPath, expectedPlan.totalFileCount());
    }
    
    @Test
    @DisplayName("Should throw exception when project path is null")
    void shouldThrowExceptionWhenProjectPathIsNull() {
        // Given
        DependencyAnalysisReport report = createTestReport();
        
        // When/Then
        assertThatThrownBy(() -> module.createMigrationPlan(null, report))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ProjectPath cannot be null or blank");
    }
    
    @Test
    @DisplayName("Should throw exception when dependency report is null")
    void shouldThrowExceptionWhenDependencyReportIsNull() {
        // Given
        String projectPath = "/test/project";
        
        // When/Then
        assertThatThrownBy(() -> module.createMigrationPlan(projectPath, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("DependencyReport cannot be null");
    }
    
    @Test
    @DisplayName("Should get progress successfully")
    void shouldGetProgressSuccessfully() {
        // Given
        String projectPath = "/test/project";
        MigrationProgress expectedProgress = createTestProgress();
        
        when(progressTracker.getProgress(projectPath)).thenReturn(expectedProgress);
        
        // When
        MigrationProgress result = module.getProgress(projectPath);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEqualTo(expectedProgress);
        verify(progressTracker).getProgress(projectPath);
    }
    
    @Test
    @DisplayName("Should throw exception when project path is null for getProgress")
    void shouldThrowExceptionWhenProjectPathIsNullForGetProgress() {
        // When/Then
        assertThatThrownBy(() -> module.getProgress(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ProjectPath cannot be null or blank");
    }
    
    @Test
    @DisplayName("Should validate refactoring successfully")
    void shouldValidateRefactoringSuccessfully() {
        // Given
        String filePath = "Test.java";
        RefactoringChanges changes = createTestChanges();
        
        // When
        ValidationResult result = module.validateRefactoring(filePath, changes);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.filePath()).isEqualTo(filePath);
        assertThat(result.isValid()).isTrue();
    }
    
    @Test
    @DisplayName("Should detect validation issues")
    void shouldDetectValidationIssues() {
        // Given
        String filePath = "Test.java";
        RefactoringChanges changes = createTestChangesWithJavax();
        
        // When
        ValidationResult result = module.validateRefactoring(filePath, changes);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.hasCriticalIssues() || !result.issues().isEmpty()).isTrue();
    }
    
    @Test
    @DisplayName("Should throw exception when file path is null for validateRefactoring")
    void shouldThrowExceptionWhenFilePathIsNullForValidateRefactoring() {
        // Given
        RefactoringChanges changes = createTestChanges();
        
        // When/Then
        assertThatThrownBy(() -> module.validateRefactoring(null, changes))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("FilePath cannot be null or blank");
    }
    
    @Test
    @DisplayName("Should throw exception when changes is null for validateRefactoring")
    void shouldThrowExceptionWhenChangesIsNullForValidateRefactoring() {
        // Given
        String filePath = "Test.java";
        
        // When/Then
        assertThatThrownBy(() -> module.validateRefactoring(filePath, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Changes cannot be null");
    }
    
    // Helper methods
    
    private DependencyAnalysisReport createTestReport() {
        return new DependencyAnalysisReport(
            new DependencyGraph(new java.util.HashSet<>(), new java.util.HashSet<>()),
            new NamespaceCompatibilityMap(java.util.Map.of()),
            List.of(),
            List.of(),
            new RiskAssessment(0.5, List.of("Low risk"), List.of()),
            new MigrationReadinessScore(0.8, "Ready for migration")
        );
    }
    
    private MigrationPlan createTestPlan() {
        return new MigrationPlan(
            List.of(new RefactoringPhase(
                1,
                "Test Phase",
                List.of("Test.java"),
                List.of("AddJakartaNamespace"),
                List.of(),
                Duration.ofMinutes(10)
            )),
            List.of("Test.java"),
            Duration.ofMinutes(10),
            new RiskAssessment(0.5, List.of(), List.of()),
            List.of()
        );
    }
    
    private MigrationProgress createTestProgress() {
        return new MigrationProgress(
            MigrationState.NOT_STARTED,
            0,
            new ProgressStatistics(10, 0, 0, 10),
            List.of(),
            java.time.LocalDateTime.now()
        );
    }
    
    private RefactoringChanges createTestChanges() {
        return new RefactoringChanges(
            "Test.java",
            "package test;\nimport jakarta.servlet.ServletException;",
            "package test;\nimport jakarta.servlet.ServletException;",
            List.of(),
            List.of(Recipe.jakartaNamespaceRecipe())
        );
    }
    
    private RefactoringChanges createTestChangesWithJavax() {
        return new RefactoringChanges(
            "Test.java",
            "package test;\nimport jakarta.servlet.ServletException;",
            "package test;\nimport javax.servlet.ServletException;",
            List.of(),
            List.of(Recipe.jakartaNamespaceRecipe())
        );
    }
}

