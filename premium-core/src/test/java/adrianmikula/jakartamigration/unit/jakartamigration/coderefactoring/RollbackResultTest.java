package unit.jakartamigration.coderefactoring.domain;

import adrianmikula.jakartamigration.coderefactoring.domain.RollbackResult;
import adrianmikula.jakartamigration.coderefactoring.domain.RollbackStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for RollbackResult.
 */
@DisplayName("RollbackResult Tests")
class RollbackResultTest {
    
    @Test
    @DisplayName("Should create successful rollback result")
    void shouldCreateSuccessfulRollbackResult() {
        // Given
        String filePath = "Test.java";
        String checkpointId = "checkpoint-123";
        
        // When
        RollbackResult result = RollbackResult.success(filePath, checkpointId);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.success()).isTrue();
        assertThat(result.filePath()).isEqualTo(filePath);
        assertThat(result.checkpointId()).isEqualTo(checkpointId);
        assertThat(result.status()).isEqualTo(RollbackStatus.SUCCESS);
        assertThat(result.message()).contains("successfully");
    }
    
    @Test
    @DisplayName("Should create failed rollback result")
    void shouldCreateFailedRollbackResult() {
        // Given
        String filePath = "Test.java";
        String checkpointId = "checkpoint-123";
        String reason = "Checkpoint not found";
        
        // When
        RollbackResult result = RollbackResult.failure(filePath, checkpointId, reason);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.success()).isFalse();
        assertThat(result.filePath()).isEqualTo(filePath);
        assertThat(result.checkpointId()).isEqualTo(checkpointId);
        assertThat(result.status()).isEqualTo(RollbackStatus.FAILED);
        assertThat(result.message()).isEqualTo(reason);
    }
    
    @Test
    @DisplayName("Should throw exception when file path is null")
    void shouldThrowExceptionWhenFilePathIsNull() {
        // When/Then
        assertThatThrownBy(() -> new RollbackResult(
            true,
            null,
            "checkpoint-123",
            "Message",
            RollbackStatus.SUCCESS
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("FilePath cannot be null or blank");
    }
    
    @Test
    @DisplayName("Should throw exception when checkpoint id is null")
    void shouldThrowExceptionWhenCheckpointIdIsNull() {
        // When/Then
        assertThatThrownBy(() -> new RollbackResult(
            true,
            "Test.java",
            null,
            "Message",
            RollbackStatus.SUCCESS
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("CheckpointId cannot be null or blank");
    }
    
    @Test
    @DisplayName("Should throw exception when message is null")
    void shouldThrowExceptionWhenMessageIsNull() {
        // When/Then
        assertThatThrownBy(() -> new RollbackResult(
            true,
            "Test.java",
            "checkpoint-123",
            null,
            RollbackStatus.SUCCESS
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Message cannot be null");
    }
    
    @Test
    @DisplayName("Should throw exception when status is null")
    void shouldThrowExceptionWhenStatusIsNull() {
        // When/Then
        assertThatThrownBy(() -> new RollbackResult(
            true,
            "Test.java",
            "checkpoint-123",
            "Message",
            null
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Status cannot be null");
    }
}

