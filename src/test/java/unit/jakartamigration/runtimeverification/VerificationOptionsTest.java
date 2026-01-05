package unit.jakartamigration.runtimeverification;

import com.bugbounty.jakartamigration.runtimeverification.domain.VerificationOptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("VerificationOptions Tests")
class VerificationOptionsTest {
    
    @Test
    @DisplayName("Should create VerificationOptions with valid data")
    void shouldCreateVerificationOptionsWithValidData() {
        // When
        VerificationOptions options = new VerificationOptions(
            Duration.ofMinutes(5),
            2L * 1024 * 1024 * 1024, // 2GB
            true,
            true,
            Collections.emptyList()
        );
        
        // Then
        assertNotNull(options);
        assertEquals(Duration.ofMinutes(5), options.timeout());
        assertEquals(2L * 1024 * 1024 * 1024, options.maxMemoryBytes());
        assertTrue(options.captureStdout());
        assertTrue(options.captureStderr());
    }
    
    @Test
    @DisplayName("Should create default VerificationOptions")
    void shouldCreateDefaultVerificationOptions() {
        // When
        VerificationOptions options = VerificationOptions.defaults();
        
        // Then
        assertNotNull(options);
        assertEquals(Duration.ofMinutes(5), options.timeout());
        assertEquals(2L * 1024 * 1024 * 1024, options.maxMemoryBytes());
        assertTrue(options.captureStdout());
        assertTrue(options.captureStderr());
    }
    
    @Test
    @DisplayName("Should throw exception when maxMemoryBytes is negative")
    void shouldThrowExceptionWhenMaxMemoryBytesIsNegative() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> {
            new VerificationOptions(
                Duration.ofMinutes(5),
                -1, // Invalid negative value
                true,
                true,
                Collections.emptyList()
            );
        });
    }
}

