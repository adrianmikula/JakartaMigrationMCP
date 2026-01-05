package com.bugbounty.jakartamigration.coderefactoring.service;

import com.bugbounty.jakartamigration.coderefactoring.domain.*;
import com.bugbounty.jakartamigration.dependencyanalysis.domain.DependencyAnalysisReport;

import java.nio.file.Path;
import java.util.List;

/**
 * Main interface for the Code Refactoring Module.
 * Systematically refactors code from javax to jakarta using OpenRewrite rules,
 * with intelligent ordering, progress tracking, and incremental application.
 */
public interface CodeRefactoringModule {
    
    /**
     * Creates a migration plan with optimal refactoring order.
     *
     * @param projectPath Path to the project root
     * @param dependencyReport Dependency analysis report
     * @return Migration plan with phases and execution strategy
     */
    MigrationPlan createMigrationPlan(
        String projectPath,
        DependencyAnalysisReport dependencyReport
    );
    
    /**
     * Refactors a batch of files using OpenRewrite recipes.
     *
     * @param files List of file paths to refactor (relative to project root)
     * @param recipes List of recipes to apply
     * @param options Refactoring options
     * @return Refactoring result with success/failure information
     */
    RefactoringResult refactorBatch(
        List<String> files,
        List<Recipe> recipes,
        RefactoringOptions options
    );
    
    /**
     * Tracks migration progress across the codebase.
     *
     * @param projectPath Path to the project root
     * @return Current migration progress
     */
    MigrationProgress getProgress(String projectPath);
    
    /**
     * Validates refactored code for correctness.
     *
     * @param filePath Path to the refactored file
     * @param changes Refactoring changes that were applied
     * @return Validation result
     */
    ValidationResult validateRefactoring(
        String filePath,
        RefactoringChanges changes
    );
    
    /**
     * Rolls back refactoring changes if needed.
     *
     * @param filePath Path to the file to rollback
     * @param checkpointId Checkpoint ID to rollback to
     * @return Rollback result
     */
    RollbackResult rollback(
        String filePath,
        String checkpointId
    );
}

