package adrianmikula.jakartamigration.coderefactoring.service.integration;

import adrianmikula.jakartamigration.analysis.persistence.CentralMigrationAnalysisStore;
import adrianmikula.jakartamigration.analysis.persistence.SqliteMigrationAnalysisStore;
import adrianmikula.jakartamigration.coderefactoring.domain.RecipeCategory;
import adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition;
import adrianmikula.jakartamigration.coderefactoring.domain.RecipeExecutionResult;
import adrianmikula.jakartamigration.coderefactoring.domain.RecipeExecutionHistory;
import adrianmikula.jakartamigration.coderefactoring.domain.RecipeType;
import adrianmikula.jakartamigration.coderefactoring.service.RecipeService;
import adrianmikula.jakartamigration.coderefactoring.service.impl.RecipeServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("slow")
public class RefactorRecipeIntegrationTest {

    @TempDir
    Path tempDir;

    private CentralMigrationAnalysisStore centralStore;
    private SqliteMigrationAnalysisStore projectStore;
    private RecipeService recipeService;

    @BeforeEach
    void setUp() throws IOException {
        Path projectPath = tempDir.resolve("integration-project");
        Files.createDirectories(projectPath);

        centralStore = new CentralMigrationAnalysisStore(tempDir.resolve("central.db"));
        projectStore = new SqliteMigrationAnalysisStore(projectPath);
        recipeService = new RecipeServiceImpl(centralStore, projectStore);

        seedRegexRecipes();
    }

    private void seedRegexRecipes() {
        centralStore.saveRecipe(RecipeDefinition.builder()
            .name("MigrateServlet")
            .description("Converts javax.servlet imports to jakarta.servlet")
            .category(RecipeCategory.WEB)
            .recipeType(RecipeType.REGEX)
            .pattern("javax\\.servlet")
            .replacement("jakarta.servlet")
            .filePattern("**/*.java")
            .reversible(true)
            .build());

        centralStore.saveRecipe(RecipeDefinition.builder()
            .name("MigrateJPA")
            .description("Converts javax.persistence imports to jakarta.persistence")
            .category(RecipeCategory.DATABASE)
            .recipeType(RecipeType.REGEX)
            .pattern("javax\\.persistence")
            .replacement("jakarta.persistence")
            .filePattern("**/*.java")
            .reversible(true)
            .build());

        centralStore.saveRecipe(RecipeDefinition.builder()
            .name("MigrateBeanValidation")
            .description("Converts javax.validation imports to jakarta.validation")
            .category(RecipeCategory.APIS)
            .recipeType(RecipeType.REGEX)
            .pattern("javax\\.validation")
            .replacement("jakarta.validation")
            .filePattern("**/*.java")
            .reversible(true)
            .build());

        centralStore.saveRecipe(RecipeDefinition.builder()
            .name("MigrateCDI")
            .description("Converts javax.inject imports to jakarta.inject")
            .category(RecipeCategory.CDI)
            .recipeType(RecipeType.REGEX)
            .pattern("javax\\.inject")
            .replacement("jakarta.inject")
            .filePattern("**/*.java")
            .reversible(true)
            .build());

        centralStore.saveRecipe(RecipeDefinition.builder()
            .name("MigrateEJB")
            .description("Converts javax.ejb imports to jakarta.ejb")
            .category(RecipeCategory.OTHER)
            .recipeType(RecipeType.REGEX)
            .pattern("javax\\.ejb")
            .replacement("jakarta.ejb")
            .filePattern("**/*.java")
            .reversible(true)
            .build());

        centralStore.saveRecipe(RecipeDefinition.builder()
            .name("MigrateREST")
            .description("Converts javax.ws.rs imports to jakarta.ws.rs")
            .category(RecipeCategory.APIS)
            .recipeType(RecipeType.REGEX)
            .pattern("javax\\.ws\\.rs")
            .replacement("jakarta.ws.rs")
            .filePattern("**/*.java")
            .reversible(true)
            .build());
    }

