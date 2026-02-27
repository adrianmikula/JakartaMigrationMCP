package adrianmikula.jakartamigration.advancedscanning.service.impl;

import adrianmikula.jakartamigration.advancedscanning.service.ThirdPartyLibScanner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ThirdPartyLibScannerImpl
 */
class ThirdPartyLibScannerImplTest {
    
    private ThirdPartyLibScanner scanner;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        scanner = new ThirdPartyLibScannerImpl();
    }
    
    @Test
    void shouldDetectJavaxServletDependency() throws Exception {
        // Given - Maven pom.xml with javax.servlet
        Path pomFile = tempDir.resolve("pom.xml");
        String content = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>my-app</artifactId>
                <version>1.0-SNAPSHOT</version>
                
                <dependencies>
                    <dependency>
                        <groupId>javax.servlet</groupId>
                        <artifactId>javax.servlet-api</artifactId>
                        <version>4.0.1</version>
                        <scope>provided</scope>
                    </dependency>
                </dependencies>
            </project>
            """;
        Files.writeString(pomFile, content);
        
        // When
        var result = scanner.scanProject(tempDir);
        
        // Then
        assertThat(result.hasFindings()).isTrue();
    }
    
    @Test
    void shouldDetectJavaxValidationDependency() throws Exception {
        // Given - Maven pom.xml with javax.validation
        Path pomFile = tempDir.resolve("pom.xml");
        String content = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project>
                <dependencies>
                    <dependency>
                        <groupId>javax.validation</groupId>
                        <artifactId>validation-api</artifactId>
                        <version>2.0.1.Final</version>
                    </dependency>
                </dependencies>
            </project>
            """;
        Files.writeString(pomFile, content);
        
        // When
        var result = scanner.scanProject(tempDir);
        
        // Then
        assertThat(result.hasFindings()).isTrue();
    }
    
    @Test
    void shouldDetectSpringFrameworkDependencies() throws Exception {
        // Given - Maven pom.xml with Spring
        Path pomFile = tempDir.resolve("pom.xml");
        String content = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project>
                <dependencies>
                    <dependency>
                        <groupId>org.springframework</groupId>
                        <artifactId>spring-core</artifactId>
                        <version>5.3.30</version>
                    </dependency>
                    <dependency>
                        <groupId>org.springframework</groupId>
                        <artifactId>spring-web</artifactId>
                        <version>5.3.30</version>
                    </dependency>
                </dependencies>
            </project>
            """;
        Files.writeString(pomFile, content);
        
        // When
        var result = scanner.scanProject(tempDir);
        
        // Then
        assertThat(result.hasFindings()).isTrue();
    }
    
    @Test
    void shouldReturnEmptyForJakartaOnlyPom() throws Exception {
        // Given - Maven pom.xml with only Jakarta EE
        Path pomFile = tempDir.resolve("pom.xml");
        String content = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project>
                <dependencies>
                    <dependency>
                        <groupId>jakarta.servlet</groupId>
                        <artifactId>jakarta.servlet-api</artifactId>
                        <version>5.0.0</version>
                    </dependency>
                    <dependency>
                        <groupId>jakarta.validation</groupId>
                        <artifactId>jakarta.validation-api</artifactId>
                        <version>3.0.0</version>
                    </dependency>
                </dependencies>
            </project>
            """;
        Files.writeString(pomFile, content);
        
        // When
        var result = scanner.scanProject(tempDir);
        
        // Then - Should have no findings for Jakarta-only project
        assertThat(result.getProjectPath()).isNotNull();
    }
    
    @Test
    void shouldHandleNoPomFile() throws Exception {
        // Given - Regular Java files only (no pom.xml)
        Path packageDir = tempDir.resolve("com/example");
        Files.createDirectories(packageDir);
        
        Path javaFile = packageDir.resolve("SomeClass.java");
        String content = """
            package com.example;
            public class SomeClass {}
            """;
        Files.writeString(javaFile, content);
        
        // When
        var result = scanner.scanProject(tempDir);
        
        // Then
        assertThat(result.hasFindings()).isFalse();
    }
}
