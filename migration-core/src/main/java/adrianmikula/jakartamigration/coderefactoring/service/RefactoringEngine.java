/*
 * Copyright 2024 Adrian Kozak
 * Copyright 2024 Prairie Trail Software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package adrianmikula.jakartamigration.coderefactoring.service;

import adrianmikula.jakartamigration.coderefactoring.domain.Recipe;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Engine for executing refactoring operations.
 * 
 * NOTE: This is a community stub. Full implementation with OpenRewrite-based
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
