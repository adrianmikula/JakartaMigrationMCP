package adrianmikula.jakartamigration.coderefactoring.service;

import adrianmikula.jakartamigration.coderefactoring.domain.Checkpoint;
import adrianmikula.jakartamigration.coderefactoring.domain.MigrationProgress;
import adrianmikula.jakartamigration.coderefactoring.domain.MigrationState;
import adrianmikula.jakartamigration.coderefactoring.domain.ProgressStatistics;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks migration progress across the codebase.
 */
public class ProgressTracker {
    
    private final Map<String, MigrationProgress> progressMap = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> refactoredFiles = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> failedFiles = new ConcurrentHashMap<>();
    private final Map<String, List<Checkpoint>> checkpoints = new ConcurrentHashMap<>();
    
    /**
     * Initializes progress tracking for a project.
     */
    public void initialize(String projectPath, int totalFiles) {
        if (projectPath == null || projectPath.isBlank()) {
            throw new IllegalArgumentException("ProjectPath cannot be null or blank");
        }
        if (totalFiles < 0) {
            throw new IllegalArgumentException("TotalFiles cannot be negative");
        }
        
        MigrationProgress progress = new MigrationProgress(
            MigrationState.NOT_STARTED,
            0,
            new ProgressStatistics(totalFiles, 0, 0, totalFiles),
            List.of(),
            LocalDateTime.now()
        );
        
        progressMap.put(projectPath, progress);
        refactoredFiles.put(projectPath, new HashSet<>());
        failedFiles.put(projectPath, new HashSet<>());
        checkpoints.put(projectPath, new ArrayList<>());
    }
    
    /**
     * Gets the current progress for a project.
     */
    public MigrationProgress getProgress(String projectPath) {
        return progressMap.getOrDefault(projectPath, createEmptyProgress());
    }
    
    /**
     * Marks a file as refactored.
     */
    public void markFileRefactored(String projectPath, String filePath) {
        updateProgress(projectPath, filePath, true);
    }
    
    /**
     * Marks a file as failed.
     */
    public void markFileFailed(String projectPath, String filePath) {
        updateProgress(projectPath, filePath, false);
    }
    
    /**
     * Adds a checkpoint to the progress.
     */
    public void addCheckpoint(String projectPath, Checkpoint checkpoint) {
        if (projectPath == null || projectPath.isBlank()) {
            throw new IllegalArgumentException("ProjectPath cannot be null or blank");
        }
        if (checkpoint == null) {
            throw new IllegalArgumentException("Checkpoint cannot be null");
        }
        
        checkpoints.computeIfAbsent(projectPath, k -> new ArrayList<>()).add(checkpoint);
        updateProgressState(projectPath);
    }
    
    /**
     * Updates the current phase.
     */
    public void updatePhase(String projectPath, int phase) {
        MigrationProgress current = getProgress(projectPath);
        MigrationProgress updated = new MigrationProgress(
            current.currentState(),
            phase,
            current.statistics(),
            current.checkpoints(),
            LocalDateTime.now()
        );
        progressMap.put(projectPath, updated);
    }
    
    private void updateProgress(String projectPath, String filePath, boolean success) {
        Set<String> refactored = refactoredFiles.computeIfAbsent(projectPath, k -> new HashSet<>());
        Set<String> failed = failedFiles.computeIfAbsent(projectPath, k -> new HashSet<>());
        
        if (success) {
            refactored.add(filePath);
            failed.remove(filePath);
        } else {
            failed.add(filePath);
            refactored.remove(filePath);
        }
        
        updateProgressState(projectPath);
    }
    
    private void updateProgressState(String projectPath) {
        MigrationProgress current = getProgress(projectPath);
        ProgressStatistics stats = calculateStatistics(projectPath);
        
        MigrationState newState = determineState(stats);
        
        MigrationProgress updated = new MigrationProgress(
            newState,
            current.currentPhase(),
            stats,
            checkpoints.getOrDefault(projectPath, List.of()),
            LocalDateTime.now()
        );
        
        progressMap.put(projectPath, updated);
    }
    
    private ProgressStatistics calculateStatistics(String projectPath) {
        MigrationProgress current = getProgress(projectPath);
        int totalFiles = current.statistics().totalFiles();
        
        Set<String> refactored = refactoredFiles.getOrDefault(projectPath, Set.of());
        Set<String> failed = failedFiles.getOrDefault(projectPath, Set.of());
        
        int refactoredCount = refactored.size();
        int failedCount = failed.size();
        int pendingCount = totalFiles - refactoredCount - failedCount;
        
        return new ProgressStatistics(totalFiles, refactoredCount, failedCount, pendingCount);
    }
    
    private MigrationState determineState(ProgressStatistics stats) {
        if (stats.isComplete()) {
            return MigrationState.COMPLETE;
        } else if (stats.refactoredFiles() > 0 || stats.failedFiles() > 0) {
            return MigrationState.IN_PROGRESS;
        } else {
            return MigrationState.NOT_STARTED;
        }
    }
    
    private MigrationProgress createEmptyProgress() {
        return new MigrationProgress(
            MigrationState.NOT_STARTED,
            0,
            new ProgressStatistics(0, 0, 0, 0),
            List.of(),
            LocalDateTime.now()
        );
    }
}