    @Test
    @DisplayName("Should migrate javax.servlet to jakarta.servlet in J2EE7Samples")
    void shouldMigrateServletInJ2EESamples() throws IOException {
        Path sampleDir = downloadExample("J2EE7 Samples");

        RecipeDefinition recipe = findRecipe("MigrateServlet");
        RecipeExecutionResult result = recipeService.applyRecipe(recipe.getName(), sampleDir);

        assertThat(result.success()).isTrue();
        assertThat(result.filesChanged()).isGreaterThan(0);

        for (String changedFile : result.changedFilePaths()) {
            String content = Files.readString(sampleDir.resolve(changedFile));
            assertThat(content)
                .as("Changed file %s should not contain unmigrated javax.servlet imports", changedFile)
                .doesNotContain("import javax.servlet.");
        }
    }

    @Test
    @DisplayName("Should migrate javax.persistence to jakarta.persistence in J2EE7Samples")
    void shouldMigrateJpaInJ2EESamples() throws IOException {
        Path sampleDir = downloadExample("J2EE7 Samples");

        RecipeDefinition recipe = findRecipe("MigrateJPA");
        RecipeExecutionResult result = recipeService.applyRecipe(recipe.getName(), sampleDir);

        assertThat(result.success()).isTrue();
        assertThat(result.filesChanged()).isGreaterThan(0);

        for (String changedFile : result.changedFilePaths()) {
            String content = Files.readString(sampleDir.resolve(changedFile));
            assertThat(content)
                .as("Changed file %s should not contain unmigrated javax.persistence imports", changedFile)
                .doesNotContain("import javax.persistence.");
        }
    }

    @Test
    @DisplayName("Should migrate javax.validation to jakarta.validation in JavaxValidation repo")
    void shouldMigrateValidationInJavaxValidationExample() throws IOException {
        Path sampleDir = downloadExample("javax-validation");

        RecipeDefinition recipe = findRecipe("MigrateBeanValidation");
        RecipeExecutionResult result = recipeService.applyRecipe(recipe.getName(), sampleDir);

        assertThat(result.success()).isTrue();
        assertThat(result.filesChanged()).isGreaterThan(0);

        for (String changedFile : result.changedFilePaths()) {
            String content = Files.readString(sampleDir.resolve(changedFile));
            assertThat(content)
                .as("Changed file %s should not contain unmigrated javax.validation imports", changedFile)
                .doesNotContain("import javax.validation.");
        }
    }

    @Test
    @DisplayName("Should migrate javax.inject to jakarta.inject in J2EE7Samples")
    void shouldMigrateCdiInJ2EESamples() throws IOException {
        Path sampleDir = downloadExample("J2EE7 Samples");

        RecipeDefinition recipe = findRecipe("MigrateCDI");
        RecipeExecutionResult result = recipeService.applyRecipe(recipe.getName(), sampleDir);

        assertThat(result.success()).isTrue();
        assertThat(result.filesChanged()).isGreaterThan(0);

        for (String changedFile : result.changedFilePaths()) {
            String content = Files.readString(sampleDir.resolve(changedFile));
            assertThat(content)
                .as("Changed file %s should not contain unmigrated javax.inject imports", changedFile)
                .doesNotContain("import javax.inject.");
        }
    }

    @Test
    @DisplayName("Should migrate javax.ejb to jakarta.ejb in J2EE7Samples")
    void shouldMigrateEjbInJ2EESamples() throws IOException {
        Path sampleDir = downloadExample("J2EE7 Samples");

        RecipeDefinition recipe = findRecipe("MigrateEJB");
        RecipeExecutionResult result = recipeService.applyRecipe(recipe.getName(), sampleDir);

        assertThat(result.success()).isTrue();
        assertThat(result.filesChanged()).isGreaterThan(0);

        for (String changedFile : result.changedFilePaths()) {
            String content = Files.readString(sampleDir.resolve(changedFile));
            assertThat(content)
                .as("Changed file %s should not contain unmigrated javax.ejb imports", changedFile)
                .doesNotContain("import javax.ejb.");
        }
    }

