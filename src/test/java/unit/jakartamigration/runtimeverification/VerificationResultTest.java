package unit.jakartamigration.runtimeverification;

import com.bugbounty.jakartamigration.runtimeverification.domain.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("VerificationResult Tests")
class VerificationResultTest {
    
    @Test
    @DisplayName("Should create VerificationResult with valid data")
    void shouldCreateVerificationResultWithValidData() {
        // Given
        ExecutionMetrics metrics = new ExecutionMetrics(
            Duration.ofSeconds(10),
            1024 * 1024 * 512, // 512MB
            0,
            false
        );
        
        ErrorAnalysis analysis = new ErrorAnalysis(
            ErrorCategory.NAMESPACE_MIGRATION,
            "javax classes not migrated",
            List.of("Missing jakarta dependency"),
            Collections.emptyList(),
            Collections.emptyList(),
            0.9
        );
        
        // When
        VerificationResult result = new VerificationResult(
            VerificationStatus.SUCCESS,
            Collections.emptyList(),
            Collections.emptyList(),
            metrics,
            analysis,
            Collections.emptyList()
        );
        
        // Then
        assertNotNull(result);
        assertEquals(VerificationStatus.SUCCESS, result.status());
        assertTrue(result.errors().isEmpty());
        assertTrue(result.warnings().isEmpty());
    }
    
    @Test
    @DisplayName("Should create VerificationResult with errors")
    void shouldCreateVerificationResultWithErrors() {
        // Given
        StackTrace stackTrace = new StackTrace(
            "java.lang.ClassNotFoundException",
            "javax.servlet.ServletException",
            List.of()
        );
        
        RuntimeError error = new RuntimeError(
            ErrorType.CLASS_NOT_FOUND,
            "javax.servlet.ServletException not found",
            stackTrace,
            "javax.servlet.ServletException",
            "testMethod",
            LocalDateTime.now(),
            0.95
        );
        
        ExecutionMetrics metrics = new ExecutionMetrics(
            Duration.ofSeconds(5),
            1024 * 1024,
            1,
            false
        );
        
        ErrorAnalysis analysis = new ErrorAnalysis(
            ErrorCategory.NAMESPACE_MIGRATION,
            "javax classes not migrated",
            List.of(),
            Collections.emptyList(),
            Collections.emptyList(),
            0.9
        );
        
        // When
        VerificationResult result = new VerificationResult(
            VerificationStatus.FAILED,
            List.of(error),
            Collections.emptyList(),
            metrics,
            analysis,
            Collections.emptyList()
        );
        
        // Then
        assertEquals(VerificationStatus.FAILED, result.status());
        assertEquals(1, result.errors().size());
        assertEquals(error, result.errors().get(0));
    }
    
    @Test
    @DisplayName("Should throw exception when required fields are null")
    void shouldThrowExceptionWhenRequiredFieldsAreNull() {
        // Given
        ExecutionMetrics metrics = new ExecutionMetrics(
            Duration.ofSeconds(10),
            1024 * 1024,
            0,
            false
        );
        
        ErrorAnalysis analysis = new ErrorAnalysis(
            ErrorCategory.NAMESPACE_MIGRATION,
            "Root cause",
            List.of(),
            Collections.emptyList(),
            Collections.emptyList(),
            0.9
        );
        
        // When/Then
        assertThrows(NullPointerException.class, () -> {
            new VerificationResult(
                null, // status is null
                Collections.emptyList(),
                Collections.emptyList(),
                metrics,
                analysis,
                Collections.emptyList()
            );
        });
    }
}

