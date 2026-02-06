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

import java.time.Duration;

/**
 * Metrics for verification execution.
 * 
 * NOTE: This is a community stub. Full implementation with detailed metrics
 * is available in the premium edition.
 */
public record ExecutionMetrics(
    Duration executionTime,
    long memoryUsedBytes,
    int exitCode,
    boolean timedOut
) {
    public ExecutionMetrics {
        if (executionTime == null) {
            executionTime = Duration.ZERO;
        }
        if (memoryUsedBytes < 0) {
            memoryUsedBytes = 0;
        }
    }
    
    public static ExecutionMetrics empty() {
        return new ExecutionMetrics(Duration.ZERO, 0, 0, false);
    }
}
