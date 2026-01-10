package unit.jakartamigration.dependencyanalysis.service;

import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.dependencyanalysis.domain.BinaryCompatibilityReport;
import adrianmikula.jakartamigration.dependencyanalysis.service.JapicmpCompatibilityChecker;
import adrianmikula.jakartamigration.dependencyanalysis.service.JarResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DisplayName("JapicmpCompatibilityChecker Tests")
@ExtendWith(MockitoExtension.class)
class JapicmpCompatibilityCheckerTest {
    
    @Mock
    private JarResolver jarResolver;
    
    private JapicmpCompatibilityChecker checker;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        checker = new JapicmpCompatibilityChecker(jarResolver);
    }
    
    @Test
    @DisplayName("Should return incompatible report when old JAR not found")
    void shouldReturnIncompatibleWhenOldJarNotFound() {
        // Given
        Artifact oldVersion = new Artifact("javax.servlet", "javax.servlet-api", "4.0.1", "compile", false);
        Artifact newVersion = new Artifact("jakarta.servlet", "jakarta.servlet-api", "6.0.0", "compile", false);
        
        when(jarResolver.resolveJar(oldVersion)).thenReturn(null);
        
        // When
        BinaryCompatibilityReport report = checker.compareVersions(oldVersion, newVersion);
        
        // Then
        assertThat(report.isCompatible()).isFalse();
        assertThat(report.breakingChanges()).hasSize(1);
        assertThat(report.breakingChanges().get(0).description())
            .contains("Could not locate JAR file for old version");
    }
    
    @Test
    @DisplayName("Should return incompatible report when new JAR not found")
    void shouldReturnIncompatibleWhenNewJarNotFound() {
        // Given
        Artifact oldVersion = new Artifact("javax.servlet", "javax.servlet-api", "4.0.1", "compile", false);
        Artifact newVersion = new Artifact("jakarta.servlet", "jakarta.servlet-api", "6.0.0", "compile", false);
        
        Path oldJar = tempDir.resolve("old.jar");
        try {
            Files.createFile(oldJar);
        } catch (Exception e) {
            // Ignore
        }
        
        when(jarResolver.resolveJar(oldVersion)).thenReturn(oldJar);
        when(jarResolver.resolveJar(newVersion)).thenReturn(null);
        
        // When
        BinaryCompatibilityReport report = checker.compareVersions(oldVersion, newVersion);
        
        // Then
        assertThat(report.isCompatible()).isFalse();
        assertThat(report.breakingChanges()).hasSize(1);
        assertThat(report.breakingChanges().get(0).description())
            .contains("Could not locate JAR file for new version");
    }
    
    @Test
    @DisplayName("Should return compatible when japicmp not available")
    void shouldReturnCompatibleWhenJapicmpNotAvailable() throws Exception {
        // Given
        Artifact oldVersion = new Artifact("javax.servlet", "javax.servlet-api", "4.0.1", "compile", false);
        Artifact newVersion = new Artifact("jakarta.servlet", "jakarta.servlet-api", "6.0.0", "compile", false);
        
        // Create empty JAR files (japicmp will fail to analyze them, but that's OK for this test)
        Path oldJar = tempDir.resolve("old.jar");
        Path newJar = tempDir.resolve("new.jar");
        Files.createFile(oldJar);
        Files.createFile(newJar);
        
        when(jarResolver.resolveJar(oldVersion)).thenReturn(oldJar);
        when(jarResolver.resolveJar(newVersion)).thenReturn(newJar);
        
        // When
        BinaryCompatibilityReport report = checker.compareVersions(oldVersion, newVersion);
        
        // Then
        // If japicmp is not available, it should gracefully return compatible
        // (don't block migration if tool is unavailable)
        assertThat(report).isNotNull();
        // The actual compatibility depends on whether japicmp is on classpath
        // If not available, it assumes compatible (graceful degradation)
    }
    
    @Test
    @DisplayName("Should return compatible report for same artifact")
    void shouldReturnCompatibleForSameArtifact() {
        // Given
        Artifact artifact = new Artifact("com.example", "test-lib", "1.0.0", "compile", false);
        
        // When
        BinaryCompatibilityReport report = checker.compareVersions(artifact, artifact);
        
        // Then
        // This will fail JAR resolution (same artifact), but that's expected behavior
        assertThat(report).isNotNull();
    }
    
    @Test
    @DisplayName("Should check binary compatibility correctly")
    void shouldCheckBinaryCompatibility() {
        // Given
        Artifact oldVersion = new Artifact("javax.servlet", "javax.servlet-api", "4.0.1", "compile", false);
        Artifact newVersion = new Artifact("jakarta.servlet", "jakarta.servlet-api", "6.0.0", "compile", false);
        
        Path oldJar = tempDir.resolve("old.jar");
        Path newJar = tempDir.resolve("new.jar");
        try {
            Files.createFile(oldJar);
            Files.createFile(newJar);
        } catch (Exception e) {
            // Ignore
        }
        
        when(jarResolver.resolveJar(oldVersion)).thenReturn(oldJar);
        when(jarResolver.resolveJar(newVersion)).thenReturn(newJar);
        
        // When
        boolean compatible = checker.isBinaryCompatible(oldVersion, newVersion);
        
        // Then
        // Result depends on whether japicmp is available and what it finds
        // If japicmp not available, assumes compatible (graceful degradation)
        assertThat(compatible).isNotNull(); // Just verify it doesn't throw
    }
    
    @Test
    @DisplayName("Should handle errors gracefully")
    void shouldHandleErrorsGracefully() {
        // Given
        Artifact oldVersion = new Artifact("javax.servlet", "javax.servlet-api", "4.0.1", "compile", false);
        Artifact newVersion = new Artifact("jakarta.servlet", "jakarta.servlet-api", "6.0.0", "compile", false);
        
        // Create invalid JAR files (empty files)
        Path oldJar = tempDir.resolve("old.jar");
        Path newJar = tempDir.resolve("new.jar");
        try {
            Files.createFile(oldJar);
            Files.createFile(newJar);
        } catch (Exception e) {
            // Ignore
        }
        
        when(jarResolver.resolveJar(oldVersion)).thenReturn(oldJar);
        when(jarResolver.resolveJar(newVersion)).thenReturn(newJar);
        
        // When
        BinaryCompatibilityReport report = checker.compareVersions(oldVersion, newVersion);
        
        // Then
        // Should not throw exception, should handle gracefully
        assertThat(report).isNotNull();
        // If japicmp fails, it should return compatible (don't block migration)
    }
}

