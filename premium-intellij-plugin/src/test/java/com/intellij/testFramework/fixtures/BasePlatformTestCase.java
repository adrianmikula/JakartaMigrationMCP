package com.intellij.testFramework.fixtures;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Simplified base test case class for platform-related tests.
 * Provides common functionality for test setup and teardown.
 */
public abstract class BasePlatformTestCase {
    
    protected Project mockProject;
    protected Path tempDir;
    
    @BeforeEach
    public void setUp() throws Exception {
        // Create a simple mock project
        mockProject = ProjectManager.getInstance().getDefaultProject();
        tempDir = Files.createTempDirectory("test");
    }
    
    @AfterEach
    public void tearDown() throws IOException {
        if (tempDir != null && Files.exists(tempDir)) {
            Files.walk(tempDir)
                    .sorted((a, b) -> b.compareTo(a) * -1)
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            // Ignore cleanup errors
                        }
                    });
            Files.delete(tempDir);
        }
    }
    
    protected Project getProject() {
        return mockProject;
    }
    
    protected Path getTempDir() {
        return tempDir;
    }
}
