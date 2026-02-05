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

import java.time.Duration;
import java.util.List;

/**
 * Represents a single phase in the migration plan.
 * 
 * NOTE: This is a community stub. Full implementation with migration actions
 * is available in the premium edition.
 */
public record RefactoringPhase(
    int phaseNumber,
    String description,
    List<String> files,
    List<PhaseAction> actions,
    List<String> recipes,
    List<String> dependencies,
    Duration estimatedDuration
) {
    public RefactoringPhase {
        if (phaseNumber < 1) {
            phaseNumber = 1;
        }
        if (description == null || description.isBlank()) {
            description = "Migration Phase";
        }
        if (files == null) {
            files = List.of();
        }
        if (actions == null) {
            actions = List.of();
        }
        if (recipes == null) {
            recipes = List.of();
        }
        if (dependencies == null) {
            dependencies = List.of();
        }
        if (estimatedDuration == null || estimatedDuration.isNegative()) {
            estimatedDuration = Duration.ZERO;
        }
    }
}
