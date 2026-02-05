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

import adrianmikula.jakartamigration.coderefactoring.domain.MigrationPlan;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyAnalysisReport;
import adrianmikula.jakartamigration.sourcecodescanning.service.SourceCodeScanner;

import java.nio.file.Path;

/**
 * Service for creating migration plans.
 * 
 * NOTE: This is a community stub. Full implementation with OpenRewrite-based
 * planning is available in the premium edition.
 */
public class MigrationPlanner {
    
    private final SourceCodeScanner sourceCodeScanner;
    
    public MigrationPlanner(SourceCodeScanner sourceCodeScanner) {
        this.sourceCodeScanner = sourceCodeScanner;
    }
    
    /**
     * Creates a migration plan for the given project.
     * 
     * @param projectPath Path to the project
     * @param report Dependency analysis report
     * @return Migration plan or null if premium features not available
     */
    public MigrationPlan createPlan(Path projectPath, DependencyAnalysisReport report) {
        // Premium feature - returns null in community edition
        return null;
    }
    
    /**
     * Creates a migration plan for the given project.
     * 
     * @param projectPath Path to the project to migrate
     * @param sourceCodeScanner Scanner to analyze source code
     * @param recipes List of recipes to apply
     * @return Migration plan or null if premium features not available
     */
    public MigrationPlan createMigrationPlan(
            Path projectPath,
            SourceCodeScanner scanner,
            java.util.List<String> recipes) {
        // Premium feature - returns null in community edition
        return null;
    }
    
    /**
     * Estimates the duration of a migration.
     * 
     * @param projectPath Path to the project
     * @return Estimated duration in seconds, or 0 for community edition
     */
    public long estimateMigrationDuration(Path projectPath) {
        return 0;
    }
}
