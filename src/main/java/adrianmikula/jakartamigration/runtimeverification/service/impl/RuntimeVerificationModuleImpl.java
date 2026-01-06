package adrianmikula.jakartamigration.runtimeverification.service.impl;

import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.runtimeverification.domain.*;
import adrianmikula.jakartamigration.runtimeverification.service.BytecodeAnalyzer;
import adrianmikula.jakartamigration.runtimeverification.service.ErrorAnalyzer;
import adrianmikula.jakartamigration.runtimeverification.service.ProcessExecutor;
import adrianmikula.jakartamigration.runtimeverification.service.RuntimeVerificationModule;
import adrianmikula.jakartamigration.runtimeverification.service.impl.AsmBytecodeAnalyzer;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implementation of RuntimeVerificationModule.
 */
public class RuntimeVerificationModuleImpl implements RuntimeVerificationModule {
    
    private final ProcessExecutor processExecutor;
    private final ErrorAnalyzer errorAnalyzer;
    private final HttpClient httpClient;
    private final BytecodeAnalyzer bytecodeAnalyzer;
    
    public RuntimeVerificationModuleImpl() {
        this.processExecutor = new ProcessExecutor();
        this.errorAnalyzer = new ErrorAnalyzer();
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.bytecodeAnalyzer = new AsmBytecodeAnalyzer();
    }
    
    public RuntimeVerificationModuleImpl(
        ProcessExecutor processExecutor,
        ErrorAnalyzer errorAnalyzer,
        HttpClient httpClient,
        BytecodeAnalyzer bytecodeAnalyzer
    ) {
        this.processExecutor = processExecutor;
        this.errorAnalyzer = errorAnalyzer;
        this.httpClient = httpClient;
        this.bytecodeAnalyzer = bytecodeAnalyzer != null ? bytecodeAnalyzer : new AsmBytecodeAnalyzer();
    }
    
    @Override
    public VerificationResult verifyRuntime(Path jarPath, VerificationOptions options, VerificationStrategy strategy) {
        return switch (strategy) {
            case BYTECODE_ONLY -> verifyWithBytecode(jarPath);
            case PROCESS_ONLY -> verifyWithProcess(jarPath, options);
            case BYTECODE_THEN_PROCESS -> verifyBytecodeThenProcess(jarPath, options);
            case BOTH_PARALLEL -> verifyBothParallel(jarPath, options);
        };
    }
    
    @Override
    public VerificationResult verifyRuntime(Path jarPath, VerificationOptions options) {
        // Default to BYTECODE_THEN_PROCESS for backward compatibility with better performance
        return verifyRuntime(jarPath, options, VerificationStrategy.BYTECODE_THEN_PROCESS);
    }
    
    @Override
    public BytecodeAnalysisResult analyzeBytecode(Path jarPath) {
        return bytecodeAnalyzer.analyzeJar(jarPath);
    }
    
    private VerificationResult verifyWithBytecode(Path jarPath) {
        BytecodeAnalysisResult bytecodeResult = bytecodeAnalyzer.analyzeJar(jarPath);
        
        // Convert bytecode analysis to VerificationResult
        List<RuntimeError> errors = new ArrayList<>(bytecodeResult.potentialErrors());
        List<Warning> warnings = new ArrayList<>(bytecodeResult.warnings());
        
        // Add warnings for javax classes found
        for (String javaxClass : bytecodeResult.javaxClasses()) {
            warnings.add(new Warning(
                "javax class found in bytecode: " + javaxClass,
                "JAVAX_CLASS_DETECTED",
                java.time.LocalDateTime.now(),
                0.8
            ));
        }
        
        VerificationStatus status = bytecodeResult.hasIssues() 
            ? VerificationStatus.PARTIAL 
            : VerificationStatus.SUCCESS;
        
        ExecutionMetrics metrics = new ExecutionMetrics(
            Duration.ofMillis(bytecodeResult.analysisTimeMs()),
            0,
            0,
            false
        );
        
        MigrationContext context = new MigrationContext(
            new DependencyGraph(),
            "BYTECODE_ANALYSIS",
            false
        );
        
        ErrorAnalysis analysis = errorAnalyzer.analyzeErrors(errors, context);
        
        return new VerificationResult(
            status,
            errors,
            warnings,
            metrics,
            analysis,
            analysis.suggestedFixes()
        );
    }
    
