package unit.jakartamigration.coderefactoring.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for ValidationResult.
 */
@DisplayName("ValidationResult Tests")
class ValidationResultTest {
    
    @Test
    @DisplayName("Should create successful validation result")
    void shouldCreateSuccessfulValidationResult() {
        // Given
        String filePath = "Test.java";
        List<ValidationIssue> issues = List.of();
        
        // When
        ValidationResult result = new ValidationResult(
            true,
            issues,
            filePath,
            ValidationStatus.PASSED
        );
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.hasCriticalIssues()).isFalse();
    }
    
    @Test
    @DisplayName("Should detect critical issues")
    void shouldDetectCriticalIssues() {
        // Given
        String filePath = "Test.java";
        List<ValidationIssue> issues = List.of(
            new ValidationIssue(1, "Critical error", ValidationSeverity.CRITICAL, "Fix it")
        );
        
        // When
        ValidationResult result = new ValidationResult(
            false,
            issues,
            filePath,
            ValidationStatus.FAILED
        );
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.hasCriticalIssues()).isTrue();
        assertThat(result.isSuccessful()).isFalse();
    }
    
    @Test
    @DisplayName("Should throw exception when issues is null")
    void shouldThrowExceptionWhenIssuesIsNull() {
        // When/Then
        assertThatThrownBy(() -> new ValidationResult(
            true,
            null,
            "Test.java",
            ValidationStatus.PASSED
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Issues cannot be null");
    }
    
    @Test
    @DisplayName("Should throw exception when file path is null")
    void shouldThrowExceptionWhenFilePathIsNull() {
        // When/Then
        assertThatThrownBy(() -> new ValidationResult(
            true,
            List.of(),
            null,
            ValidationStatus.PASSED
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("FilePath cannot be null or blank");
    }
    
    @Test
    @DisplayName("Should throw exception when status is null")
    void shouldThrowExceptionWhenStatusIsNull() {
        // When/Then
        assertThatThrownBy(() -> new ValidationResult(
            true,
            List.of(),
            "Test.java",
            null
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Status cannot be null");
    }
}

