package unit.jakartamigration.coderefactoring.service;

import com.bugbounty.jakartamigration.coderefactoring.domain.Checkpoint;
import com.bugbounty.jakartamigration.coderefactoring.domain.MigrationProgress;
import com.bugbounty.jakartamigration.coderefactoring.domain.MigrationState;
import com.bugbounty.jakartamigration.coderefactoring.domain.ProgressStatistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ProgressTracker Tests")
class ProgressTrackerTest {
    
    private final ProgressTracker tracker = new ProgressTracker();
    
    @Test
    @DisplayName("Should initialize progress with NOT_STARTED state")
    void shouldInitializeProgress() {
        // Given
        String projectPath = "test-project";
        int totalFiles = 100;
        
        // When
        tracker.initialize(projectPath, totalFiles);
        MigrationProgress progress = tracker.getProgress(projectPath);
        
        // Then
        assertThat(progress).isNotNull();
        assertThat(progress.currentState()).isEqualTo(MigrationState.NOT_STARTED);
        assertThat(progress.statistics().totalFiles()).isEqualTo(100);
        assertThat(progress.statistics().refactoredFiles()).isEqualTo(0);
        assertThat(progress.statistics().pendingFiles()).isEqualTo(100);
    }
    
    @Test
    @DisplayName("Should update progress when file is refactored")
    void shouldUpdateProgressOnRefactoring() {
        // Given
        String projectPath = "test-project";
        tracker.initialize(projectPath, 10);
        
        // When
        tracker.markFileRefactored(projectPath, "File1.java");
        tracker.markFileRefactored(projectPath, "File2.java");
        MigrationProgress progress = tracker.getProgress(projectPath);
        
        // Then
        assertThat(progress.statistics().refactoredFiles()).isEqualTo(2);
        assertThat(progress.statistics().pendingFiles()).isEqualTo(8);
        assertThat(progress.currentState()).isEqualTo(MigrationState.IN_PROGRESS);
    }
    
    @Test
    @DisplayName("Should update progress when file fails")
    void shouldUpdateProgressOnFailure() {
        // Given
        String projectPath = "test-project";
        tracker.initialize(projectPath, 10);
        tracker.markFileRefactored(projectPath, "File1.java");
        
        // When
        tracker.markFileFailed(projectPath, "File2.java");
        MigrationProgress progress = tracker.getProgress(projectPath);
        
        // Then
        assertThat(progress.statistics().refactoredFiles()).isEqualTo(1);
        assertThat(progress.statistics().failedFiles()).isEqualTo(1);
        assertThat(progress.statistics().pendingFiles()).isEqualTo(8);
    }
    
    @Test
    @DisplayName("Should add checkpoint when file is refactored")
    void shouldAddCheckpointOnRefactoring() {
        // Given
        String projectPath = "test-project";
        tracker.initialize(projectPath, 5);
        Checkpoint checkpoint = new Checkpoint(
            "checkpoint-1",
            "File1.java",
            LocalDateTime.now(),
            "Refactored"
        );
        
        // When
        tracker.addCheckpoint(projectPath, checkpoint);
        tracker.markFileRefactored(projectPath, "File1.java");
        MigrationProgress progress = tracker.getProgress(projectPath);
        
        // Then
        assertThat(progress.checkpoints()).hasSize(1);
        assertThat(progress.checkpoints().get(0).checkpointId()).isEqualTo("checkpoint-1");
    }
    
    @Test
    @DisplayName("Should mark migration as complete when all files are done")
    void shouldMarkCompleteWhenAllFilesDone() {
        // Given
        String projectPath = "test-project";
        tracker.initialize(projectPath, 3);
        
        // When
        tracker.markFileRefactored(projectPath, "File1.java");
        tracker.markFileRefactored(projectPath, "File2.java");
        tracker.markFileRefactored(projectPath, "File3.java");
        MigrationProgress progress = tracker.getProgress(projectPath);
        
        // Then
        assertThat(progress.statistics().isComplete()).isTrue();
        assertThat(progress.statistics().refactoredFiles()).isEqualTo(3);
        assertThat(progress.statistics().pendingFiles()).isEqualTo(0);
    }
}

