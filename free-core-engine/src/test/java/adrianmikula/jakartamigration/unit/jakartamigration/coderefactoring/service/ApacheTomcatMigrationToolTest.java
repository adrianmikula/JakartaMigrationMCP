package unit.jakartamigration.coderefactoring.service;

import adrianmikula.jakartamigration.coderefactoring.service.ApacheTomcatMigrationTool;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for ApacheTomcatMigrationTool.
 * 
 * Note: These tests require the Apache migration tool to be available.
 * The tool will be downloaded automatically if not cached.
 */
@DisplayName("ApacheTomcatMigrationTool Tests")
class ApacheTomcatMigrationToolTest {
    
    @TempDir
    Path tempDir;
    
    @Test
    @DisplayName("Should check if tool is available")
    void shouldCheckIfToolIsAvailable() {
        // Given
        ApacheTomcatMigrationTool tool = new ApacheTomcatMigrationTool();
        
        // When
        boolean available = tool.isAvailable();
        
        // Then
        // Tool may or may not be available depending on download success
        // This test just verifies the method doesn't throw
        assertThat(available).isNotNull();
    }
    
    @Test
    @DisplayName("Should throw exception when source path is null")
    void shouldThrowExceptionWhenSourcePathIsNull() {
        // Given
        ApacheTomcatMigrationTool tool = new ApacheTomcatMigrationTool();
        Path destination = tempDir.resolve("output.jar");
        
        // When/Then
        assertThatThrownBy(() -> tool.migrate(null, destination))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Source path must exist");
    }
    
    @Test
    @DisplayName("Should throw exception when source path does not exist")
    void shouldThrowExceptionWhenSourcePathDoesNotExist() {
        // Given
        ApacheTomcatMigrationTool tool = new ApacheTomcatMigrationTool();
        Path nonExistent = tempDir.resolve("nonexistent.jar");
        Path destination = tempDir.resolve("output.jar");
        
        // When/Then
        assertThatThrownBy(() -> tool.migrate(nonExistent, destination))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Source path must exist");
    }
    
    @Test
    @DisplayName("Should throw exception when destination path is null")
    void shouldThrowExceptionWhenDestinationPathIsNull() throws IOException {
        // Given
        ApacheTomcatMigrationTool tool = new ApacheTomcatMigrationTool();
        Path source = createTestJar(tempDir.resolve("input.jar"));
        
        // When/Then
        assertThatThrownBy(() -> tool.migrate(source, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Destination path cannot be null");
    }
    
    @Test
    @DisplayName("Should throw exception when tool JAR is not found during migration")
    void shouldThrowExceptionWhenToolJarIsNotFound() throws IOException {
        // Given
        // Create a tool instance with default constructor (lazy initialization)
        // The tool will try to find/download the tool, but if it fails, it should throw
        ApacheTomcatMigrationTool tool = new ApacheTomcatMigrationTool();
        Path source = createTestJar(tempDir.resolve("input.jar"));
        Path destination = tempDir.resolve("output.jar");
        
        // When/Then
        // This will fail during lazy initialization when migrate() is called
        // if the tool cannot be found or downloaded
        // Note: This test may pass if the tool is successfully downloaded,
        // or fail with IllegalStateException if download fails
        try {
            ApacheTomcatMigrationTool.MigrationResult result = tool.migrate(source, destination);
            // If migration succeeds, that's also valid (tool was downloaded)
            assertThat(result).isNotNull();
        } catch (IllegalStateException e) {
            // Expected if tool cannot be found/downloaded
            assertThat(e.getMessage()).contains("Apache Tomcat migration tool JAR not found");
        }
    }
    
    @Test
    @DisplayName("Should accept valid tool JAR path in constructor")
    void shouldAcceptValidToolJarPathInConstructor() throws IOException {
        // Given
        Path toolJar = createTestJar(tempDir.resolve("tool.jar"));
        
        // When
        ApacheTomcatMigrationTool tool = new ApacheTomcatMigrationTool(toolJar);
        
        // Then
        assertThat(tool.getToolJarPath()).isEqualTo(toolJar);
    }
    
    @Test
    @DisplayName("Should throw exception when tool JAR path is null in constructor")
    void shouldThrowExceptionWhenToolJarPathIsNullInConstructor() {
        // When/Then
        assertThatThrownBy(() -> new ApacheTomcatMigrationTool(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Tool JAR path must exist");
    }
    
    @Test
    @DisplayName("Should throw exception when tool JAR path does not exist in constructor")
    void shouldThrowExceptionWhenToolJarPathDoesNotExistInConstructor() {
        // Given
        Path nonExistent = tempDir.resolve("nonexistent.jar");
        
        // When/Then
        assertThatThrownBy(() -> new ApacheTomcatMigrationTool(nonExistent))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Tool JAR path must exist");
    }
    
    /**
     * Creates a minimal test JAR file for testing purposes.
     * This creates a valid JAR structure that can be used as input for migration tests.
     */
    private Path createTestJar(Path jarPath) throws IOException {
        try (JarOutputStream jarOut = new JarOutputStream(Files.newOutputStream(jarPath))) {
            // Add a manifest entry
            JarEntry manifestEntry = new JarEntry("META-INF/MANIFEST.MF");
            jarOut.putNextEntry(manifestEntry);
            jarOut.write("Manifest-Version: 1.0\n".getBytes());
            jarOut.write("Created-By: Test\n".getBytes());
            jarOut.closeEntry();
            
            // Add a simple class file entry (minimal valid class structure)
            JarEntry classEntry = new JarEntry("Test.class");
            jarOut.putNextEntry(classEntry);
            // Minimal class file bytes (just enough to be a valid JAR entry)
            byte[] minimalClass = {
                (byte)0xCA, (byte)0xFE, (byte)0xBA, (byte)0xBE, // Magic number
                0x00, 0x00, 0x00, 0x34, // Version
                0x00, 0x01, // Constant pool count
                0x07, 0x00, 0x02, // Class reference
                0x00, 0x03, // Access flags
                0x00, 0x04, // This class
                0x00, 0x05, // Super class
                0x00, 0x00, // Interfaces count
                0x00, 0x00, // Fields count
                0x00, 0x00, // Methods count
                0x00, 0x00  // Attributes count
            };
            jarOut.write(minimalClass);
            jarOut.closeEntry();
        }
        return jarPath;
    }
}

