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
package adrianmikula.jakartamigration.coderefactoring.domain;

import adrianmikula.jakartamigration.dependencyanalysis.domain.RiskAssessment;

import java.time.Duration;
import java.util.List;

/**
 * Complete migration plan with phases and execution strategy.
 * 
 * NOTE: This is a community stub. Full implementation with persistence
 * is available in the premium edition.
 */
public record MigrationPlan(
    List<RefactoringPhase> phases,
    List<String> fileSequence,
    Duration estimatedDuration,
    RiskAssessment overallRisk,
    List<String> prerequisites
) {
    public MigrationPlan {
        if (phases == null || phases.isEmpty()) {
            throw new IllegalArgumentException("Phases cannot be null or empty");
        }
        if (fileSequence == null) {
            fileSequence = List.of();
        }
        if (estimatedDuration == null || estimatedDuration.isNegative()) {
            estimatedDuration = Duration.ZERO;
        }
        if (overallRisk == null) {
            throw new IllegalArgumentException("OverallRisk cannot be null");
        }
        if (prerequisites == null) {
            prerequisites = List.of();
        }
    }
    
    /**
     * Returns the total number of files to be refactored.
     */
    public int totalFileCount() {
        return fileSequence.size();
    }
    
    /**
     * Returns the number of phases in the plan.
     */
    public int phaseCount() {
        return phases.size();
    }
}