    private VerificationResult verifyWithProcess(Path jarPath, VerificationOptions options) {
        // Original process execution implementation
        if (!Files.exists(jarPath)) {
            return createErrorResult(
                VerificationStatus.FAILED,
                "JAR file does not exist: " + jarPath,
                options
            );
        }
        
        if (!Files.isRegularFile(jarPath)) {
            return createErrorResult(
                VerificationStatus.FAILED,
                "Path is not a file: " + jarPath,
                options
            );
        }
        
        // Execute the JAR file
        ProcessExecutor.ProcessExecutionResult executionResult = processExecutor.execute(jarPath, options);
        
        // Parse errors from output
        List<RuntimeError> errors = errorAnalyzer.parseErrorsFromOutput(
            executionResult.stderr(),
            executionResult.stdout()
        );
        
        // Determine status
        VerificationStatus status = determineStatus(executionResult, errors);
        
        // Generate warnings from stdout/stderr
        List<Warning> warnings = generateWarnings(executionResult);
        
        // Analyze errors
        MigrationContext context = new MigrationContext(
            new DependencyGraph(),
            "RUNTIME_VERIFICATION",
            false
        );
        
        ErrorAnalysis analysis = errorAnalyzer.analyzeErrors(errors, context);
        
        // Generate remediation steps
        List<RemediationStep> remediationSteps = analysis.suggestedFixes();
        
        return new VerificationResult(
            status,
            errors,
            warnings,
            executionResult.metrics(),
            analysis,
            remediationSteps
        );
    }
    
    private VerificationResult verifyBytecodeThenProcess(Path jarPath, VerificationOptions options) {
        // First, do fast bytecode analysis
        BytecodeAnalysisResult bytecodeResult = bytecodeAnalyzer.analyzeJar(jarPath);
        
        // If bytecode analysis found issues, do process execution for comprehensive check
        if (bytecodeResult.hasIssues()) {
            VerificationResult processResult = verifyWithProcess(jarPath, options);
            
            // Merge results - combine errors and warnings
            List<RuntimeError> combinedErrors = new ArrayList<>(bytecodeResult.potentialErrors());
            combinedErrors.addAll(processResult.errors());
            
            List<Warning> combinedWarnings = new ArrayList<>(bytecodeResult.warnings());
            combinedWarnings.addAll(processResult.warnings());
            
            // Use process execution metrics (more accurate)
            MigrationContext context = new MigrationContext(
                new DependencyGraph(),
                "BYTECODE_THEN_PROCESS",
                false
            );
            
            ErrorAnalysis analysis = errorAnalyzer.analyzeErrors(combinedErrors, context);
            
            return new VerificationResult(
                processResult.status(),
                combinedErrors,
                combinedWarnings,
                processResult.metrics(),
                analysis,
                analysis.suggestedFixes()
            );
        }
        
        // No issues found in bytecode, return bytecode result
        return verifyWithBytecode(jarPath);
    }
    
