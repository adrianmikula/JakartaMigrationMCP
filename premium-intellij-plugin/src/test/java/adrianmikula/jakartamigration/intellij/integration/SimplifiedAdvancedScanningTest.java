package adrianmikula.jakartamigration.intellij.integration;

import adrianmikula.jakartamigration.intellij.service.SimplifiedAdvancedScanningService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for simplified advanced scanning service
 */
public class SimplifiedAdvancedScanningTest {
    
    @TempDir
    Path tempDir;
    
    @Test
    @DisplayName("Should scan project without complex memory management")
    void testBasicScanning() throws IOException {
        // Create a simple Java project
        Path projectDir = tempDir.resolve("test-project");
        Files.createDirectories(projectDir.resolve("src/main/java"));
        
        String javaFile = """
            package com.example;
            
            import javax.persistence.Entity;
            import javax.validation.constraints.NotNull;
            import javax.servlet.http.HttpServlet;
            import javax.inject.Inject;
            
            @Entity
            public class TestEntity {
                @NotNull
                private String name;
                
                @Inject
                private TestService service;
            }
            
            public class TestService {}
            """;
        
        Files.write(projectDir.resolve("src/main/java/com/example/TestEntity.java"), javaFile.getBytes());
        
        // Create pom.xml
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>test-project</artifactId>
                <version>1.0.0</version>
                <dependencies>
                    <dependency>
                        <groupId>javax.persistence</groupId>
                        <artifactId>javax.persistence-api</artifactId>
                        <version>2.2</version>
                    </dependency>
                </dependencies>
            </project>
            """;
        
        Files.write(projectDir.resolve("pom.xml"), pomContent.getBytes());
        
        // Test simplified scanning
        SimplifiedAdvancedScanningService service = new SimplifiedAdvancedScanningService();
        
        var result = service.scanAll(projectDir);
        
        assertNotNull(result);
        assertTrue(result.contains("scan completed"), "Should complete scan successfully");
        assertTrue(result.contains("0 JPA issues") || result.contains("JPA issues"), "Should handle empty JPA scan results");
    }
    
    @Test
    @DisplayName("Should handle empty project gracefully")
    void testEmptyProject() throws IOException {
        Path projectDir = tempDir.resolve("empty-project");
        Files.createDirectories(projectDir);
        
        SimplifiedAdvancedScanningService service = new SimplifiedAdvancedScanningService();
        
        var result = service.scanAll(projectDir);
        
        assertNotNull(result);
        assertTrue(result.contains("scan completed"), "Should complete scan successfully");
        assertTrue(result.contains("JPA issues"), "Should handle JPA scan results");
        // Should not crash on empty project
    }
}
