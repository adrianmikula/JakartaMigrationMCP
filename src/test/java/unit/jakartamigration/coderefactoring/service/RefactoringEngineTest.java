package unit.jakartamigration.coderefactoring.service;

import com.bugbounty.jakartamigration.coderefactoring.domain.*;
import com.bugbounty.jakartamigration.coderefactoring.service.RefactoringEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for RefactoringEngine.
 */
@DisplayName("RefactoringEngine Tests")
class RefactoringEngineTest {
    
    private RefactoringEngine engine;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        engine = new RefactoringEngine();
    }
    
    @Test
    @DisplayName("Should refactor Java file with Jakarta namespace recipe")
    void shouldRefactorJavaFileWithJakartaNamespaceRecipe() throws IOException {
        // Given
        Path testFile = tempDir.resolve("Test.java");
        String originalContent = """
            package com.example;
            
            import javax.servlet.ServletException;
            import javax.servlet.http.HttpServlet;
            
            public class Test extends HttpServlet {
                public void doGet() throws ServletException {
                    // Test
                }
            }
            """;
        Files.writeString(testFile, originalContent);
        
        List<Recipe> recipes = List.of(Recipe.jakartaNamespaceRecipe());
        
        // When
        RefactoringChanges result = engine.refactorFile(testFile, recipes);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.hasChanges()).isTrue();
        assertThat(result.refactoredContent()).contains("jakarta.servlet");
        assertThat(result.refactoredContent()).doesNotContain("javax.servlet");
        assertThat(result.changeCount()).isGreaterThan(0);
    }
    
    @Test
    @DisplayName("Should refactor persistence.xml with persistence recipe")
    void shouldRefactorPersistenceXmlWithPersistenceRecipe() throws IOException {
        // Given
        Path testFile = tempDir.resolve("persistence.xml");
        String originalContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <persistence xmlns="http://java.sun.com/xml/ns/persistence"
                         version="2.0">
            </persistence>
            """;
        Files.writeString(testFile, originalContent);
        
        List<Recipe> recipes = List.of(Recipe.persistenceXmlRecipe());
        
        // When
        RefactoringChanges result = engine.refactorFile(testFile, recipes);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.refactoredContent()).contains("https://jakarta.ee/xml/ns/persistence");
        assertThat(result.refactoredContent()).doesNotContain("http://java.sun.com/xml/ns/persistence");
    }
    
    @Test
    @DisplayName("Should refactor web.xml with web recipe")
    void shouldRefactorWebXmlWithWebRecipe() throws IOException {
        // Given
        Path testFile = tempDir.resolve("web.xml");
        String originalContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <web-app xmlns="http://java.sun.com/xml/ns/javaee"
                     version="3.0">
            </web-app>
            """;
        Files.writeString(testFile, originalContent);
        
        List<Recipe> recipes = List.of(Recipe.webXmlRecipe());
        
        // When
        RefactoringChanges result = engine.refactorFile(testFile, recipes);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.refactoredContent()).contains("https://jakarta.ee/xml/ns/jakartaee");
        assertThat(result.refactoredContent()).doesNotContain("http://java.sun.com/xml/ns/javaee");
    }
    
    @Test
    @DisplayName("Should throw exception when file path is null")
    void shouldThrowExceptionWhenFilePathIsNull() {
        // Given
        List<Recipe> recipes = List.of(Recipe.jakartaNamespaceRecipe());
        
        // When/Then
        assertThatThrownBy(() -> engine.refactorFile(null, recipes))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("FilePath cannot be null");
    }
    
    @Test
    @DisplayName("Should throw exception when recipes is null")
    void shouldThrowExceptionWhenRecipesIsNull() throws IOException {
        // Given
        Path testFile = tempDir.resolve("Test.java");
        Files.writeString(testFile, "public class Test {}");
        
        // When/Then
        assertThatThrownBy(() -> engine.refactorFile(testFile, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Recipes cannot be null or empty");
    }
    
    @Test
    @DisplayName("Should throw exception when recipes is empty")
    void shouldThrowExceptionWhenRecipesIsEmpty() throws IOException {
        // Given
        Path testFile = tempDir.resolve("Test.java");
        Files.writeString(testFile, "public class Test {}");
        
        // When/Then
        assertThatThrownBy(() -> engine.refactorFile(testFile, List.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Recipes cannot be null or empty");
    }
    
    @Test
    @DisplayName("Should throw exception when file does not exist")
    void shouldThrowExceptionWhenFileDoesNotExist() {
        // Given
        Path nonExistentFile = tempDir.resolve("NonExistent.java");
        List<Recipe> recipes = List.of(Recipe.jakartaNamespaceRecipe());
        
        // When/Then
        assertThatThrownBy(() -> engine.refactorFile(nonExistentFile, recipes))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("File does not exist");
    }
    
    @Test
    @DisplayName("Should handle file with no changes needed")
    void shouldHandleFileWithNoChangesNeeded() throws IOException {
        // Given
        Path testFile = tempDir.resolve("Test.java");
        String originalContent = """
            package com.example;
            
            public class Test {
                // No javax imports
            }
            """;
        Files.writeString(testFile, originalContent);
        
        List<Recipe> recipes = List.of(Recipe.jakartaNamespaceRecipe());
        
        // When
        RefactoringChanges result = engine.refactorFile(testFile, recipes);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.originalContent()).isEqualTo(originalContent);
        // May or may not have changes depending on implementation
    }
    
    @Test
    @DisplayName("Should apply multiple recipes in sequence")
    void shouldApplyMultipleRecipesInSequence() throws IOException {
        // Given
        Path testFile = tempDir.resolve("Test.java");
        String originalContent = """
            package com.example;
            
            import javax.servlet.ServletException;
            
            public class Test {
            }
            """;
        Files.writeString(testFile, originalContent);
        
        List<Recipe> recipes = List.of(
            Recipe.jakartaNamespaceRecipe(),
            Recipe.persistenceXmlRecipe() // Won't apply to Java file, but should not error
        );
        
        // When
        RefactoringChanges result = engine.refactorFile(testFile, recipes);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.refactoredContent()).contains("jakarta.servlet");
        assertThat(result.appliedRecipes().size()).isEqualTo(2);
    }
}

