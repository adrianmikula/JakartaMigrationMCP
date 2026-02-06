/*
 * Copyright 2024 Adrian Kozak
 * Copyright 2024 Prairie Trail Software
 *
 * This software is proprietary and may not be used, copied, modified,
 * or distributed except under the terms of a separate commercial license agreement.
 */
package adrianmikula.jakartamigration.runtimeverification.service;

import adrianmikula.jakartamigration.runtimeverification.domain.VerificationOptions;
import adrianmikula.jakartamigration.runtimeverification.domain.VerificationResult;

import java.nio.file.Path;

/**
 * Module for runtime verification of migrated applications.
 * 
 * NOTE: This is a stub. Full implementation with bytecode analysis
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
