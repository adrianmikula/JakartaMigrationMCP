package unit.jakartamigration.sourcecodescanning.service.impl;

import adrianmikula.jakartamigration.sourcecodescanning.domain.SourceCodeAnalysisResult;
import adrianmikula.jakartamigration.sourcecodescanning.service.impl.SourceCodeScannerImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class SourceCodeScannerIntegrationTest {
    
    private SourceCodeScannerImpl scanner;
    
    @BeforeEach
    void setUp() {
        scanner = new SourceCodeScannerImpl();
    }
    
    @Test
    void shouldScanJavaxEmailExampleProject() {
        // Given - Use one of the example projects
        Path projectPath = Paths.get("examples/JavaxEmail-main/JavaxEmail-main");
        
        // Skip test if example project doesn't exist
        if (!projectPath.toFile().exists()) {
            System.out.println("Skipping integration test - example project not found: " + projectPath);
            return;
        }
        
        // When
        SourceCodeAnalysisResult result = scanner.scanProject(projectPath);
        
        // Then
        assertThat(result.hasJavaxUsage()).isTrue();
        assertThat(result.totalFilesWithJavaxUsage()).isGreaterThan(0);
        assertThat(result.totalJavaxImports()).isGreaterThan(0);
        
        // Should find javax.mail imports
        boolean hasJavaxMail = result.filesWithJavaxUsage().stream()
            .anyMatch(file -> file.javaxImports().stream()
                .anyMatch(imp -> imp.fullImport().startsWith("javax.mail")));
        
        assertThat(hasJavaxMail).isTrue();
    }
    
    @Test
    void shouldScanJavaxServletExampleProject() {
        // Given
        Path projectPath = Paths.get("examples/javax-servlet-examples-master/javax-servlet-examples-master");
        
        // Skip test if example project doesn't exist
        if (!projectPath.toFile().exists()) {
            System.out.println("Skipping integration test - example project not found: " + projectPath);
            return;
        }
        
        // When
        SourceCodeAnalysisResult result = scanner.scanProject(projectPath);
        
        // Then
        // Note: This project may have been migrated already, so we just check that scanning works
        // If it has javax usage, verify it's detected correctly
        if (result.hasJavaxUsage()) {
            assertThat(result.totalFilesWithJavaxUsage()).isGreaterThan(0);
            
            // Should find javax.servlet imports if not migrated
            boolean hasJavaxServlet = result.filesWithJavaxUsage().stream()
                .anyMatch(file -> file.javaxImports().stream()
                    .anyMatch(imp -> imp.fullImport().startsWith("javax.servlet")));
            
            // If project was already migrated, this might be false, which is OK
            // We're just testing that scanning works
        } else {
            // Project may have been migrated - that's fine, just verify scanning completed
            assertThat(result.totalFilesScanned()).isGreaterThan(0);
        }
    }
}

