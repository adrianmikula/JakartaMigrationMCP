package adrianmikula.jakartamigration.runtimeverification.service;

import adrianmikula.jakartamigration.runtimeverification.domain.BytecodeAnalysisResult;
import adrianmikula.jakartamigration.runtimeverification.domain.RuntimeError;
import adrianmikula.jakartamigration.runtimeverification.domain.Warning;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Analyzes Java bytecode to detect Jakarta migration issues.
 * Fast, lightweight alternative to process execution.
 */
public interface BytecodeAnalyzer {
    
    /**
     * Analyzes a JAR file for Jakarta migration issues.
     *
     * @param jarPath Path to the JAR file to analyze
     * @return Bytecode analysis result with detected issues
     */
    BytecodeAnalysisResult analyzeJar(Path jarPath);
    
    /**
     * Analyzes a directory containing class files.
     *
     * @param classesDirectory Path to directory with .class files
     * @return Bytecode analysis result
     */
    BytecodeAnalysisResult analyzeClasses(Path classesDirectory);
    
    /**
     * Checks if a class uses javax namespace.
     *
     * @param className Fully qualified class name (e.g., "javax.servlet.ServletException")
     * @return true if class is in javax namespace
     */
    boolean isJavaxClass(String className);
    
    /**
     * Checks if a class uses jakarta namespace.
     *
     * @param className Fully qualified class name
     * @return true if class is in jakarta namespace
     */
    boolean isJakartaClass(String className);
    
    /**
     * Gets all javax classes found in the analysis.
     *
     * @return Set of javax class names
     */
    Set<String> getJavaxClasses();
    
    /**
     * Gets all jakarta classes found in the analysis.
     *
     * @return Set of jakarta class names
     */
    Set<String> getJakartaClasses();
}

