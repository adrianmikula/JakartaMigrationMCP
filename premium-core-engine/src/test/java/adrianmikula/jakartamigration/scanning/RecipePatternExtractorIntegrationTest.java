package adrianmikula.jakartamigration.scanning;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RecipePatternExtractor Integration Tests")
@Tag("slow")
class RecipePatternExtractorIntegrationTest {

    private RecipePatternExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new RecipePatternExtractor();
    }

    @Test
    @DisplayName("Should extract ChangeDependency blocks from recipe YAML")
    void shouldExtractChangeDependencyBlocks() {
        String yaml = """
            type: specs.openrewrite.org/v1beta/recipe
            name: org.openrewrite.java.migrate.jakarta.JakartaEE9
            recipeList:
              - org.openrewrite.java.ChangeDependency:
                  oldGroupId: javax.servlet
                  oldArtifactId: javax.servlet-api
                  oldVersion: "*"
                  newGroupId: jakarta.servlet
                  newArtifactId: jakarta.servlet-api
                  newVersion: "6.0.0"
              - org.openrewrite.java.ChangeDependency:
                  oldGroupId: javax.persistence
                  oldArtifactId: javax.persistence-api
                  oldVersion: "*"
                  newGroupId: jakarta.persistence
                  newArtifactId: jakarta.persistence-api
                  newVersion: "3.1.0"
            """;

        RecipePatternExtractor.RecipePatterns patterns = extractor.parseRecipeYaml(yaml);

        assertThat(patterns.coordinateMappings()).hasSize(2);

        Map<String, String> coordMap = patterns.toCoordinateMap();
        assertThat(coordMap).containsEntry("javax.servlet:javax.servlet-api", "jakarta.servlet:jakarta.servlet-api");
        assertThat(coordMap).containsEntry("javax.persistence:javax.persistence-api", "jakarta.persistence:jakarta.persistence-api");
    }

    @Test
    @DisplayName("Should extract ChangePackage blocks from recipe YAML")
    void shouldExtractChangePackageBlocks() {
        String yaml = """
            type: specs.openrewrite.org/v1beta/recipe
            name: org.openrewrite.java.migrate.jakarta.JakartaEE9
            recipeList:
              - org.openrewrite.java.ChangePackage:
                  oldPackageName: javax.servlet
                  newPackageName: jakarta.servlet
              - org.openrewrite.java.ChangePackage:
                  oldPackageName: javax.persistence
                  newPackageName: jakarta.persistence
              - org.openrewrite.java.ChangePackage:
                  oldPackageName: javax.annotation
                  newPackageName: jakarta.annotation
            """;

        RecipePatternExtractor.RecipePatterns patterns = extractor.parseRecipeYaml(yaml);

        assertThat(patterns.packageRenames()).hasSize(3);

        Map<String, String> packageMap = patterns.toPackageRenameMap();
        assertThat(packageMap).containsEntry("javax.servlet", "jakarta.servlet");
        assertThat(packageMap).containsEntry("javax.persistence", "jakarta.persistence");
        assertThat(packageMap).containsEntry("javax.annotation", "jakarta.annotation");
    }

    @Test
    @DisplayName("Should handle mixed ChangeDependency and ChangePackage blocks")
    void shouldHandleMixedBlocks() {
        String yaml = """
            type: specs.openrewrite.org/v1beta/recipe
            name: org.openrewrite.java.migrate.jakarta.JakartaEE9
            recipeList:
              - org.openrewrite.java.ChangeDependency:
                  oldGroupId: javax.servlet
                  oldArtifactId: javax.servlet-api
                  newGroupId: jakarta.servlet
                  newArtifactId: jakarta.servlet-api
              - org.openrewrite.java.ChangePackage:
                  oldPackageName: javax.servlet
                  newPackageName: jakarta.servlet
              - org.openrewrite.java.ChangeDependency:
                  oldGroupId: javax.ws.rs
                  oldArtifactId: javax.ws.rs-api
                  newGroupId: jakarta.ws.rs
                  newArtifactId: jakarta.ws.rs-api
              - org.openrewrite.java.ChangePackage:
                  oldPackageName: javax.ws.rs
                  newPackageName: jakarta.ws.rs
            """;

        RecipePatternExtractor.RecipePatterns patterns = extractor.parseRecipeYaml(yaml);

        assertThat(patterns.coordinateMappings()).hasSize(2);
        assertThat(patterns.packageRenames()).hasSize(2);
    }

    @Test
    @DisplayName("Should skip non-ChangeDependency/ChangePackage recipe types")
    void shouldSkipOtherRecipeTypes() {
        String yaml = """
            type: specs.openrewrite.org/v1beta/recipe
            name: org.openrewrite.java.migrate.jakarta.JakartaEE9
            recipeList:
              - org.openrewrite.java.ChangeMethodName:
                  methodPattern: javax.servlet.ServletResponse getWriter()
                  newMethodName: getWriter
              - org.openrewrite.java.ChangeDependency:
                  oldGroupId: javax.servlet
                  oldArtifactId: javax.servlet-api
                  newGroupId: jakarta.servlet
                  newArtifactId: jakarta.servlet-api
              - org.openrewrite.java.AddImport:
                  packageName: jakarta.servlet.annotation.WebServlet
            """;

        RecipePatternExtractor.RecipePatterns patterns = extractor.parseRecipeYaml(yaml);

        assertThat(patterns.coordinateMappings()).hasSize(1);
        assertThat(patterns.packageRenames()).isEmpty();
    }

    @Test
    @DisplayName("Should return empty patterns for empty YAML")
    void shouldReturnEmptyForEmptyYaml() {
        String yaml = """
            type: specs.openrewrite.org/v1beta/recipe
            name: org.openrewrite.java.migrate.jakarta.JakartaEE9
            recipeList: []
            """;

        RecipePatternExtractor.RecipePatterns patterns = extractor.parseRecipeYaml(yaml);

        assertThat(patterns.coordinateMappings()).isEmpty();
        assertThat(patterns.packageRenames()).isEmpty();
    }

    @Test
    @DisplayName("Should return empty patterns for blank YAML content")
    void shouldReturnEmptyForBlankYaml() {
        RecipePatternExtractor.RecipePatterns patterns = extractor.parseRecipeYaml("");

        assertThat(patterns.coordinateMappings()).isEmpty();
        assertThat(patterns.packageRenames()).isEmpty();
    }

    @Test
    @DisplayName("Should handle quoted values in YAML")
    void shouldHandleQuotedValues() {
        String yaml = """
            type: specs.openrewrite.org/v1beta/recipe
            name: org.openrewrite.java.migrate.jakarta.JakartaEE9
            recipeList:
              - org.openrewrite.java.ChangeDependency:
                  oldGroupId: "javax.servlet"
                  oldArtifactId: "javax.servlet-api"
                  oldVersion: "*"
                  newGroupId: "jakarta.servlet"
                  newArtifactId: "jakarta.servlet-api"
                  newVersion: "6.0.0"
            """;

        RecipePatternExtractor.RecipePatterns patterns = extractor.parseRecipeYaml(yaml);

        assertThat(patterns.coordinateMappings()).hasSize(1);
        Map<String, String> coordMap = patterns.toCoordinateMap();
        assertThat(coordMap).containsEntry("javax.servlet:javax.servlet-api", "jakarta.servlet:jakarta.servlet-api");
    }

    @Test
    @DisplayName("Should build coordinate map with correct key-value format")
    void shouldBuildCoordinateMapCorrectly() {
        String yaml = """
            type: specs.openrewrite.org/v1beta/recipe
            name: org.openrewrite.java.migrate.jakarta.JakartaEE9
            recipeList:
              - org.openrewrite.java.ChangeDependency:
                  oldGroupId: javax.validation
                  oldArtifactId: validation-api
                  newGroupId: jakarta.validation
                  newArtifactId: jakarta.validation-api
            """;

        RecipePatternExtractor.RecipePatterns patterns = extractor.parseRecipeYaml(yaml);
        Map<String, String> coordMap = patterns.toCoordinateMap();

        assertThat(coordMap).hasSize(1);
        assertThat(coordMap).containsEntry("javax.validation:validation-api", "jakarta.validation:jakarta.validation-api");
    }

    @Test
    @DisplayName("Should build package rename map with correct key-value format")
    void shouldBuildPackageRenameMapCorrectly() {
        String yaml = """
            type: specs.openrewrite.org/v1beta/recipe
            name: org.openrewrite.java.migrate.jakarta.JakartaEE9
            recipeList:
              - org.openrewrite.java.ChangePackage:
                  oldPackageName: javax.ejb
                  newPackageName: jakarta.ejb
            """;

        RecipePatternExtractor.RecipePatterns patterns = extractor.parseRecipeYaml(yaml);
        Map<String, String> packageMap = patterns.toPackageRenameMap();

        assertThat(packageMap).hasSize(1);
        assertThat(packageMap).containsEntry("javax.ejb", "jakarta.ejb");
    }

    @Test
    @DisplayName("Should handle multiple ChangeDependency blocks in sequence")
    void shouldHandleSequentialChangeDependencyBlocks() {
        String yaml = """
            type: specs.openrewrite.org/v1beta/recipe
            name: org.openrewrite.java.migrate.jakarta.JakartaEE9
            recipeList:
              - org.openrewrite.java.ChangeDependency:
                  oldGroupId: javax.servlet
                  oldArtifactId: javax.servlet-api
                  newGroupId: jakarta.servlet
                  newArtifactId: jakarta.servlet-api
              - org.openrewrite.java.ChangeDependency:
                  oldGroupId: javax.annotation
                  oldArtifactId: javax.annotation-api
                  newGroupId: jakarta.annotation
                  newArtifactId: jakarta.annotation-api
              - org.openrewrite.java.ChangeDependency:
                  oldGroupId: javax.inject
                  oldArtifactId: javax.inject
                  newGroupId: jakarta.inject
                  newArtifactId: jakarta.inject-api
              - org.openrewrite.java.ChangeDependency:
                  oldGroupId: javax.enterprise
                  oldArtifactId: cdi-api
                  newGroupId: jakarta.enterprise
                  newArtifactId: jakarta.enterprise.cdi-api
            """;

        RecipePatternExtractor.RecipePatterns patterns = extractor.parseRecipeYaml(yaml);

        assertThat(patterns.coordinateMappings()).hasSize(4);
        Map<String, String> coordMap = patterns.toCoordinateMap();
        assertThat(coordMap).containsKey("javax.servlet:javax.servlet-api");
        assertThat(coordMap).containsKey("javax.annotation:javax.annotation-api");
        assertThat(coordMap).containsKey("javax.inject:javax.inject");
        assertThat(coordMap).containsKey("javax.enterprise:cdi-api");
    }

    @Test
    @DisplayName("Should return empty patterns via static factory")
    void shouldReturnEmptyPatternsFromFactory() {
        RecipePatternExtractor.RecipePatterns empty = RecipePatternExtractor.RecipePatterns.empty();

        assertThat(empty.coordinateMappings()).isEmpty();
        assertThat(empty.packageRenames()).isEmpty();
        assertThat(empty.fetchedAt()).isNull();
    }
}
