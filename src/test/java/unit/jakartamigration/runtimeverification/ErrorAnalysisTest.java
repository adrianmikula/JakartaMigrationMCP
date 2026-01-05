package unit.jakartamigration.runtimeverification;

import com.bugbounty.jakartamigration.runtimeverification.domain.ErrorAnalysis;
import com.bugbounty.jakartamigration.runtimeverification.domain.ErrorCategory;
import com.bugbounty.jakartamigration.runtimeverification.domain.RemediationStep;
import com.bugbounty.jakartamigration.runtimeverification.domain.SimilarPastFailure;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ErrorAnalysis Tests")
class ErrorAnalysisTest {
    
    @Test
    @DisplayName("Should create ErrorAnalysis with valid data")
    void shouldCreateErrorAnalysisWithValidData() {
        // Given
        RemediationStep step = new RemediationStep(
            "Update import statement",
            "Replace javax.servlet with jakarta.servlet",
            List.of("Update all imports", "Rebuild project"),
            1
        );
        
        SimilarPastFailure pastFailure = new SimilarPastFailure(
            "ClassNotFoundException: javax.servlet",
            "Updated to jakarta.servlet",
            0.85
        );
        
        // When
        ErrorAnalysis analysis = new ErrorAnalysis(
            ErrorCategory.NAMESPACE_MIGRATION,
            "javax classes not migrated to jakarta",
            List.of("Missing jakarta dependency", "Old import statements"),
            List.of(pastFailure),
            List.of(step),
            0.9
        );
        
        // Then
        assertNotNull(analysis);
        assertEquals(ErrorCategory.NAMESPACE_MIGRATION, analysis.category());
        assertEquals("javax classes not migrated to jakarta", analysis.rootCause());
        assertEquals(2, analysis.contributingFactors().size());
        assertEquals(1, analysis.similarFailures().size());
        assertEquals(1, analysis.suggestedFixes().size());
        assertEquals(0.9, analysis.confidence());
    }
    
    @Test
    @DisplayName("Should throw exception when confidence is out of range")
    void shouldThrowExceptionWhenConfidenceOutOfRange() {
        // When/Then
        assertThrows(IllegalArgumentException.class, () -> {
            new ErrorAnalysis(
                ErrorCategory.NAMESPACE_MIGRATION,
                "Root cause",
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList(),
                1.5 // Invalid confidence > 1.0
            );
        });
    }
}