    private VerificationResult verifyBothParallel(Path jarPath, VerificationOptions options) {
        // For now, run sequentially (can be optimized with CompletableFuture later)
        VerificationResult bytecodeResult = verifyWithBytecode(jarPath);
        VerificationResult processResult = verifyWithProcess(jarPath, options);
        
        // Merge results
        List<RuntimeError> combinedErrors = new ArrayList<>(bytecodeResult.errors());
        combinedErrors.addAll(processResult.errors());
        
        List<Warning> combinedWarnings = new ArrayList<>(bytecodeResult.warnings());
        combinedWarnings.addAll(processResult.warnings());
        
        // Use the more comprehensive status
        VerificationStatus status = processResult.status() == VerificationStatus.FAILED 
            ? VerificationStatus.FAILED 
            : bytecodeResult.status();
        
        // Combine metrics
        ExecutionMetrics combinedMetrics = new ExecutionMetrics(
            bytecodeResult.metrics().executionTime().plus(processResult.metrics().executionTime()),
            Math.max(bytecodeResult.metrics().memoryUsedBytes(), processResult.metrics().memoryUsedBytes()),
            processResult.metrics().exitCode(),
            processResult.metrics().timedOut()
        );
        
        MigrationContext context = new MigrationContext(
            new DependencyGraph(),
            "BOTH_PARALLEL",
            false
        );
        
        ErrorAnalysis analysis = errorAnalyzer.analyzeErrors(combinedErrors, context);
        
        return new VerificationResult(
            status,
            combinedErrors,
            combinedWarnings,
            combinedMetrics,
            analysis,
            analysis.suggestedFixes()
        );
    }
    
    @Override
    public ErrorAnalysis analyzeErrors(List<RuntimeError> errors, MigrationContext context) {
        return errorAnalyzer.analyzeErrors(errors, context);
    }
    
