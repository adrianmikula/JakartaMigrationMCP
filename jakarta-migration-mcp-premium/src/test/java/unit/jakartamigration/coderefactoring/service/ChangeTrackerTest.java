package unit.jakartamigration.coderefactoring.service;

import adrianmikula.jakartamigration.coderefactoring.domain.Checkpoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ChangeTracker Tests")
class ChangeTrackerTest {
    
    private final ChangeTracker tracker = new ChangeTracker();
    
    @Test
    @DisplayName("Should create checkpoint for file")
    void shouldCreateCheckpoint() {
        // Given
        String filePath = "src/main/java/Example.java";
        String originalContent = "package javax.servlet;";
        String description = "Before Jakarta migration";
        
        // When
        String checkpointId = tracker.createCheckpoint(filePath, originalContent, description);
        
        // Then
        assertThat(checkpointId).isNotNull();
        assertThat(checkpointId).isNotBlank();
    }
    
    @Test
    @DisplayName("Should retrieve checkpoint by ID")
    void shouldRetrieveCheckpoint() {
        // Given
        String filePath = "src/main/java/Example.java";
        String originalContent = "package javax.servlet;";
        String checkpointId = tracker.createCheckpoint(filePath, originalContent, "Test checkpoint");
        
        // When
        Optional<Checkpoint> checkpoint = tracker.getCheckpoint(checkpointId);
        
        // Then
        assertThat(checkpoint).isPresent();
        assertThat(checkpoint.get().filePath()).isEqualTo(filePath);
        assertThat(checkpoint.get().description()).isEqualTo("Test checkpoint");
    }
    
    @Test
    @DisplayName("Should return empty when checkpoint not found")
    void shouldReturnEmptyWhenCheckpointNotFound() {
        // When
        Optional<Checkpoint> checkpoint = tracker.getCheckpoint("non-existent-id");
        
        // Then
        assertThat(checkpoint).isEmpty();
    }
    
    @Test
    @DisplayName("Should retrieve original content from checkpoint")
    void shouldRetrieveOriginalContent() {
        // Given
        String filePath = "src/main/java/Example.java";
        String originalContent = "package javax.servlet;\npublic class Example {}";
        String checkpointId = tracker.createCheckpoint(filePath, originalContent, "Original");
        
        // When
        Optional<String> content = tracker.getOriginalContent(checkpointId);
        
        // Then
        assertThat(content).isPresent();
        assertThat(content.get()).isEqualTo(originalContent);
    }
}

