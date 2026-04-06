package adrianmikula.jakartamigration.intellij.integration;

import adrianmikula.jakartamigration.sourcecodescanning.service.impl.SimplifiedSourceCodeScannerImpl;
import adrianmikula.jakartamigration.sourcecodescanning.domain.SourceCodeAnalysisResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for simplified source code scanner
 */
public class SimplifiedSourceCodeScannerTest {
    
    @TempDir
    Path tempDir;
    
    @Test
    @DisplayName("Should scan project with simple regex detection")
    void testBasicScanning() throws IOException {
        // Create a simple Java project with javax imports
        Path projectDir = tempDir.resolve("test-project");
        Files.createDirectories(projectDir.resolve("src/main/java"));
        
        String javaFile = """
            package com.example;
            
            import javax.persistence.Entity;
            import javax.validation.constraints.NotNull;
            import javax.servlet.http.HttpServlet;
            import jakarta.enterprise.context.ApplicationScoped;
            
            @Entity
            public class TestEntity {
                @NotNull
                private String name;
            }
            """;
        
        Files.write(projectDir.resolve("src/main/java/com/example/TestEntity.java"), javaFile.getBytes());
        
        // Test simplified scanning
        SimplifiedSourceCodeScannerImpl scanner = new SimplifiedSourceCodeScannerImpl();
        
        var result = scanner.scanProject(projectDir);
        
        assertNotNull(result);
        assertTrue(result.totalFilesScanned() > 0, "Should scan some files");
        assertTrue(result.totalFilesWithJavaxUsage() > 0, "Should detect javax usage");
        assertTrue(result.totalJavaxImports() > 0, "Should count javax imports");
    }
    
    @Test
    @DisplayName("Should handle empty project gracefully")
    void testEmptyProject() throws IOException {
        Path projectDir = tempDir.resolve("empty-project");
        Files.createDirectories(projectDir);
        
        SimplifiedSourceCodeScannerImpl scanner = new SimplifiedSourceCodeScannerImpl();
        
        var result = scanner.scanProject(projectDir);
        
        assertNotNull(result);
        assertEquals(0, result.totalFilesScanned());
        assertEquals(0, result.totalFilesWithJavaxUsage());
        assertEquals(0, result.totalJavaxImports());
    }
}