    @Test
    @DisplayName("Should migrate javax.ws.rs to jakarta.ws.rs in J2EE7Samples")
    void shouldMigrateRestInJ2EESamples() throws IOException {
        Path sampleDir = downloadExample("J2EE7 Samples");

        RecipeDefinition recipe = findRecipe("MigrateREST");
        RecipeExecutionResult result = recipeService.applyRecipe(recipe.getName(), sampleDir);

        assertThat(result.success()).isTrue();
        assertThat(result.filesChanged()).isGreaterThan(0);

        for (String changedFile : result.changedFilePaths()) {
            String content = Files.readString(sampleDir.resolve(changedFile));
            assertThat(content)
                .as("Changed file %s should not contain unmigrated javax.ws.rs imports", changedFile)
                .doesNotContain("import javax.ws.rs.");
        }
    }

    @Test
    @DisplayName("Should support undo after applying recipe to J2EE7Samples")
    void shouldSupportUndoAfterApplyToJ2EESamples() throws IOException {
        Path sampleDir = downloadExample("J2EE7 Samples");

        List<Path> javaFiles = Files.walk(sampleDir)
            .filter(Files::isRegularFile)
            .filter(path -> path.toString().endsWith(".java"))
            .filter(path -> {
                try {
                    String content = Files.readString(path);
                    return content.contains("javax.ws.rs");
                } catch (IOException e) {
                    return false;
                }
            })
            .toList();

        assertThat(javaFiles).as("J2EE7Samples should contain files with javax.ws.rs").isNotEmpty();
        Path targetFile = javaFiles.get(0);
        String original = Files.readString(targetFile);

        RecipeDefinition recipe = findRecipe("MigrateREST");
        RecipeExecutionResult applyResult = recipeService.applyRecipe(recipe.getName(), sampleDir);
        assertThat(applyResult.success()).isTrue();
        assertThat(applyResult.filesChanged()).isGreaterThan(0);
        assertThat(applyResult.executionId()).isNotNull();

        String afterApply = Files.readString(targetFile);
        assertThat(afterApply)
            .as("Applied recipe should modify javax.ws.rs references in %s", targetFile)
            .isNotEqualTo(original);

        RecipeExecutionResult undoResult = recipeService.undoRecipe(applyResult.executionId(), sampleDir);
        assertThat(undoResult.success()).isTrue();

        String afterUndo = Files.readString(targetFile);
        assertThat(afterUndo)
            .as("Undo should restore original content of %s", targetFile)
            .isEqualTo(original);
    }

    @Test
    @DisplayName("Should record execution history when applying recipes to J2EE7Samples")
    void shouldRecordExecutionHistoryForJ2EESamples() throws IOException {
        Path sampleDir = downloadExample("J2EE7 Samples");

        RecipeDefinition recipe = findRecipe("MigrateREST");
        recipeService.applyRecipe(recipe.getName(), sampleDir);

        List<RecipeExecutionHistory> history = recipeService.getHistory(sampleDir);
        assertThat(history).hasSize(1);
        assertThat(history.get(0).getRecipeName()).isEqualTo(recipe.getName());
        assertThat(history.get(0).isSuccess()).isTrue();
        assertThat(history.get(0).isUndo()).isFalse();
    }

    @Test
    @DisplayName("Should provide recipes with project-specific status from history for J2EE7Samples")
    void shouldProvideRecipesWithProjectSpecificStatusForJ2EESamples() throws IOException {
        Path sampleDir = downloadExample("J2EE7 Samples");

        RecipeDefinition recipe = findRecipe("MigrateREST");
        recipeService.applyRecipe(recipe.getName(), sampleDir);

        List<RecipeDefinition> recipes = recipeService.getRecipes(sampleDir);
        RecipeDefinition applied = recipes.stream()
            .filter(r -> r.getName().equals(recipe.getName()))
            .findFirst()
            .orElseThrow();

        assertThat(applied.getStatus()).isEqualTo(RecipeDefinition.RecipeStatus.RUN_SUCCESS);
        assertThat(applied.getLastRunDate()).isNotNull();
    }

