package unit.jakartamigration.runtimeverification;

import adrianmikula.jakartamigration.runtimeverification.domain.ErrorType;
import adrianmikula.jakartamigration.runtimeverification.domain.RuntimeError;
import adrianmikula.jakartamigration.runtimeverification.domain.StackTrace;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RuntimeError Tests")
class RuntimeErrorTest {
    
    @Test
    @DisplayName("Should create RuntimeError with valid data")
    void shouldCreateRuntimeErrorWithValidData() {
        // Given
        StackTrace stackTrace = new StackTrace(
            "java.lang.ClassNotFoundException",
            "javax.servlet.ServletException",
            List.of(new StackTrace.StackTraceElement(
                "com.example.Test",
                "testMethod",
                "Test.java",
                42
            ))
        );
        
        // When
        RuntimeError error = new RuntimeError(
            ErrorType.CLASS_NOT_FOUND,
            "javax.servlet.ServletException not found",
            stackTrace,
            "javax.servlet.ServletException",
            "testMethod",
            LocalDateTime.now(),
            0.95
        );
        
        // Then
        assertNotNull(error);
        assertEquals(ErrorType.CLASS_NOT_FOUND, error.type());
        assertEquals("javax.servlet.ServletException not found", error.message());
        assertEquals(0.95, error.confidence());
    }
    
    @Test
    @DisplayName("Should throw exception when confidence is out of range")
    void shouldThrowExceptionWhenConfidenceOutOfRange() {
        // Given
        StackTrace stackTrace = new StackTrace(
            "java.lang.ClassNotFoundException",
            "Error",
            List.of()
        );
        
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> {
            new RuntimeError(
                ErrorType.CLASS_NOT_FOUND,
                "Error",
                stackTrace,
                "Test",
                "method",
                LocalDateTime.now(),
                1.5 // Invalid confidence > 1.0
            );
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            new RuntimeError(
                ErrorType.CLASS_NOT_FOUND,
                "Error",
                stackTrace,
                "Test",
                "method",
                LocalDateTime.now(),
                -0.1 // Invalid confidence < 0.0
            );
        });
    }
    
    @Test
    @DisplayName("Should throw exception when required fields are null")
    void shouldThrowExceptionWhenRequiredFieldsAreNull() {
        // Given
        StackTrace stackTrace = new StackTrace(
            "java.lang.ClassNotFoundException",
            "Error",
            List.of()
        );
        
        // When/Then
        assertThrows(NullPointerException.class, () -> {
            new RuntimeError(
                null, // type is null
                "Error",
                stackTrace,
                "Test",
                "method",
                LocalDateTime.now(),
                0.9
            );
        });
        
        assertThrows(NullPointerException.class, () -> {
            new RuntimeError(
                ErrorType.CLASS_NOT_FOUND,
                null, // message is null
                stackTrace,
                "Test",
                "method",
                LocalDateTime.now(),
                0.9
            );
        });
    }
}

