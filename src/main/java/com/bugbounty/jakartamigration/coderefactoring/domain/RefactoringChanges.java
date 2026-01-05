package com.bugbounty.jakartamigration.coderefactoring.domain;

import java.util.List;

/**
 * Represents changes made during refactoring.
 */
public record RefactoringChanges(
    String filePath,
    String originalContent,
    String refactoredContent,
    List<ChangeDetail> changes,
    List<Recipe> appliedRecipes
) {
    public RefactoringChanges {
        if (filePath == null || filePath.isBlank()) {
            throw new IllegalArgumentException("FilePath cannot be null or blank");
        }
        if (originalContent == null) {
            throw new IllegalArgumentException("OriginalContent cannot be null");
        }
        if (refactoredContent == null) {
            throw new IllegalArgumentException("RefactoredContent cannot be null");
        }
        if (changes == null) {
            throw new IllegalArgumentException("Changes cannot be null");
        }
        if (appliedRecipes == null) {
            throw new IllegalArgumentException("AppliedRecipes cannot be null");
        }
    }
    
    /**
     * Returns true if any changes were made.
     */
    public boolean hasChanges() {
        return !originalContent.equals(refactoredContent) || !changes.isEmpty();
    }
    
    /**
     * Returns the number of changes made.
     */
    public int changeCount() {
        return changes.size();
    }
}

