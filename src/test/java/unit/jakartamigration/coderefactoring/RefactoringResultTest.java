package unit.jakartamigration.coderefactoring;

import com.bugbounty.jakartamigration.coderefactoring.domain.RefactoringFailure;
import com.bugbounty.jakartamigration.coderefactoring.domain.RefactoringResult;
import com.bugbounty.jakartamigration.coderefactoring.domain.RefactoringStatistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RefactoringResult Tests")
class RefactoringResultTest {
    
    @Test
    @DisplayName("Should create successful refactoring result")
    void shouldCreateSuccessfulResult() {
        // Given
        List<String> refactoredFiles = List.of("File1.java", "File2.java");
        List<RefactoringFailure> failures = List.of();
        RefactoringStatistics statistics = new RefactoringStatistics(2, 0, 2);
        String checkpointId = "checkpoint-123";
        
        // When
        RefactoringResult result = new RefactoringResult(
            refactoredFiles,
            failures,
            statistics,
            checkpointId,
            true
        );
        
        // Then
        assertThat(result.refactoredFiles()).hasSize(2);
        assertThat(result.failures()).isEmpty();
        assertThat(result.statistics().totalFiles()).isEqualTo(2);
        assertThat(result.statistics().successfulFiles()).isEqualTo(2);
        assertThat(result.statistics().failedFiles()).isEqualTo(0);
        assertThat(result.checkpointId()).isEqualTo(checkpointId);
        assertThat(result.canRollback()).isTrue();
    }
    
    @Test
    @DisplayName("Should create result with failures")
    void shouldCreateResultWithFailures() {
        // Given
        List<String> refactoredFiles = List.of("File1.java");
        RefactoringFailure failure = new RefactoringFailure(
            "File2.java",
            "Parse error",
            "Invalid syntax at line 10"
        );
        List<RefactoringFailure> failures = List.of(failure);
        RefactoringStatistics statistics = new RefactoringStatistics(2, 1, 1);
        
        // When
        RefactoringResult result = new RefactoringResult(
            refactoredFiles,
            failures,
            statistics,
            "checkpoint-456",
            false
        );
        
        // Then
        assertThat(result.refactoredFiles()).hasSize(1);
        assertThat(result.failures()).hasSize(1);
        assertThat(result.failures().get(0).filePath()).isEqualTo("File2.java");
        assertThat(result.failures().get(0).errorType()).isEqualTo("Parse error");
        assertThat(result.statistics().failedFiles()).isEqualTo(1);
        assertThat(result.canRollback()).isFalse();
    }
    
    @Test
    @DisplayName("Should calculate success rate correctly")
    void shouldCalculateSuccessRate() {
        // Given
        RefactoringStatistics stats = new RefactoringStatistics(10, 2, 8);
        
        // When
        double successRate = stats.successRate();
        
        // Then
        assertThat(successRate).isEqualTo(0.8);
    }
}

