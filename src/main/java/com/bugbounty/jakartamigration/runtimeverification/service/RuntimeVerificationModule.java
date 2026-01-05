package com.bugbounty.jakartamigration.runtimeverification.service;

import com.bugbounty.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import com.bugbounty.jakartamigration.runtimeverification.domain.*;

import java.nio.file.Path;
import java.util.List;

/**
 * Main interface for the Runtime Verification Module.
 * Executes JAR files and monitors for runtime class resolution issues,
 * particularly those caused by javax/jakarta namespace problems.
 */
public interface RuntimeVerificationModule {
    
    /**
     * Executes JAR in isolated process and monitors for issues.
     *
     * @param jarPath Path to the JAR file to execute
     * @param options Verification options
     * @return Verification result with errors, warnings, and analysis
     */
    VerificationResult verifyRuntime(
        Path jarPath,
        VerificationOptions options
    );
    
    /**
     * Analyzes runtime errors for Jakarta migration issues.
     *
     * @param errors List of runtime errors to analyze
     * @param context Migration context information
     * @return Error analysis with root cause and remediation suggestions
     */
    ErrorAnalysis analyzeErrors(
        List<RuntimeError> errors,
        MigrationContext context
    );
    
    /**
     * Performs static analysis as alternative to runtime execution.
     *
     * @param projectPath Path to the project root
     * @param dependencyGraph Dependency graph of the project
     * @return Static analysis result
     */
    StaticAnalysisResult performStaticAnalysis(
        Path projectPath,
        DependencyGraph dependencyGraph
    );
    
    /**
     * Instruments class loading to detect resolution issues.
     *
     * @param jarPath Path to the JAR file
     * @param options Instrumentation options
     * @return Class loader analysis result
     */
    ClassLoaderAnalysisResult instrumentClassLoading(
        Path jarPath,
        InstrumentationOptions options
    );
    
    /**
     * Validates application health after migration.
     *
     * @param applicationUrl URL of the application to check
     * @param options Health check options
     * @return Health check result
     */
    HealthCheckResult performHealthCheck(
        String applicationUrl,
        HealthCheckOptions options
    );
}

