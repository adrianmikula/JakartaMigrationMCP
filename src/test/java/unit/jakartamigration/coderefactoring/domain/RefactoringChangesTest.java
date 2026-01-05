package unit.jakartamigration.coderefactoring.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for RefactoringChanges.
 */
@DisplayName("RefactoringChanges Tests")
class RefactoringChangesTest {
    
    @Test
    @DisplayName("Should create refactoring changes successfully")
    void shouldCreateRefactoringChangesSuccessfully() {
        // Given
        String filePath = "Test.java";
        String originalContent = "import javax.servlet.ServletException;";
        String refactoredContent = "import jakarta.servlet.ServletException;";
        List<ChangeDetail> changes = List.of(
            new ChangeDetail(1, originalContent, refactoredContent, "Updated import", ChangeType.IMPORT_CHANGE)
        );
        List<Recipe> recipes = List.of(Recipe.jakartaNamespaceRecipe());
        
        // When
        RefactoringChanges result = new RefactoringChanges(
            filePath,
            originalContent,
            refactoredContent,
            changes,
            recipes
        );
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.filePath()).isEqualTo(filePath);
        assertThat(result.originalContent()).isEqualTo(originalContent);
        assertThat(result.refactoredContent()).isEqualTo(refactoredContent);
        assertThat(result.hasChanges()).isTrue();
        assertThat(result.changeCount()).isEqualTo(1);
    }
    
    @Test
    @DisplayName("Should detect when no changes were made")
    void shouldDetectWhenNoChangesWereMade() {
        // Given
        String content = "public class Test {}";
        List<ChangeDetail> changes = List.of();
        List<Recipe> recipes = List.of(Recipe.jakartaNamespaceRecipe());
        
        // When
        RefactoringChanges result = new RefactoringChanges(
            "Test.java",
            content,
            content,
            changes,
            recipes
        );
        
        // Then
        assertThat(result.hasChanges()).isFalse();
        assertThat(result.changeCount()).isEqualTo(0);
    }
    
    @Test
    @DisplayName("Should throw exception when file path is null")
    void shouldThrowExceptionWhenFilePathIsNull() {
        // When/Then
        assertThatThrownBy(() -> new RefactoringChanges(
            null,
            "content",
            "content",
            List.of(),
            List.of()
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("FilePath cannot be null or blank");
    }
    
    @Test
    @DisplayName("Should throw exception when original content is null")
    void shouldThrowExceptionWhenOriginalContentIsNull() {
        // When/Then
        assertThatThrownBy(() -> new RefactoringChanges(
            "Test.java",
            null,
            "content",
            List.of(),
            List.of()
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("OriginalContent cannot be null");
    }
    
    @Test
    @DisplayName("Should throw exception when refactored content is null")
    void shouldThrowExceptionWhenRefactoredContentIsNull() {
        // When/Then
        assertThatThrownBy(() -> new RefactoringChanges(
            "Test.java",
            "content",
            null,
            List.of(),
            List.of()
        )).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("RefactoredContent cannot be null");
    }
}

