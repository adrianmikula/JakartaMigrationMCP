package adrianmikula.jakartamigration.coderefactoring.service;

import adrianmikula.jakartamigration.coderefactoring.domain.Checkpoint;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks changes and manages checkpoints for rollback purposes.
 */
public class ChangeTracker {
    
    private final Map<String, Checkpoint> checkpoints = new ConcurrentHashMap<>();
    private final Map<String, String> originalContents = new ConcurrentHashMap<>();
    
    /**
     * Creates a checkpoint for a file before refactoring.
     */
    public String createCheckpoint(String filePath, String originalContent, String description) {
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("FilePath cannot be null or blank");
        }
        if (originalContent == null) {
            throw new IllegalArgumentException("OriginalContent cannot be null");
        }
        if (description == null) {
            throw new IllegalArgumentException("Description cannot be null");
        }
        
        String checkpointId = UUID.randomUUID().toString();
        Checkpoint checkpoint = new Checkpoint(
            checkpointId,
            filePath,
            LocalDateTime.now(),
            description
        );
        
        checkpoints.put(checkpointId, checkpoint);
        originalContents.put(checkpointId, originalContent);
        
        return checkpointId;
    }
    
    /**
     * Gets a checkpoint by ID.
     */
    public Optional<Checkpoint> getCheckpoint(String checkpointId) {
        if (checkpointId == null || checkpointId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(checkpoints.get(checkpointId));
    }
    
    /**
     * Gets the original content for a checkpoint.
     */
    public Optional<String> getOriginalContent(String checkpointId) {
        if (checkpointId == null || checkpointId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(originalContents.get(checkpointId));
    }
    
    /**
     * Removes a checkpoint (after successful rollback or completion).
     */
    public void removeCheckpoint(String checkpointId) {
        checkpoints.remove(checkpointId);
        originalContents.remove(checkpointId);
    }
    
    /**
     * Checks if a checkpoint exists.
     */
    public boolean hasCheckpoint(String checkpointId) {
        return checkpointId != null && checkpoints.containsKey(checkpointId);
    }
}

