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
package adrianmikula.jakartamigration.runtimeverification.domain;

import java.util.List;

/**
 * Result of runtime verification.
 * 
 * NOTE: This is a community stub. Full implementation with bytecode analysis
 * is available in the premium edition.
 */
public record VerificationResult(
    boolean success,
    List<String> issues,
    List<String> warnings,
    List<String> info,
    int jakartaReferences,
    int javaxReferences
) {
    public VerificationResult {
        if (issues == null) {
            issues = List.of();
        }
        if (warnings == null) {
            warnings = List.of();
        }
        if (info == null) {
            info = List.of();
        }
    }
    
    public static VerificationResult empty() {
        return new VerificationResult(true, List.of(), List.of(), List.of(), 0, 0);
    }
    
    public boolean hasIssues() {
        return !issues.isEmpty();
    }
    
    /**
     * Returns the status of the verification.
     */
    public String status() {
        return success ? "VERIFIED" : "ISSUES_FOUND";
    }
    
    /**
     * Returns the list of errors (issues).
     */
    public List<String> errors() {
        return issues;
    }
}
