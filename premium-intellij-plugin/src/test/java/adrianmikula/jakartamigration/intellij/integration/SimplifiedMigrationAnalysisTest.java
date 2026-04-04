package adrianmikula.jakartamigration.intellij.integration;

import adrianmikula.jakartamigration.intellij.service.SimplifiedMigrationAnalysisService;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyAnalysisReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for simplified migration analysis service
 */
public class SimplifiedMigrationAnalysisTest {
    
    @TempDir
    Path tempDir;
    
    @Test
    @DisplayName("Should analyze project with simple service")
    void testBasicAnalysis() throws IOException {
        // Create a simple Java project
        Path projectDir = tempDir.resolve("test-project");
        Files.createDirectories(projectDir.resolve("src/main/java"));
        
        String javaFile = """
            package com.example;
            
            import javax.persistence.Entity;
            public class TestEntity {}
            """;
        
        Files.write(projectDir.resolve("src/main/java/com/example/TestEntity.java"), javaFile.getBytes());
        
        // Test simplified analysis
        SimplifiedMigrationAnalysisService service = new SimplifiedMigrationAnalysisService();
        
        var result = service.analyzeProject(projectDir);
        
        assertNotNull(result);
        assertEquals(0, result.dependencyGraph().getNodes().size()); // No artifacts detected
        assertEquals(0, result.blockers().size()); // No blockers detected
        assertEquals(0, result.recommendations().size()); // No recommendations
        assertTrue(result.readinessScore().score() > 0); // Should have default score
    }
    
    @Test
    @DisplayName("Should handle empty project gracefully")
    void testEmptyProject() throws IOException {
        Path projectDir = tempDir.resolve("empty-project");
        Files.createDirectories(projectDir);
        
        SimplifiedMigrationAnalysisService service = new SimplifiedMigrationAnalysisService();
        
        var result = service.analyzeProject(projectDir);
        
        assertNotNull(result);
        assertEquals(0, result.dependencyGraph().getNodes().size());
        assertEquals(0, result.blockers().size());
        assertEquals(0, result.recommendations().size());
        assertEquals(0.8, result.readinessScore().score(), 0.01); // Default score
    }
}
