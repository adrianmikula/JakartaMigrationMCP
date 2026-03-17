package adrianmikula.jakartamigration.coderefactoring.service.impl;

import adrianmikula.jakartamigration.analysis.persistence.CentralMigrationAnalysisStore;
import adrianmikula.jakartamigration.analysis.persistence.SqliteMigrationAnalysisStore;
import adrianmikula.jakartamigration.coderefactoring.domain.RecipeCategory;
import adrianmikula.jakartamigration.coderefactoring.domain.RecipeDefinition;
import adrianmikula.jakartamigration.coderefactoring.domain.RecipeExecutionHistory;
import adrianmikula.jakartamigration.coderefactoring.domain.RecipeExecutionResult;
import adrianmikula.jakartamigration.coderefactoring.domain.RecipeType;
import adrianmikula.jakartamigration.coderefactoring.service.RecipeService;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openrewrite.config.Environment;
import org.openrewrite.Result;
import adrianmikula.jakartamigration.coderefactoring.service.util.OpenRewriteRecipeExecutor;

/**
 * Implementation of RecipeService for migration refactorings.
 */
@Slf4j
public class RecipeServiceImpl implements RecipeService {

    private final CentralMigrationAnalysisStore centralStore;
    private final SqliteMigrationAnalysisStore projectStore;

    public RecipeServiceImpl(CentralMigrationAnalysisStore centralStore, SqliteMigrationAnalysisStore projectStore) {
        this.centralStore = centralStore;
        this.projectStore = projectStore;
    }

    @Override
    public List<RecipeDefinition> getRecipes() {
        return centralStore.getRecipes();
    }

    @Override
    public List<RecipeDefinition> getRecipesByCategory(RecipeCategory category) {
        return centralStore.getRecipesByCategory(category.name());
    }

    @Override
    public RecipeExecutionResult applyRecipe(String recipeName, Path projectPath) {
        log.info("Applying recipe '{}' to project: {}", recipeName, projectPath);

        Optional<RecipeDefinition> recipeOpt = getRecipes().stream()
                .filter(r -> r.getName().equals(recipeName))
                .findFirst();

        if (recipeOpt.isEmpty()) {
            return new RecipeExecutionResult(false, 0, 0, Collections.emptyList(), "Recipe not found: " + recipeName,
                    null);
        }

        RecipeDefinition recipe = recipeOpt.get();
        RecipeExecutionResult result;

        if (recipe.getRecipeType() == RecipeType.OPENREWRITE) {
            result = applyOpenRewriteRecipe(recipe, projectPath);
        } else {
            result = applyRegexRecipe(recipe, projectPath);
        }

        return result;
    }

    private RecipeExecutionResult applyRegexRecipe(RecipeDefinition recipe, Path projectPath) {
        List<String> changedFiles = new ArrayList<>();
        int processedCount = 0;

        try {
            String patternStr = recipe.getPattern();
            String replacement = recipe.getReplacement();
            String filePattern = recipe.getFilePattern();

            if (patternStr == null || replacement == null) {
                return new RecipeExecutionResult(false, 0, 0, Collections.emptyList(),
                        "Regex recipe missing pattern or replacement", null);
            }

            Pattern pattern = Pattern.compile(patternStr);

            // Record execution start
            RecipeExecutionHistory history = RecipeExecutionHistory.builder()
                    .recipeName(recipe.getName())
                    .executedAt(Instant.now())
                    .success(false) // default until finished
                    .isUndo(false)
                    .build();

            long executionId = projectStore.saveRecipeExecution(projectPath, history);

            try (Stream<Path> paths = Files.walk(projectPath)) {
                List<Path> targetFiles = paths
                        .filter(Files::isRegularFile)
                        .filter(p -> {
                            Path relativePath = projectPath.relativize(p);
                            String pathStr = relativePath.toString().replace('\\', '/');
                            String patternGlob = (filePattern != null ? filePattern : "**/*").replace('\\', '/');

                            // Robust glob-to-regex for common patterns like **/*.java or pom.xml
                            String regex = patternGlob
                                    .replace(".", "\\.")
                                    .replace("**/", "(.*/)?")
                                    .replace("*", "[^/]*")
                                    .replace("[^/]*[^/]*", ".*");

                            boolean matches = pathStr.matches(regex);
                            log.debug("Matching file: '{}' vs pattern: '{}' (regex: '{}') -> {}",
                                    pathStr, patternGlob, regex, matches);
                            return matches;
                        })
                        .collect(Collectors.toList());

                processedCount = targetFiles.size();
                log.info("Found {} target files matching pattern", processedCount);

                for (Path file : targetFiles) {
                    Path relativePath = projectPath.relativize(file);
                    log.debug("Processing file: {}", relativePath);
                    String content = Files.readString(file);
                    if (pattern.matcher(content).find()) {
                        log.info("Match found in file: {}", relativePath);
                        // Store original content for undo
                        projectStore.saveRecipeChangedFile(executionId, relativePath.toString(),
                                content);

                        String newContent = pattern.matcher(content).replaceAll(replacement);
                        Files.writeString(file, newContent);
                        changedFiles.add(relativePath.toString());
                    } else {
                        log.debug("No regex match in file: {}", relativePath);
                    }
                }
            }

            // Update history success
            history.setSuccess(true);
            history.setMessage("Applied to " + changedFiles.size() + " files");
            projectStore.updateRecipeExecution(history);

            return new RecipeExecutionResult(true, processedCount, changedFiles.size(), changedFiles, null,
                    executionId);

        } catch (Exception e) {
            log.error("Error applying regex recipe: {}", e.getMessage(), e);
            return new RecipeExecutionResult(false, processedCount, 0, Collections.emptyList(), e.getMessage(), null);
        }
    }

