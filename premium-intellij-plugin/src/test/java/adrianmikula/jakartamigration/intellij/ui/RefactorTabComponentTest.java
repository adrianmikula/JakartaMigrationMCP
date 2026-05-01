package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.coderefactoring.domain.RecipeExecutionResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for RefactorTabComponent file modification display functionality.
 */
public class RefactorTabComponentTest {

    @Test
    public void testFileResultsDisplayWithModifiedFiles() {
        RecipeExecutionResult result = new RecipeExecutionResult(
            true,
            5,
            3,
            List.of("src/main/java/com/example/Class1.java",
                    "src/main/java/com/example/Class2.java",
                    "src/test/java/com/example/Test1.java"),
            null,
            12345L
        );

        assertTrue(result.success());
        assertEquals(5, result.filesProcessed());
        assertEquals(3, result.filesChanged());
        assertEquals(3, result.changedFilePaths().size());
        assertTrue(result.changedFilePaths().contains("src/main/java/com/example/Class1.java"));
    }

    @Test
    public void testFileResultsDisplayWithNoModifiedFiles() {
        RecipeExecutionResult result = new RecipeExecutionResult(
            true,
            10,
            0,
            List.of(),
            null,
            12346L
        );

        assertTrue(result.success());
        assertEquals(10, result.filesProcessed());
        assertEquals(0, result.filesChanged());
        assertTrue(result.changedFilePaths().isEmpty());
    }

    @Test
    public void testFileResultsDisplayWithFailedExecution() {
        RecipeExecutionResult result = new RecipeExecutionResult(
            false,
            0,
            0,
            List.of(),
            "Test error message",
            null
        );

        assertFalse(result.success());
        assertEquals(0, result.filesProcessed());
        assertEquals(0, result.filesChanged());
        assertTrue(result.changedFilePaths().isEmpty());
        assertEquals("Test error message", result.errorMessage());
    }
}
