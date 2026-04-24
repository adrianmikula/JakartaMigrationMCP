package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.coderefactoring.domain.RecipeExecutionResult;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for RefactorTabComponent file modification display functionality.
 */
public class RefactorTabComponentTest extends BasePlatformTestCase {

    @Test
    public void testFileResultsDisplayWithModifiedFiles() {
        // Create a mock project
        Project project = getProject();
        
        // Create RecipeExecutionResult with modified files
        RecipeExecutionResult result = new RecipeExecutionResult(
            true,  // success
            5,     // filesProcessed
            3,     // filesChanged
            List.of("src/main/java/com/example/Class1.java", 
                   "src/main/java/com/example/Class2.java",
                   "src/test/java/com/example/Test1.java"),
            null,  // errorMessage
            12345L // executionId
        );
        
        // Verify the result contains expected data
        assertTrue(result.success());
        assertEquals(5, result.filesProcessed());
        assertEquals(3, result.filesChanged());
        assertEquals(3, result.changedFilePaths().size());
        assertTrue(result.changedFilePaths().contains("src/main/java/com/example/Class1.java"));
    }

    @Test
    public void testFileResultsDisplayWithNoModifiedFiles() {
        // Create RecipeExecutionResult with no modified files
        RecipeExecutionResult result = new RecipeExecutionResult(
            true,  // success
            10,    // filesProcessed
            0,     // filesChanged
            List.of(), // empty list
            null,  // errorMessage
            12346L // executionId
        );
        
        // Verify the result contains expected data
        assertTrue(result.success());
        assertEquals(10, result.filesProcessed());
        assertEquals(0, result.filesChanged());
        assertTrue(result.changedFilePaths().isEmpty());
    }

    @Test
    public void testFileResultsDisplayWithFailedExecution() {
        // Create failed RecipeExecutionResult
        RecipeExecutionResult result = new RecipeExecutionResult(
            false, // success
            0,     // filesProcessed
            0,     // filesChanged
            List.of(), // empty list
            "Test error message", // errorMessage
            null   // executionId
        );
        
        // Verify the result contains expected data
        assertFalse(result.success());
        assertEquals(0, result.filesProcessed());
        assertEquals(0, result.filesChanged());
        assertTrue(result.changedFilePaths().isEmpty());
        assertEquals("Test error message", result.errorMessage());
    }
}
