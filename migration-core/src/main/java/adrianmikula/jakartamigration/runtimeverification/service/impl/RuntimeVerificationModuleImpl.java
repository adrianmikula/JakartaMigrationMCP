/*
 * Copyright 2024 Adrian Kozak
 * Copyright 2024 Prairie Trail Software
 *
 * This software is proprietary and may not be used, copied, modified,
 * or distributed except under the terms of a separate commercial license agreement.
 */
package adrianmikula.jakartamigration.runtimeverification.service.impl;

import adrianmikula.jakartamigration.runtimeverification.domain.VerificationOptions;
import adrianmikula.jakartamigration.runtimeverification.domain.VerificationResult;
import adrianmikula.jakartamigration.runtimeverification.service.RuntimeVerificationModule;

import java.nio.file.Path;

/**
 * Implementation of RuntimeVerificationModule.
 * 
 * NOTE: This is a stub. Full implementation with ASM bytecode analysis
 * is available in the premium edition.
 */
public class RuntimeVerificationModuleImpl implements RuntimeVerificationModule {
    
    public RuntimeVerificationModuleImpl() {
        // Premium feature - no-op in stub implementation
    }
    
    @Override
    public VerificationResult verifyProject(Path projectPath, VerificationOptions options) {
        // Premium feature - returns empty result in stub implementation
        return VerificationResult.empty();
    }
    
    @Override
    public VerificationResult verifyRuntime(Path jarPath, VerificationOptions options) {
        // Premium feature - returns empty result in stub implementation
        return VerificationResult.empty();
    }
    
    @Override
    public boolean checkJarForjavaxReferences(Path jarPath) {
        // Premium feature - returns false in stub implementation
        return false;
    }
    
    @Override
    public String getModuleName() {
        return "Runtime Verification (Stub)";
    }
}
