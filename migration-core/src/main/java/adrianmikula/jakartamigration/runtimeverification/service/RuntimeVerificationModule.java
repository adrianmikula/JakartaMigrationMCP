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
package adrianmikula.jakartamigration.runtimeverification.service;

import adrianmikula.jakartamigration.runtimeverification.domain.VerificationOptions;
import adrianmikula.jakartamigration.runtimeverification.domain.VerificationResult;

import java.nio.file.Path;

/**
 * Module for runtime verification of migrated applications.
 * 
 * NOTE: This is a community stub. Full implementation with bytecode analysis
 * using ASM is available in the premium edition.
 */
public interface RuntimeVerificationModule {
    
    /**
     * Verifies a project for javax/jakarta references.
     * 
     * @param projectPath Path to the project
     * @param options Verification options
     * @return Verification result
     */
    VerificationResult verifyProject(Path projectPath, VerificationOptions options);
    
    /**
     * Verifies a JAR file for javax/jakarta references.
     * 
     * @param jarPath Path to the JAR file
     * @param options Verification options
     * @return Verification result
     */
    VerificationResult verifyRuntime(Path jarPath, VerificationOptions options);
    
    /**
     * Checks if a JAR file contains javax references.
     * 
     * @param jarPath Path to the JAR file
     * @return true if javax references are found
     */
    boolean checkJarForjavaxReferences(Path jarPath);
    
    /**
     * Gets the module name.
     * 
     * @return Module name
     */
    String getModuleName();
}
