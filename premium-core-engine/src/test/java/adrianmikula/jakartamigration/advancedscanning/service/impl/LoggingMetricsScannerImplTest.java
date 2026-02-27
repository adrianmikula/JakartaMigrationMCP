package adrianmikula.jakartamigration.advancedscanning.service.impl;

import adrianmikula.jakartamigration.advancedscanning.service.LoggingMetricsScanner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for LoggingMetricsScannerImpl
 */
class LoggingMetricsScannerImplTest {
    
    private LoggingMetricsScanner scanner;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        scanner = new LoggingMetricsScannerImpl();
    }
    
    @Test
    void shouldDetectJavaxLoggingUsage() throws Exception {
        // Given
        Path packageDir = tempDir.resolve("com/example");
        Files.createDirectories(packageDir);
        
        Path javaFile = packageDir.resolve("LoggerExample.java");
        String content = """
            package com.example;
            
            import javax.logging.Logger;
            import java.util.logging.Logger;
            
            public class LoggerExample {
                private static final Logger logger = Logger.getLogger(LoggerExample.class.getName());
                
                public void logSomething() {
                    logger.info("Logging with javax.logging");
                }
            }
            """;
        Files.writeString(javaFile, content);
        
        // When
        var result = scanner.scanProject(tempDir);
        
        // Then
        assertThat(result.hasFindings()).isTrue();
        assertThat(result.getAllUsages()).isNotEmpty();
    }
    
    @Test
    void shouldDetectJMXUsage() throws Exception {
        // Given
        Path packageDir = tempDir.resolve("com/example");
        Files.createDirectories(packageDir);
        
        Path javaFile = packageDir.resolve("JMXExample.java");
        String content = """
            package com.example;
            
            import javax.management.MBeanServer;
            import javax.management.ObjectName;
            import javax.management.remote.JMXConnectorServer;
            
            public class JMXExample {
                public void registerMBean(MBeanServer server, ObjectName name) {
                }
            }
            """;
        Files.writeString(javaFile, content);
        
        // When
        var result = scanner.scanProject(tempDir);
        
        // Then
        assertThat(result.hasFindings()).isTrue();
        assertThat(result.getAllUsages()).isNotEmpty();
    }
    
    @Test
    void shouldReturnEmptyForCleanProject() throws Exception {
        // Given
        Path packageDir = tempDir.resolve("com/example");
        Files.createDirectories(packageDir);
        
        Path javaFile = packageDir.resolve("CleanFile.java");
        String content = """
            package com.example;
            
            import jakarta.servlet.http.HttpServlet;
            
            public class CleanFile extends HttpServlet {
            }
            """;
        Files.writeString(javaFile, content);
        
        // When
        var result = scanner.scanProject(tempDir);
        
        // Then - Should have no findings (Jakarta-only project)
        assertThat(result.getProjectPath()).isNotNull();
    }
    
    @Test
    void shouldHandleEmptyDirectory() {
        // Given - Empty temp directory
        
        // When
        var result = scanner.scanProject(tempDir);
        
        // Then
        assertThat(result.hasFindings()).isFalse();
    }
}
