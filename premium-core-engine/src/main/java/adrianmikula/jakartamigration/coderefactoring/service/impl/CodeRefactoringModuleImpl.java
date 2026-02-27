package adrianmikula.jakartamigration.coderefactoring.service.impl;

import adrianmikula.jakartamigration.coderefactoring.domain.*;
import adrianmikula.jakartamigration.coderefactoring.service.*;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyAnalysisReport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of the Code Refactoring Module.
 * Systematically refactors code from javax to jakarta using recipes.
 */
@RequiredArgsConstructor
@Slf4j
public class CodeRefactoringModuleImpl implements CodeRefactoringModule {
    
    private final MigrationPlanner migrationPlanner;
    private final RecipeLibrary recipeLibrary;
    private final RefactoringEngine refactoringEngine;
    private final ChangeTracker changeTracker;
    private final ProgressTracker progressTracker;
    
    @Override
    public MigrationPlan createMigrationPlan(
        String projectPath,
        DependencyAnalysisReport dependencyReport
    ) {
        if (projectPath == null || projectPath.isBlank()) {
            throw new IllegalArgumentException("ProjectPath cannot be null or blank");
        }
        if (dependencyReport == null) {
            throw new IllegalArgumentException("DependencyReport cannot be null");
        }
        
        log.info("Creating migration plan for project: {}", projectPath);
        
        MigrationPlan plan = migrationPlanner.createPlan(projectPath, dependencyReport);
        
        // Initialize progress tracking
        progressTracker.initialize(projectPath, plan.totalFileCount());
        
        return plan;
    }
    
