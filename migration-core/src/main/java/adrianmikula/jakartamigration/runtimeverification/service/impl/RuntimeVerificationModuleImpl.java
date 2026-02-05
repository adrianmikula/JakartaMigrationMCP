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
package adrianmikula.jakartamigration.runtimeverification.service.impl;

import adrianmikula.jakartamigration.runtimeverification.domain.VerificationOptions;
import adrianmikula.jakartamigration.runtimeverification.domain.VerificationResult;
import adrianmikula.jakartamigration.runtimeverification.service.RuntimeVerificationModule;

import java.nio.file.Path;

/**
 * Implementation of RuntimeVerificationModule.
 * 
 * NOTE: This is a community stub. Full implementation with ASM bytecode analysis
 * is available in the premium edition.
 */
public class RuntimeVerificationModuleImpl implements RuntimeVerificationModule {
    
    public RuntimeVerificationModuleImpl() {
        // Premium feature - no-op in community edition
    }
    
    @Override
    public VerificationResult verifyProject(Path projectPath, VerificationOptions options) {
        // Premium feature - returns empty result in community edition
        return VerificationResult.empty();
    }
    
    @Override
    public VerificationResult verifyRuntime(Path jarPath, VerificationOptions options) {
        // Premium feature - returns empty result in community edition
        return VerificationResult.empty();
    }
    
    @Override
    public boolean checkJarForjavaxReferences(Path jarPath) {
        // Premium feature - returns false in community edition
        return false;
    }
    
    @Override
    public String getModuleName() {
        return "Runtime Verification (Community Stub)";
    }
}