    @Override
    public StaticAnalysisResult performStaticAnalysis(Path projectPath, DependencyGraph dependencyGraph) {
        if (!Files.exists(projectPath)) {
            return new StaticAnalysisResult(
                true,
                Collections.emptyList(),
                List.of(new Warning(
                    "Project path does not exist: " + projectPath,
                    "PATH_ERROR",
                    java.time.LocalDateTime.now(),
                    1.0
                )),
                dependencyGraph,
                "Static analysis failed: project path does not exist"
            );
        }
        
        // Perform basic static analysis
        List<RuntimeError> potentialErrors = new ArrayList<>();
        List<Warning> warnings = new ArrayList<>();
        
        // Check for common Jakarta migration issues in source files
        try {
            Files.walk(projectPath)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".java"))
                .forEach(javaFile -> {
                    analyzeJavaFile(javaFile, potentialErrors, warnings);
                });
        } catch (Exception e) {
            warnings.add(new Warning(
                "Error during static analysis: " + e.getMessage(),
                "ANALYSIS_ERROR",
                java.time.LocalDateTime.now(),
                0.5
            ));
        }
        
        boolean hasIssues = !potentialErrors.isEmpty() || !warnings.isEmpty();
        String summary = String.format(
            "Static analysis completed. Found %d potential errors and %d warnings.",
            potentialErrors.size(),
            warnings.size()
        );
        
        return new StaticAnalysisResult(
            hasIssues,
            potentialErrors,
            warnings,
            dependencyGraph,
            summary
        );
    }
    
    @Override
    public ClassLoaderAnalysisResult instrumentClassLoading(Path jarPath, InstrumentationOptions options) {
        // This is a placeholder implementation
        // In a real implementation, this would use a Java agent to instrument class loading
        return new ClassLoaderAnalysisResult(
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            "Class loader instrumentation not yet implemented. Use verifyRuntime() for runtime verification."
        );
    }
    
    @Override
    public HealthCheckResult performHealthCheck(String applicationUrl, HealthCheckOptions options) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(java.net.URI.create(applicationUrl + options.healthEndpoint()))
                .timeout(options.timeout())
                .GET()
                .build();
            
            Instant startTime = Instant.now();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            Duration responseTime = Duration.between(startTime, Instant.now());
            
            boolean healthy = response.statusCode() == options.expectedStatusCode();
            List<String> issues = new ArrayList<>();
            
            if (!healthy) {
                issues.add(String.format(
                    "Expected status code %d but got %d",
                    options.expectedStatusCode(),
                    response.statusCode()
                ));
            }
            
            // Check response body for health indicators
            String body = response.body();
            if (body != null && body.toLowerCase().contains("\"status\":\"down\"")) {
                healthy = false;
                issues.add("Health check endpoint reports status: down");
            }
            
            return new HealthCheckResult(
                healthy,
                response.statusCode(),
                responseTime,
                body,
                issues
            );
            
        } catch (Exception e) {
            return new HealthCheckResult(
                false,
                0,
                Duration.ZERO,
                "",
                List.of("Health check failed: " + e.getMessage())
            );
        }
    }
    
    private VerificationStatus determineStatus(
        ProcessExecutor.ProcessExecutionResult executionResult,
        List<RuntimeError> errors
    ) {
        if (executionResult.metrics().timedOut()) {
            return VerificationStatus.TIMEOUT;
        }
        
        if (executionResult.metrics().exitCode() != 0) {
            if (!errors.isEmpty()) {
                return VerificationStatus.FAILED;
            }
            return VerificationStatus.PARTIAL;
        }
        
        if (!errors.isEmpty()) {
            return VerificationStatus.PARTIAL;
        }
        
        return VerificationStatus.SUCCESS;
    }
    
    private List<Warning> generateWarnings(ProcessExecutor.ProcessExecutionResult executionResult) {
        List<Warning> warnings = new ArrayList<>();
        
        // Check stderr for warnings
        for (String line : executionResult.stderr()) {
            String lowerLine = line.toLowerCase();
            if (lowerLine.contains("warning") || lowerLine.contains("deprecated")) {
                warnings.add(new Warning(
                    line,
                    "RUNTIME_WARNING",
                    java.time.LocalDateTime.now(),
                    0.5
                ));
            }
        }
        
        return warnings;
    }
    
    private VerificationResult createErrorResult(
        VerificationStatus status,
        String errorMessage,
        VerificationOptions options
    ) {
        RuntimeError error = new RuntimeError(
            ErrorType.OTHER,
            errorMessage,
            new StackTrace("RuntimeVerificationModule", errorMessage, Collections.emptyList()),
            "RuntimeVerificationModule",
            "verifyRuntime",
            java.time.LocalDateTime.now(),
            1.0
        );
        
        ExecutionMetrics metrics = new ExecutionMetrics(
            Duration.ZERO,
            0,
            -1,
            false
        );
        
        ErrorAnalysis analysis = new ErrorAnalysis(
            ErrorCategory.UNKNOWN,
            errorMessage,
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            1.0
        );
        
        return new VerificationResult(
            status,
            List.of(error),
            Collections.emptyList(),
            metrics,
            analysis,
            Collections.emptyList()
        );
    }
    
    private void analyzeJavaFile(Path javaFile, List<RuntimeError> potentialErrors, List<Warning> warnings) {
        try {
            String content = Files.readString(javaFile);
            
            // Check for javax imports that should be jakarta
            if (content.contains("import javax.servlet") ||
                content.contains("import javax.persistence") ||
                content.contains("import javax.ejb") ||
                content.contains("import javax.validation")) {
                
                warnings.add(new Warning(
                    String.format("File %s contains javax imports that may need migration", javaFile.getFileName()),
                    "JAVAX_IMPORT",
                    java.time.LocalDateTime.now(),
                    0.7
                ));
            }
            
            // Check for mixed namespaces
            boolean hasJavax = content.contains("javax.servlet") || content.contains("javax.persistence");
            boolean hasJakarta = content.contains("jakarta.servlet") || content.contains("jakarta.persistence");
            
            if (hasJavax && hasJakarta) {
                potentialErrors.add(new RuntimeError(
                    ErrorType.LINKAGE_ERROR,
                    String.format("Mixed javax and jakarta namespaces in %s", javaFile.getFileName()),
                    new StackTrace("StaticAnalysis", "Mixed namespaces", Collections.emptyList()),
                    javaFile.getFileName().toString(),
                    "static_analysis",
                    java.time.LocalDateTime.now(),
                    0.8
                ));
            }
            
        } catch (Exception e) {
            warnings.add(new Warning(
                "Error analyzing file " + javaFile + ": " + e.getMessage(),
                "ANALYSIS_ERROR",
                java.time.LocalDateTime.now(),
                0.3
            ));
        }
    }
}

