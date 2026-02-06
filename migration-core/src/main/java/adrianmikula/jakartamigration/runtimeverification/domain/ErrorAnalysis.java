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

import java.time.Instant;

/**
 * Analysis of a verification error.
 * 
 * NOTE: This is a community stub. Full implementation with detailed error
 * analysis and suggestions is available in the premium edition.
 */
public record ErrorAnalysis(
    String errorMessage,
    ErrorCategory category,
    String stackTrace,
    String suggestedFix,
    Instant timestamp
) {
    public ErrorAnalysis {
        if (category == null) {
            category = ErrorCategory.UNKNOWN;
        }
        if (timestamp == null) {
            timestamp = Instant.now();
        }
        if (errorMessage == null) {
            errorMessage = "Unknown error";
        }
    }
    
    public static ErrorAnalysis of(String message, ErrorCategory category) {
        return new ErrorAnalysis(message, category, null, null, Instant.now());
    }
    
    public static ErrorAnalysis unknown(String message) {
        return new ErrorAnalysis(message, ErrorCategory.UNKNOWN, null, null, Instant.now());
    }
}
