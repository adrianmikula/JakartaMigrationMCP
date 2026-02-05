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

/**
 * Tracks progress of refactoring operations.
 * 
 * NOTE: This is a community stub. Full implementation with detailed progress
 * tracking is available in the premium edition.
 */
public class ProgressTracker {
    
    /**
     * Gets the current progress percentage.
     * 
     * @return Progress (0-100)
     */
    public int getProgress() {
        return 0;
    }
    
    /**
     * Gets the current status message.
     * 
     * @return Status message
     */
    public String getStatus() {
        return "No active migration";
    }
    
    /**
     * Gets the number of files processed.
     * 
     * @return Number of processed files
     */
    public int getFilesProcessed() {
        return 0;
    }
    
    /**
     * Gets the total number of files to process.
     * 
     * @return Total files
     */
    public int getTotalFiles() {
        return 0;
    }
}
