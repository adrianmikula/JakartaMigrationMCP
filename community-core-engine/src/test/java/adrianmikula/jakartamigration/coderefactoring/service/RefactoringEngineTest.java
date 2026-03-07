package adrianmikula.jakartamigration.coderefactoring.service;

import adrianmikula.jakartamigration.coderefactoring.domain.*;
// Redundant import removed
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

    @Test
    @DisplayName("Should refactor JPA with MigrateJPA recipe")
    void shouldRefactorJPAWithMigrateJPARecipe() throws IOException {
        // Given
        Path testFile = tempDir.resolve("User.java");
        String originalContent = """
                package com.example;

                import javax.persistence.Entity;
                import javax.persistence.Id;
                import javax.persistence.Column;

                @Entity
                public class User {
                    @Id
                    private Long id;
                    @Column
                    private String name;
                }
                """;
        Files.writeString(testFile, originalContent);

        List<Recipe> recipes = List.of(Recipe.jpaRecipe());

        // When
        RefactoringChanges result = engine.refactorFile(testFile, recipes);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.hasChanges()).isTrue();
        assertThat(result.refactoredContent()).contains("jakarta.persistence");
        assertThat(result.refactoredContent()).doesNotContain("javax.persistence");
    }

    @Test
    @DisplayName("Should refactor Bean Validation with MigrateBeanValidation recipe")
    void shouldRefactorBeanValidationWithMigrateBeanValidationRecipe() throws IOException {
        // Given
        Path testFile = tempDir.resolve("User.java");
        String originalContent = """
                package com.example;

                import javax.validation.constraints.NotNull;
                import javax.validation.constraints.Size;

                public class User {
                    @NotNull
                    @Size(min = 1, max = 100)
                    private String name;
                }
                """;
        Files.writeString(testFile, originalContent);

        List<Recipe> recipes = List.of(Recipe.beanValidationRecipe());

        // When
        RefactoringChanges result = engine.refactorFile(testFile, recipes);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.hasChanges()).isTrue();
        assertThat(result.refactoredContent()).contains("jakarta.validation");
        assertThat(result.refactoredContent()).doesNotContain("javax.validation");
    }

    @Test
    @DisplayName("Should refactor Servlet with MigrateServlet recipe")
    void shouldRefactorServletWithMigrateServletRecipe() throws IOException {
        // Given
        Path testFile = tempDir.resolve("MyServlet.java");
        String originalContent = """
                package com.example;

                import javax.servlet.ServletException;
                import javax.servlet.http.HttpServlet;
                import javax.servlet.http.HttpServletRequest;
                import javax.servlet.http.HttpServletResponse;

                public class MyServlet extends HttpServlet {
                    @Override
                    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
                    }
                }
                """;
        Files.writeString(testFile, originalContent);

        List<Recipe> recipes = List.of(Recipe.servletRecipe());

        // When
        RefactoringChanges result = engine.refactorFile(testFile, recipes);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.hasChanges()).isTrue();
        assertThat(result.refactoredContent()).contains("jakarta.servlet");
        assertThat(result.refactoredContent()).doesNotContain("javax.servlet");
    }

    @Test
    @DisplayName("Should refactor CDI with MigrateCDI recipe")
    void shouldRefactorCDIWithMigrateCDIRecipe() throws IOException {
        // Given
        Path testFile = tempDir.resolve("MyBean.java");
        String originalContent = """
                package com.example;

                import javax.enterprise.context.ApplicationScoped;
                import javax.inject.Inject;

                @ApplicationScoped
                public class MyBean {
                    @Inject
                    private OtherBean other;
                }
                """;
        Files.writeString(testFile, originalContent);

        List<Recipe> recipes = List.of(Recipe.cdiRecipe());

        // When
        RefactoringChanges result = engine.refactorFile(testFile, recipes);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.hasChanges()).isTrue();
        assertThat(result.refactoredContent()).contains("jakarta.enterprise");
        assertThat(result.refactoredContent()).contains("jakarta.inject");
    }

    @Test
    @DisplayName("Should refactor JAX-RS with MigrateREST recipe")
    void shouldRefactorJAXRSWithMigrateRESTRecipe() throws IOException {
        // Given
        Path testFile = tempDir.resolve("MyResource.java");
        String originalContent = """
                package com.example;

                import javax.ws.rs.GET;
                import javax.ws.rs.Path;
                import javax.ws.rs.PathParam;

                @Path("/users")
                public class MyResource {
                    @GET
                    @Path("/{id}")
                    public String getUser(@PathParam("id") String id) {
                        return id;
                    }
                }
                """;
        Files.writeString(testFile, originalContent);

        List<Recipe> recipes = List.of(Recipe.restRecipe());

        // When
        RefactoringChanges result = engine.refactorFile(testFile, recipes);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.hasChanges()).isTrue();
        assertThat(result.refactoredContent()).contains("jakarta.ws.rs");
        assertThat(result.refactoredContent()).doesNotContain("javax.ws.rs");
    }

    @Test
    @DisplayName("Should refactor JAXB with MigrateJAXB recipe")
    void shouldRefactorJAXBWithMigrateJAXBRecipe() throws IOException {
        // Given
        Path testFile = tempDir.resolve("MyObject.java");
        String originalContent = """
                package com.example;

                import javax.xml.bind.annotation.XmlRootElement;

                @XmlRootElement
                public class MyObject {
                    private String name;
                }
                """;
        Files.writeString(testFile, originalContent);

        List<Recipe> recipes = List.of(Recipe.jaxbRecipe());

        // When
        RefactoringChanges result = engine.refactorFile(testFile, recipes);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.hasChanges()).isTrue();
        assertThat(result.refactoredContent()).contains("jakarta.xml.bind");
        assertThat(result.refactoredContent()).doesNotContain("javax.xml.bind");
    }

    @Test
    @DisplayName("Should refactor JMS with MigrateJMS recipe")
    void shouldRefactorJMSWithMigrateJMSRecipe() throws IOException {
        // Given
        Path testFile = tempDir.resolve("MessageBean.java");
        String originalContent = """
                package com.example;

                import javax.jms.Queue;
                import javax.jms.ConnectionFactory;

                public class MessageBean {
                    private Queue queue;
                    private ConnectionFactory factory;
                }
                """;
        Files.writeString(testFile, originalContent);

        List<Recipe> recipes = List.of(Recipe.jmsRecipe());

        // When
        RefactoringChanges result = engine.refactorFile(testFile, recipes);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.hasChanges()).isTrue();
        assertThat(result.refactoredContent()).contains("jakarta.jms");
        assertThat(result.refactoredContent()).doesNotContain("javax.jms");
    }

    @Test
    @DisplayName("Should refactor JSON-P with MigrateJSONP recipe")
    void shouldRefactorJSONPWithMigrateJSONPRecipe() throws IOException {
        // Given
        Path testFile = tempDir.resolve("JsonProcessor.java");
        String originalContent = """
                package com.example;

                import javax.json.Json;
                import javax.json.JsonObject;

                public class JsonProcessor {
                    public JsonObject createObject() {
                        return Json.createObjectBuilder().build();
                    }
                }
                """;
        Files.writeString(testFile, originalContent);

        List<Recipe> recipes = List.of(Recipe.jsonpRecipe());

        // When
        RefactoringChanges result = engine.refactorFile(testFile, recipes);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.hasChanges()).isTrue();
        assertThat(result.refactoredContent()).contains("jakarta.json");
        assertThat(result.refactoredContent()).doesNotContain("javax.json");
    }

    @Test
    @DisplayName("Should refactor Annotation with MigrateAnnotation recipe")
    void shouldRefactorAnnotationWithMigrateAnnotationRecipe() throws IOException {
        // Given
        Path testFile = tempDir.resolve("MyAnnotation.java");
        String originalContent = """
                package com.example;

                import javax.annotation.PostConstruct;
                import javax.annotation.Resource;

                public class MyBean {
                    @PostConstruct
                    public void init() {}

                    @Resource
                    private String name;
                }
                """;
        Files.writeString(testFile, originalContent);

        List<Recipe> recipes = List.of(Recipe.annotationRecipe());

        // When
        RefactoringChanges result = engine.refactorFile(testFile, recipes);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.hasChanges()).isTrue();
        assertThat(result.refactoredContent()).contains("jakarta.annotation");
        assertThat(result.refactoredContent()).doesNotContain("javax.annotation");
    }

    @Test
    @DisplayName("Should handle unknown recipe without crashing")
    void shouldHandleUnknownRecipeWithoutCrashing() throws IOException {
        // Given
        Path testFile = tempDir.resolve("Test.java");
        String originalContent = """
                package com.example;

                public class Test {}
                """;
        Files.writeString(testFile, originalContent);

        Recipe unknownRecipe = new Recipe("UnknownRecipe", "Test", "test", SafetyLevel.MEDIUM, true);
        List<Recipe> recipes = List.of(unknownRecipe);

        // When
        RefactoringChanges result = engine.refactorFile(testFile, recipes);

        // Then
        assertThat(result).isNotNull();
        // Should not crash, just return no changes
    }
}