    private RecipeExecutionResult applyOpenRewriteRecipe(RecipeDefinition recipe, Path projectPath) {
        String recipeName = recipe.getOpenRewriteRecipeName();
        if (recipeName == null || recipeName.isEmpty()) {
            return new RecipeExecutionResult(false, 0, 0, Collections.emptyList(),
                    "OpenRewrite recipe name missing in definition", null);
        }

        try {
            // 1. Load the recipe
            Environment env = Environment.builder()
                    .scanRuntimeClasspath()
                    .build();

            org.openrewrite.Recipe openRewriteRecipe = env.activateRecipes(recipeName);

            // 2. Initialize history
            RecipeExecutionHistory history = RecipeExecutionHistory.builder()
                    .recipeName(recipe.getName())
                    .executedAt(Instant.now())
                    .success(false)
                    .isUndo(false)
                    .build();

            long executionId = projectStore.saveRecipeExecution(projectPath, history);

            // 3. Run the recipe
            OpenRewriteRecipeExecutor executor = new OpenRewriteRecipeExecutor();
            List<Result> results = executor.runRecipe(openRewriteRecipe, projectPath);

            List<String> changedFiles = new ArrayList<>();
            for (Result result : results) {
                if (result.getAfter() != null && result.getBefore() != null) {
                    Path relPath = projectPath.relativize(result.getAfter().getSourcePath());
                    String originalContent = result.getBefore().printAll();

                    // Store for undo
                    projectStore.saveRecipeChangedFile(executionId, relPath.toString(), originalContent);

                    // Apply change
                    Files.writeString(result.getAfter().getSourcePath(), result.getAfter().printAll());
                    changedFiles.add(relPath.toString());
                }
            }

            // 4. Update history
            history.setSuccess(true);
            history.setMessage("OpenRewrite applied to " + changedFiles.size() + " files");
            projectStore.updateRecipeExecution(history);

            return new RecipeExecutionResult(true, results.size(), changedFiles.size(), changedFiles, null,
                    executionId);

        } catch (Exception e) {
            log.error("Error applying OpenRewrite recipe: {}", e.getMessage(), e);
            return new RecipeExecutionResult(false, 0, 0, Collections.emptyList(), e.getMessage(), null);
        }
    }

    @Override
    public RecipeExecutionResult undoRecipe(Long executionId, Path projectPath) {
        log.info("Undoing recipe execution: {}", executionId);

        try {
            List<Map<String, String>> changedFiles = projectStore.getChangedFiles(executionId);
            if (changedFiles.isEmpty()) {
                return new RecipeExecutionResult(false, 0, 0, Collections.emptyList(),
                        "No history found for execution ID: " + executionId, null);
            }

            // Record undo execution
            RecipeExecutionHistory originalHistory = projectStore.getRecipeHistory(projectPath).stream()
                    .filter(h -> h.getId().equals(executionId))
                    .findFirst().orElse(null);

            RecipeExecutionHistory undoHistory = RecipeExecutionHistory.builder()
                    .recipeName("Undo: " + (originalHistory != null ? originalHistory.getRecipeName() : "Unknown"))
                    .executedAt(Instant.now())
                    .success(true)
                    .isUndo(true)
                    .build();

            long undoId = projectStore.saveRecipeExecution(projectPath, undoHistory);

            for (Map<String, String> fileInfo : changedFiles) {
                Path filePath = projectPath.resolve(fileInfo.get("filePath"));
                String originalContent = fileInfo.get("originalContent");
                if (Files.exists(filePath)) {
                    Files.writeString(filePath, originalContent);
                }
            }

            // Link undo
            projectStore.linkUndoExecution(executionId, undoId);

            return new RecipeExecutionResult(true, changedFiles.size(), changedFiles.size(),
                    changedFiles.stream().map(m -> m.get("filePath")).collect(Collectors.toList()), null, undoId);

        } catch (Exception e) {
            log.error("Error undoing recipe: {}", e.getMessage(), e);
            return new RecipeExecutionResult(false, 0, 0, Collections.emptyList(), e.getMessage(), null);
        }
    }

    @Override
    public List<RecipeExecutionHistory> getHistory(Path projectPath) {
        return projectStore.getRecipeHistory(projectPath);
    }
}
