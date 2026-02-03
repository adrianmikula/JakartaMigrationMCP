package unit.jakartamigration.sourcecodescanning.service.impl;

import adrianmikula.jakartamigration.sourcecodescanning.domain.FileUsage;
import adrianmikula.jakartamigration.sourcecodescanning.domain.ImportStatement;
import adrianmikula.jakartamigration.sourcecodescanning.domain.SourceCodeAnalysisResult;
import adrianmikula.jakartamigration.sourcecodescanning.service.impl.SourceCodeScannerImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SourceCodeScannerTest {
    
    private SourceCodeScannerImpl scanner;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        scanner = new SourceCodeScannerImpl();
    }
    
    @Test
    void shouldScanFileWithJavaxServletImports() throws Exception {
        // Given
        Path javaFile = tempDir.resolve("TestServlet.java");
        String content = """
            package com.example;
            
            import javax.servlet.ServletException;
            import javax.servlet.http.HttpServlet;
            import javax.servlet.http.HttpServletRequest;
            import java.io.IOException;
            
            public class TestServlet extends HttpServlet {
                protected void doGet(HttpServletRequest req) throws ServletException, IOException {
                }
            }
            """;
        Files.writeString(javaFile, content);
        
        // When
        FileUsage usage = scanner.scanFile(javaFile);
        
        // Then
        assertThat(usage.hasJavaxUsage()).isTrue();
        assertThat(usage.getJavaxImportCount()).isEqualTo(3);
        assertThat(usage.javaxImports()).hasSize(3);
        
        List<String> importNames = usage.javaxImports().stream()
            .map(ImportStatement::fullImport)
            .toList();
        
        assertThat(importNames).contains(
            "javax.servlet.ServletException",
            "javax.servlet.http.HttpServlet",
            "javax.servlet.http.HttpServletRequest"
        );
        
        // Check Jakarta equivalents
        ImportStatement servletException = usage.javaxImports().stream()
            .filter(imp -> imp.fullImport().equals("javax.servlet.ServletException"))
            .findFirst()
            .orElseThrow();
        
        assertThat(servletException.jakartaEquivalent()).isEqualTo("jakarta.servlet.ServletException");
        assertThat(servletException.javaxPackage()).isEqualTo("javax.servlet");
    }
    
    @Test
    void shouldScanFileWithJavaxValidationImports() throws Exception {
        // Given
        Path javaFile = tempDir.resolve("Validator.java");
        String content = """
            package com.example;
            
            import javax.validation.constraints.NotNull;
            import javax.validation.Validator;
            
            public class Validator {
                @NotNull
                private String name;
            }
            """;
        Files.writeString(javaFile, content);
        
        // When
        FileUsage usage = scanner.scanFile(javaFile);
        
        // Then
        assertThat(usage.hasJavaxUsage()).isTrue();
        assertThat(usage.getJavaxImportCount()).isEqualTo(2);
    }
    
    @Test
    void shouldReturnEmptyForFileWithoutJavaxImports() throws Exception {
        // Given
        Path javaFile = tempDir.resolve("PlainJava.java");
        String content = """
            package com.example;
            
            import java.util.List;
            import java.io.IOException;
            
            public class PlainJava {
                public void doSomething() {
                }
            }
            """;
        Files.writeString(javaFile, content);
        
        // When
        FileUsage usage = scanner.scanFile(javaFile);
        
        // Then
        assertThat(usage.hasJavaxUsage()).isFalse();
        assertThat(usage.getJavaxImportCount()).isZero();
        assertThat(usage.javaxImports()).isEmpty();
    }
    
    @Test
    void shouldScanProjectWithMultipleFiles() throws Exception {
        // Given
        Path srcDir = tempDir.resolve("src/main/java/com/example");
        Files.createDirectories(srcDir);
        
        Path servletFile = srcDir.resolve("MyServlet.java");
        Files.writeString(servletFile, """
            package com.example;
            import javax.servlet.ServletException;
            public class MyServlet {
            }
            """);
        
        Path validatorFile = srcDir.resolve("Validator.java");
        Files.writeString(validatorFile, """
            package com.example;
            import javax.validation.NotNull;
            public class Validator {
            }
            """);
        
        Path plainFile = srcDir.resolve("Plain.java");
        Files.writeString(plainFile, """
            package com.example;
            import java.util.List;
            public class Plain {
            }
            """);
        
        // When
        SourceCodeAnalysisResult result = scanner.scanProject(tempDir);
        
        // Then
        assertThat(result.hasJavaxUsage()).isTrue();
        assertThat(result.totalFilesWithJavaxUsage()).isGreaterThanOrEqualTo(2);
        assertThat(result.totalJavaxImports()).isGreaterThanOrEqualTo(2);
        assertThat(result.filesWithJavaxUsage().size()).isGreaterThanOrEqualTo(2);
    }
    
    @Test
    void shouldExcludeBuildDirectories() throws Exception {
        // Given
        Path srcDir = tempDir.resolve("src/main/java");
        Files.createDirectories(srcDir);
        
        Path sourceFile = srcDir.resolve("Test.java");
        Files.writeString(sourceFile, """
            package com.example;
            import javax.servlet.ServletException;
            public class Test {
            }
            """);
        
        // Create build directory with Java file
        Path targetDir = tempDir.resolve("target/classes/com/example");
        Files.createDirectories(targetDir);
        Path buildFile = targetDir.resolve("Test.class.java");
        Files.writeString(buildFile, """
            package com.example;
            import javax.servlet.ServletException;
            public class Test {
            }
            """);
        
        // When
        SourceCodeAnalysisResult result = scanner.scanProject(tempDir);
        
        // Then
        // Should only find the source file, not the build file
        assertThat(result.totalFilesWithJavaxUsage()).isEqualTo(1);
        assertThat(result.filesWithJavaxUsage().get(0).filePath())
            .isEqualTo(sourceFile);
    }
    
    @Test
    void shouldHandleEmptyProject() {
        // When
        SourceCodeAnalysisResult result = scanner.scanProject(tempDir);
        
        // Then
        assertThat(result.hasJavaxUsage()).isFalse();
        assertThat(result.totalFilesScanned()).isZero();
        assertThat(result.filesWithJavaxUsage()).isEmpty();
    }
    
    @Test
    void shouldHandleInvalidPath() {
        // When
        SourceCodeAnalysisResult result = scanner.scanProject(tempDir.resolve("nonexistent"));
        
        // Then
        assertThat(result.hasJavaxUsage()).isFalse();
        assertThat(result.totalFilesScanned()).isZero();
    }
}