    @Test
    @DisplayName("Should list recipes by category with project context for J2EE7Samples")
    void shouldListRecipesByCategoryWithProjectContextForJ2EESamples() throws IOException {
        Path sampleDir = downloadExample("J2EE7 Samples");

        List<RecipeDefinition> webRecipes = recipeService.getRecipesByCategory(RecipeCategory.WEB, sampleDir);
        assertThat(webRecipes).isNotEmpty();

        List<RecipeDefinition> apiRecipes = recipeService.getRecipesByCategory(RecipeCategory.APIS, sampleDir);
        assertThat(apiRecipes).isNotEmpty();
    }

    private Path downloadExample(String projectName) throws IOException {
        String repoUrl = findProjectUrl(projectName);
        Path extractDir = tempDir.resolve("examples").resolve(repoNameFromUrl(repoUrl));
        if (Files.exists(extractDir)) {
            return resolveProjectRoot(extractDir);
        }

        if (repoUrl.contains("/tree/")) {
            String zipUrl = toArchiveZipUrl(repoUrl);
            return downloadAndExtract(zipUrl, extractDir);
        }

        IOException lastException = null;
        for (String branch : new String[]{"main", "master", "develop"}) {
            String zipUrl = repoUrl + "/archive/refs/heads/" + branch + ".zip";
            try {
                return downloadAndExtract(zipUrl, extractDir);
            } catch (IOException e) {
                lastException = e;
            }
        }

        throw new IOException("Failed to download repo from known branches for: " + repoUrl, lastException);
    }

    private Path downloadAndExtract(String zipUrl, Path extractDir) throws IOException {
        URL url = new URL(zipUrl);
        try (InputStream in = url.openStream();
             ZipInputStream zis = new ZipInputStream(in)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path out = extractDir.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(out);
                } else {
                    Files.createDirectories(out.getParent());
                    Files.copy(zis, out);
                }
                zis.closeEntry();
            }
        }

        return resolveProjectRoot(extractDir);
    }

    private String findProjectUrl(String projectName) throws IOException {
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        Map<String, Object> data;
        try (InputStream is = getClass().getResourceAsStream("/examples.yaml")) {
            if (is == null) {
                throw new RuntimeException("examples.yaml not found on classpath");
            }
            data = yamlMapper.readValue(is, Map.class);
        }

        String target = projectName.toLowerCase();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> map) {
                        Object nameObj = map.get("name");
                        if (nameObj instanceof String name && name.toLowerCase().contains(target)) {
                            Object urlObj = map.get("url");
                            if (urlObj instanceof String url) {
                                return url;
                            }
                        }
                    }
                }
            }
        }

        throw new IllegalArgumentException("Project not found in examples.yaml: " + projectName);
    }

    private String repoNameFromUrl(String url) {
        String clean = url.replaceAll("/$", "");
        int lastSlash = clean.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < clean.length() - 1) {
            return clean.substring(lastSlash + 1);
        }
        return clean;
    }

    private String toArchiveZipUrl(String repoUrl) {
        String branch = repoUrl.substring(repoUrl.indexOf("/tree/") + 6);
        int nextSlash = branch.indexOf('/');
        if (nextSlash > 0) {
            branch = branch.substring(0, nextSlash);
        }
        String base = repoUrl.substring(0, repoUrl.indexOf("/tree/"));
        return base + "/archive/refs/heads/" + branch + ".zip";
    }

    private Path resolveProjectRoot(Path extractDir) throws IOException {
        try (var stream = Files.list(extractDir)) {
            List<Path> children = stream.toList();
            if (children.size() == 1 && Files.isDirectory(children.get(0))) {
                return children.get(0);
            }
        }
        return extractDir;
    }

    private RecipeDefinition findRecipe(String name) {
        List<RecipeDefinition> recipes = centralStore.getRecipes();
        return recipes.stream()
            .filter(r -> r.getName().equals(name))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Recipe not found: " + name));
    }
}
