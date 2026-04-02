package adrianmikula.jakartamigration.intellij.integration;

import adrianmikula.jakartamigration.intellij.service.AdvancedScanningService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple integration test for advanced scanning without external dependencies.
 */
public class SimpleAdvancedScanningTest {
    
    @TempDir
    Path tempDir;
    
    private AdvancedScanningService advancedScanningService;
    
    @BeforeEach
    void setUp() {
        advancedScanningService = new AdvancedScanningService(null);
    }
    
    @Test
    @DisplayName("Advanced scanning should handle empty project gracefully")
    void testAdvancedScanningEmptyProject() throws IOException {
        // Create a minimal project
        Path projectDir = tempDir.resolve("empty-project");
        Files.createDirectories(projectDir);
        
        // Run advanced scanning
        var result = advancedScanningService.scanAll(projectDir);
        
        // Verify result
        assertNotNull(result);
        System.out.println("Advanced scanning completed successfully");
        System.out.println("JPA result: " + (result.jpaResult() != null));
        System.out.println("Bean validation result: " + (result.beanValidationResult() != null));
        System.out.println("Servlet JSP result: " + (result.servletJspResult() != null));
    }
    
    @Test
    @DisplayName("Advanced scanning should handle project with Java files")
    void testAdvancedScanningWithJavaFiles() throws IOException {
        // Create a project with Java files
        Path projectDir = tempDir.resolve("java-project");
        Path srcDir = projectDir.resolve("src/main/java/com/example");
        Files.createDirectories(srcDir);
        
        String javaContent = """
            package com.example;
            
            import javax.servlet.ServletException;
            import javax.servlet.http.HttpServlet;
            import javax.persistence.Entity;
            import javax.persistence.Id;
            import javax.validation.constraints.NotNull;
            
            @Entity
            public class TestServlet extends HttpServlet {
                @Id
                private Long id;
                
                @NotNull
                private String name;
                
                public Long getId() { return id; }
                public void setId(Long id) { this.id = id; }
                public String getName() { return name; }
                public void setName(String name) { this.name = name; }
            }
            """;
        
        Files.write(srcDir.resolve("TestServlet.java"), javaContent.getBytes());
        
        // Run advanced scanning
        var result = advancedScanningService.scanAll(projectDir);
        
        // Verify result
        assertNotNull(result);
        System.out.println("Advanced scanning completed successfully");
        System.out.println("JPA result: " + (result.jpaResult() != null));
        System.out.println("Bean validation result: " + (result.beanValidationResult() != null));
        System.out.println("Servlet JSP result: " + (result.servletJspResult() != null));
        System.out.println("CDI injection result: " + (result.cdiInjectionResult() != null));
    }
}
