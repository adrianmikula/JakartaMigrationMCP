/*
 * Copyright 2024 Adrian Kozak
 * Copyright 2024 Prairie Trail Software
 *
 * This software is proprietary and may not be used, copied, modified,
 * or distributed except under the terms of a separate commercial license agreement.
 */
package adrianmikula.jakartamigration.coderefactoring.service;

import adrianmikula.jakartamigration.coderefactoring.domain.Recipe;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Engine for executing refactoring operations.
 * 
 * NOTE: This is a stub. Full implementation with OpenRewrite-based
 * refactoring is available in the premium edition.
 */
public class RefactoringEngine {
    
    /**
     * Executes refactoring on a list of files.
     * 
     * @param files List of files to refactor
     * @param recipes Recipes to apply
     * @param dryRun If true, only preview changes
     * @return Future that completes with the number of changed files
     */
    public CompletableFuture<Integer> refactorFiles(
            List<Path> files,
            List<Recipe> recipes,
            boolean dryRun) {
        return CompletableFuture.completedFuture(0);
    }
    
    /**
     * Validates that refactoring can be applied to a file.
     * 
     * @param filePath Path to validate
     * @return true if refactoring can be applied
     */
    public boolean validateRefactoring(Path filePath) {
        return false;
    }
    
    /**
     * Gets the progress of current refactoring operations.
     * 
     * @return Progress percentage (0-100)
     */
    public int getProgress() {
        return 0;
    }
}
