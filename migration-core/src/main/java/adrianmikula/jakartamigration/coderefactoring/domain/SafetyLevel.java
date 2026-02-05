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

/**
 * Safety level for refactoring recipes.
 * 
 * NOTE: This is a community stub. Full implementation with OpenRewrite recipes
 * is available in the premium edition.
 */
public enum SafetyLevel {
    HIGH,      // Safe, well-tested recipe
    MEDIUM,    // Generally safe but may have edge cases
    LOW        // Risky, requires careful review
}
