package unit.jakartamigration.coderefactoring;

import com.bugbounty.jakartamigration.coderefactoring.domain.Checkpoint;
import com.bugbounty.jakartamigration.coderefactoring.domain.MigrationProgress;
import com.bugbounty.jakartamigration.coderefactoring.domain.MigrationState;
import com.bugbounty.jakartamigration.coderefactoring.domain.ProgressStatistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MigrationProgress Tests")
class MigrationProgressTest {
    
    @Test
    @DisplayName("Should create migration progress with initial state")
    void shouldCreateMigrationProgress() {
        // Given
        MigrationState state = MigrationState.NOT_STARTED;
        int currentPhase = 0;
        ProgressStatistics statistics = new ProgressStatistics(100, 0, 0, 100);
        List<Checkpoint> checkpoints = List.of();
        LocalDateTime lastUpdate = LocalDateTime.now();
        
        // When
        MigrationProgress progress = new MigrationProgress(
            state,
            currentPhase,
            statistics,
            checkpoints,
            lastUpdate
        );
        
        // Then
        assertThat(progress.currentState()).isEqualTo(MigrationState.NOT_STARTED);
        assertThat(progress.currentPhase()).isEqualTo(0);
        assertThat(progress.statistics().totalFiles()).isEqualTo(100);
        assertThat(progress.statistics().refactoredFiles()).isEqualTo(0);
        assertThat(progress.statistics().pendingFiles()).isEqualTo(100);
        assertThat(progress.checkpoints()).isEmpty();
    }
    
    @Test
    @DisplayName("Should create progress with checkpoints")
    void shouldCreateProgressWithCheckpoints() {
        // Given
        Checkpoint checkpoint1 = new Checkpoint(
            "checkpoint-1",
            "File1.java",
            LocalDateTime.now().minusMinutes(10),
            "Phase 1 complete"
        );
        Checkpoint checkpoint2 = new Checkpoint(
            "checkpoint-2",
            "File2.java",
            LocalDateTime.now().minusMinutes(5),
            "Phase 2 complete"
        );
        
        // When
        MigrationProgress progress = new MigrationProgress(
            MigrationState.IN_PROGRESS,
            2,
            new ProgressStatistics(100, 50, 0, 50),
            List.of(checkpoint1, checkpoint2),
            LocalDateTime.now()
        );
        
        // Then
        assertThat(progress.checkpoints()).hasSize(2);
        assertThat(progress.statistics().refactoredFiles()).isEqualTo(50);
        assertThat(progress.statistics().pendingFiles()).isEqualTo(50);
    }
    
    @Test
    @DisplayName("Should calculate progress percentage")
    void shouldCalculateProgressPercentage() {
        // Given
        ProgressStatistics stats = new ProgressStatistics(100, 75, 0, 25);
        
        // When
        double percentage = stats.progressPercentage();
        
        // Then
        assertThat(percentage).isEqualTo(75.0);
    }
}