    @Override
    public RefactoringResult refactorBatch(
        List<String> files,
        List<Recipe> recipes,
        RefactoringOptions options
    ) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("Files cannot be null or empty");
        }
        if (recipes == null || recipes.isEmpty()) {
            throw new IllegalArgumentException("Recipes cannot be null or empty");
        }
        if (options == null) {
            throw new IllegalArgumentException("Options cannot be null");
        }
        
        log.info("Refactoring batch of {} files with {} recipes", files.size(), recipes.size());
        
        Path projectPath = options.projectPath();
        List<String> refactoredFiles = new ArrayList<>();
        List<RefactoringFailure> failures = new ArrayList<>();
        String checkpointId = UUID.randomUUID().toString();
        boolean canRollback = options.createCheckpoints();
        
        // Filter out excluded files
        List<String> filesToProcess = files.stream()
            .filter(file -> !options.excludedFiles().contains(file))
            .collect(Collectors.toList());
        
        for (String filePath : filesToProcess) {
            try {
                Path fullPath = projectPath.resolve(filePath);
                
                // Create checkpoint if requested
                if (options.createCheckpoints() && Files.exists(fullPath)) {
                    String originalContent = Files.readString(fullPath);
                    changeTracker.createCheckpoint(
                        filePath,
                        originalContent,
                        "Before refactoring batch " + checkpointId
                    );
                }
                
                // Skip if dry run
                if (options.dryRun()) {
                    log.debug("Dry run: Would refactor file: {}", filePath);
                    refactoredFiles.add(filePath);
                    continue;
                }
                
                // Apply refactoring
                RefactoringChanges changes = refactoringEngine.refactorFile(fullPath, recipes);
                
                if (changes.hasChanges()) {
                    // Write refactored content
                    Files.writeString(fullPath, changes.refactoredContent());
                    
                    // Validate if requested
                    if (options.validateAfterRefactoring()) {
                        ValidationResult validation = validateRefactoring(filePath, changes);
                    if (!validation.isSuccessful()) {
                        log.warn("Validation failed for file: {}", filePath);
                        failures.add(new RefactoringFailure(
                            filePath,
                            "VALIDATION_ERROR",
                            "Validation failed: " + validation.issues().stream()
                                .map(ValidationIssue::message)
                                .collect(Collectors.joining(", "))
                        ));
                        continue;
                    }
                    }
                    
                    refactoredFiles.add(filePath);
                    progressTracker.markFileRefactored(projectPath.toString(), filePath);
                } else {
                    log.debug("No changes needed for file: {}", filePath);
                    refactoredFiles.add(filePath);
                }
                
            } catch (Exception e) {
                log.error("Failed to refactor file: {}", filePath, e);
                failures.add(new RefactoringFailure(
                    filePath,
                    "REFACTORING_ERROR",
                    e.getMessage()
                ));
                progressTracker.markFileFailed(projectPath.toString(), filePath);
            }
        }
        
        // Create checkpoint entry
        if (canRollback && !refactoredFiles.isEmpty()) {
            Checkpoint checkpoint = new Checkpoint(
                checkpointId,
                String.join(", ", refactoredFiles),
                LocalDateTime.now(),
                "Batch refactoring checkpoint"
            );
            progressTracker.addCheckpoint(projectPath.toString(), checkpoint);
        }
        
        // Calculate statistics
        RefactoringStatistics statistics = new RefactoringStatistics(
            filesToProcess.size(),
            failures.size(),
            refactoredFiles.size()
        );
        
        return new RefactoringResult(
            refactoredFiles,
            failures,
            statistics,
            checkpointId,
            canRollback
        );
    }
    
    @Override
    public MigrationProgress getProgress(String projectPath) {
        if (projectPath == null || projectPath.isBlank()) {
            throw new IllegalArgumentException("ProjectPath cannot be null or blank");
        }
        
        return progressTracker.getProgress(projectPath);
    }
    
    @Override
    public ValidationResult validateRefactoring(
        String filePath,
        RefactoringChanges changes
    ) {
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("FilePath cannot be null or blank");
        }
        if (changes == null) {
            throw new IllegalArgumentException("Changes cannot be null");
        }
        
        List<ValidationIssue> issues = new ArrayList<>();
        
        // Basic validation: check if content is valid
        String refactoredContent = changes.refactoredContent();
        
        // Check for common issues
        if (refactoredContent.contains("javax.servlet") && !refactoredContent.contains("jakarta.servlet")) {
            issues.add(new ValidationIssue(
                0,
                "Found javax.servlet reference that should be jakarta.servlet",
                ValidationSeverity.ERROR,
                "Replace javax.servlet with jakarta.servlet"
            ));
        }
        
        // Check for mixed namespaces (both javax and jakarta)
        boolean hasJavax = refactoredContent.contains("javax.");
        boolean hasJakarta = refactoredContent.contains("jakarta.");
        if (hasJavax && hasJakarta && filePath.endsWith(".java")) {
            // This might be intentional (transitional), so it's just a warning
            issues.add(new ValidationIssue(
                0,
                "File contains both javax and jakarta references",
                ValidationSeverity.WARNING,
                "Ensure this is intentional for transitional code"
            ));
        }
        
        ValidationStatus status;
        if (issues.isEmpty()) {
            status = ValidationStatus.PASSED;
        } else if (issues.stream().anyMatch(i -> i.severity() == ValidationSeverity.CRITICAL || i.severity() == ValidationSeverity.ERROR)) {
            status = ValidationStatus.FAILED;
        } else {
            status = ValidationStatus.PASSED_WITH_WARNINGS;
        }
        
        return new ValidationResult(
            issues.isEmpty() || status == ValidationStatus.PASSED_WITH_WARNINGS,
            issues,
            filePath,
            status
        );
    }
    
    @Override
    public RollbackResult rollback(String filePath, String checkpointId) {
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("FilePath cannot be null or blank");
        }
        if (checkpointId == null || checkpointId.isBlank()) {
            throw new IllegalArgumentException("CheckpointId cannot be null or blank");
        }
        
        log.info("Rolling back file: {} to checkpoint: {}", filePath, checkpointId);
        
        Optional<String> originalContent = changeTracker.getOriginalContent(checkpointId);
        if (originalContent.isEmpty()) {
            return RollbackResult.failure(
                filePath,
                checkpointId,
                "Checkpoint not found: " + checkpointId
            );
        }
        
        try {
            // Find the project path from the checkpoint
            Optional<Checkpoint> checkpoint = changeTracker.getCheckpoint(checkpointId);
            if (checkpoint.isEmpty()) {
                return RollbackResult.failure(
                    filePath,
                    checkpointId,
                    "Checkpoint metadata not found"
                );
            }
            
            // For now, we need the project path - this is a limitation we can improve
            // In a real implementation, we'd store the full path in the checkpoint
            log.warn("Rollback requires project path - this is a simplified implementation");
            
            return RollbackResult.failure(
                filePath,
                checkpointId,
                "Rollback requires project path - not fully implemented yet"
            );
            
        } catch (Exception e) {
            log.error("Failed to rollback file: {}", filePath, e);
            return RollbackResult.failure(
                filePath,
                checkpointId,
                "Rollback failed: " + e.getMessage()
            );
        }
    }
}

