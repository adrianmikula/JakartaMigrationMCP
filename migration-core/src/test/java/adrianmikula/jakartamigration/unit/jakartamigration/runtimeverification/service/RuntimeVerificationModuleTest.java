package unit.jakartamigration.runtimeverification.service;

import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.runtimeverification.domain.*;
import adrianmikula.jakartamigration.runtimeverification.service.RuntimeVerificationModule;
import adrianmikula.jakartamigration.runtimeverification.service.impl.RuntimeVerificationModuleImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RuntimeVerificationModule Tests")
class RuntimeVerificationModuleTest {
    
    private RuntimeVerificationModule module;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        module = new RuntimeVerificationModuleImpl();
    }
    
    @Test
    @DisplayName("Should verify runtime execution of JAR file")
    void shouldVerifyRuntimeExecution() throws Exception {
        // Given
        Path jarPath = tempDir.resolve("test.jar");
        createValidJarFile(jarPath);
        
        VerificationOptions options = VerificationOptions.defaults();
        
        // When
        VerificationResult result = module.verifyRuntime(jarPath, options);
        
        // Then
        assertNotNull(result);
        assertNotNull(result.status());
        assertNotNull(result.errors());
        assertNotNull(result.warnings());
        assertNotNull(result.metrics());
    }
    
    @Test
    @DisplayName("Should verify using bytecode analysis strategy")
    void shouldVerifyWithBytecodeStrategy() throws Exception {
        // Given
        Path jarPath = tempDir.resolve("test.jar");
        createValidJarFile(jarPath);
        
        VerificationOptions options = VerificationOptions.defaults();
        
        // When
        VerificationResult result = module.verifyRuntime(
            jarPath, 
            options, 
            VerificationStrategy.BYTECODE_ONLY
        );
        
        // Then
        assertNotNull(result);
        assertNotNull(result.status());
        // Bytecode analysis should be fast
        assertTrue(result.metrics().executionTime().toMillis() < 5000);
    }
    
    @Test
    @DisplayName("Should verify using process execution strategy")
    void shouldVerifyWithProcessStrategy() throws Exception {
        // Given
        Path jarPath = tempDir.resolve("test.jar");
        createValidJarFile(jarPath);
        
        VerificationOptions options = VerificationOptions.defaults();
        
        // When
        VerificationResult result = module.verifyRuntime(
            jarPath, 
            options, 
            VerificationStrategy.PROCESS_ONLY
        );
        
        // Then
        assertNotNull(result);
        assertNotNull(result.status());
    }
    
    @Test
    @DisplayName("Should analyze bytecode directly")
    void shouldAnalyzeBytecode() throws Exception {
        // Given
        Path jarPath = tempDir.resolve("test.jar");
        createValidJarFile(jarPath);
        
        // When
        BytecodeAnalysisResult result = module.analyzeBytecode(jarPath);
        
        // Then
        assertNotNull(result);
        assertNotNull(result.javaxClasses());
        assertNotNull(result.jakartaClasses());
        assertNotNull(result.mixedNamespaceClasses());
        assertTrue(result.analysisTimeMs() >= 0);
        assertTrue(result.classesAnalyzed() >= 0);
    }
    
    @Test
    @DisplayName("Should analyze errors for Jakarta migration issues")
    void shouldAnalyzeErrors() {
        // Given
        StackTrace stackTrace = new StackTrace(
            "java.lang.ClassNotFoundException",
            "javax.servlet.ServletException",
            List.of(new StackTrace.StackTraceElement(
                "com.example.Test",
                "testMethod",
                "Test.java",
                42
            ))
        );
        
        RuntimeError error = new RuntimeError(
            ErrorType.CLASS_NOT_FOUND,
            "javax.servlet.ServletException not found",
            stackTrace,
            "javax.servlet.ServletException",
            "testMethod",
            LocalDateTime.now(),
            0.95
        );
        
        DependencyGraph graph = new DependencyGraph();
        MigrationContext context = new MigrationContext(
            graph,
            "POST_MIGRATION",
            true
        );
        
        // When
        ErrorAnalysis analysis = module.analyzeErrors(List.of(error), context);
        
        // Then
        assertNotNull(analysis);
        assertNotNull(analysis.category());
        assertNotNull(analysis.rootCause());
        assertNotNull(analysis.suggestedFixes());
    }
    
    @Test
    @DisplayName("Should perform static analysis")
    void shouldPerformStaticAnalysis() throws Exception {
        // Given
        Path projectPath = tempDir;
        Files.createDirectories(projectPath);
        
        DependencyGraph graph = new DependencyGraph();
        
        // When
        StaticAnalysisResult result = module.performStaticAnalysis(projectPath, graph);
        
        // Then
        assertNotNull(result);
        assertNotNull(result.potentialErrors());
        assertNotNull(result.warnings());
        assertNotNull(result.analyzedGraph());
        assertNotNull(result.analysisSummary());
    }
    
    @Test
    @DisplayName("Should instrument class loading")
    void shouldInstrumentClassLoading() throws Exception {
        // Given
        Path jarPath = tempDir.resolve("test.jar");
        createValidJarFile(jarPath);
        
        InstrumentationOptions options = InstrumentationOptions.defaults();
        
        // When
        ClassLoaderAnalysisResult result = module.instrumentClassLoading(jarPath, options);
        
        // Then
        assertNotNull(result);
        assertNotNull(result.loadedClasses());
        assertNotNull(result.failedLoads());
        assertNotNull(result.namespaceConflicts());
        assertNotNull(result.summary());
    }
    
    @Test
    @DisplayName("Should perform health check")
    void shouldPerformHealthCheck() {
        // Given
        String applicationUrl = "http://localhost:8080";
        HealthCheckOptions options = HealthCheckOptions.defaults();
        
        // When
        HealthCheckResult result = module.performHealthCheck(applicationUrl, options);
        
        // Then
        assertNotNull(result);
        assertNotNull(result.responseTime());
        assertNotNull(result.issues());
    }
    
    @Test
    @DisplayName("Should detect namespace migration errors")
    void shouldDetectNamespaceMigrationErrors() {
        // Given
        StackTrace stackTrace = new StackTrace(
            "java.lang.ClassNotFoundException",
            "javax.servlet.ServletException",
            List.of()
        );
        
        RuntimeError error = new RuntimeError(
            ErrorType.CLASS_NOT_FOUND,
            "javax.servlet.ServletException not found",
            stackTrace,
            "javax.servlet.ServletException",
            "testMethod",
            LocalDateTime.now(),
            0.95
        );
        
        DependencyGraph graph = new DependencyGraph();
        MigrationContext context = new MigrationContext(
            graph,
            "POST_MIGRATION",
            true
        );
        
        // When
        ErrorAnalysis analysis = module.analyzeErrors(List.of(error), context);
        
        // Then
        assertEquals(ErrorCategory.NAMESPACE_MIGRATION, analysis.category());
        assertFalse(analysis.suggestedFixes().isEmpty());
    }
    
    @Test
    @DisplayName("Should detect classpath issues")
    void shouldDetectClasspathIssues() {
        // Given
        StackTrace stackTrace = new StackTrace(
            "java.lang.NoClassDefFoundError",
            "jakarta.servlet.ServletException",
            List.of()
        );
        
        RuntimeError error = new RuntimeError(
            ErrorType.NO_CLASS_DEF_FOUND,
            "jakarta.servlet.ServletException not found in classpath",
            stackTrace,
            "jakarta.servlet.ServletException",
            "testMethod",
            LocalDateTime.now(),
            0.9
        );
        
        DependencyGraph graph = new DependencyGraph();
        MigrationContext context = new MigrationContext(
            graph,
            "POST_MIGRATION",
            true
        );
        
        // When
        ErrorAnalysis analysis = module.analyzeErrors(List.of(error), context);
        
        // Then
        assertEquals(ErrorCategory.CLASSPATH_ISSUE, analysis.category());
    }
    
    /**
     * Creates a valid (non-empty) JAR file for testing.
     */
    private void createValidJarFile(Path jarPath) throws Exception {
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarPath.toFile()))) {
            // Add a manifest entry
            JarEntry manifestEntry = new JarEntry("META-INF/MANIFEST.MF");
            jos.putNextEntry(manifestEntry);
            jos.write("Manifest-Version: 1.0\n".getBytes());
            jos.closeEntry();
            
            // Add a dummy class entry
            JarEntry classEntry = new JarEntry("Test.class");
            jos.putNextEntry(classEntry);
            jos.write(new byte[]{(byte)0xCA, (byte)0xFE, (byte)0xBA, (byte)0xBE}); // Java class file magic number
            jos.closeEntry();
        }
    }
}

