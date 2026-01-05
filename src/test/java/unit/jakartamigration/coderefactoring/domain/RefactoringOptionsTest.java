package unit.jakartamigration.coderefactoring.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for RefactoringOptions.
 */
@DisplayName("RefactoringOptions Tests")
class RefactoringOptionsTest {
    
    @Test
    @DisplayName("Should create default options successfully")
    void shouldCreateDefaultOptionsSuccessfully() {
        // Given
        var projectPath = Paths.get("/test/project");
        
        // When
        RefactoringOptions options = RefactoringOptions.defaults(projectPath);
        
        // Then
        assertThat(options).isNotNull();
        assertThat(options.projectPath()).isEqualTo(projectPath);
        assertThat(options.createCheckpoints()).isTrue();
        assertThat(options.validateAfterRefactoring()).isTrue();
        assertThat(options.dryRun()).isFalse();
        assertThat(options.excludedFiles()).isEmpty();
        assertThat(options.maxRetries()).isEqualTo(3);
    }
    
    @Test
    @DisplayName("Should create dry-run options successfully")
    void shouldCreateDryRunOptionsSuccessfully() {
        // Given
        var projectPath = Paths.get("/test/project");
        
        // When
        RefactoringOptions options = RefactoringOptions.dryRun(projectPath);
        
        // Then
        assertThat(options).isNotNull();
        assertThat(options.projectPath()).isEqualTo(projectPath);
        assertThat(options.dryRun()).isTrue();
        assertThat(options.createCheckpoints()).isFalse();
        assertThat(options.validateAfterRefactoring()).isFalse();
    }
    
    @Test
    @DisplayName("Should throw exception when project path is null")
    void shouldThrowExceptionWhenProjectPathIsNull() {
        // When/Then
        assertThatThrownBy(() -> new RefactoringOptions(
            null,
            true,
            true,
            false,
            List.of(),
            3
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ProjectPath cannot be null");
    }
    
    @Test
    @DisplayName("Should throw exception when excluded files is null")
    void shouldThrowExceptionWhenExcludedFilesIsNull() {
        // Given
        var projectPath = Paths.get("/test/project");
        
        // When/Then
        assertThatThrownBy(() -> new RefactoringOptions(
            projectPath,
            true,
            true,
            false,
            null,
            3
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ExcludedFiles cannot be null");
    }
    
    @Test
    @DisplayName("Should throw exception when max retries is negative")
    void shouldThrowExceptionWhenMaxRetriesIsNegative() {
        // Given
        var projectPath = Paths.get("/test/project");
        
        // When/Then
        assertThatThrownBy(() -> new RefactoringOptions(
            projectPath,
            true,
            true,
            false,
            List.of(),
            -1
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("MaxRetries cannot be negative");
    }
}

