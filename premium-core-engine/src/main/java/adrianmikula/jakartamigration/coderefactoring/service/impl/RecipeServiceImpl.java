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

import java.net.URL;
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
import adrianmikula.jakartamigration.coderefactoring.service.util.FilteringClassLoader;
import adrianmikula.jakartamigration.coderefactoring.service.util.IsolatedClassLoader;

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
    public List<RecipeDefinition> getRecipes(Path projectPath) {
        List<RecipeDefinition> recipes = centralStore.getRecipes();
        enrichRecipesWithStatus(recipes, projectPath);
        return recipes;
    }

    @Override
    public List<RecipeDefinition> getRecipesByCategory(RecipeCategory category, Path projectPath) {
        List<RecipeDefinition> recipes = centralStore.getRecipesByCategory(category.name());
        enrichRecipesWithStatus(recipes, projectPath);
        return recipes;
    }

    private void enrichRecipesWithStatus(List<RecipeDefinition> recipes, Path projectPath) {
        List<RecipeExecutionHistory> history = projectStore.getRecipeHistory(projectPath);

        for (RecipeDefinition recipe : recipes) {
            // Find most recent execution for this recipe
            Optional<RecipeExecutionHistory> latestExec = history.stream()
                    .filter(h -> h.getRecipeName().equals(recipe.getName()))
                    .filter(h -> !h.isUndo()) // Ignore undo actions as the "last run" of the recipe itself
                    .max(Comparator.comparing(RecipeExecutionHistory::getExecutedAt));

            if (latestExec.isPresent()) {
                RecipeExecutionHistory exec = latestExec.get();
                recipe.setLastRunDate(exec.getExecutedAt());

                if (!exec.isSuccess()) {
                    recipe.setStatus(RecipeDefinition.RecipeStatus.RUN_FAILED);
                } else {
                    // Check if it was undone
                    boolean isUndone = history.stream()
                            .anyMatch(h -> h.isUndo() && h.getRecipeName().contains(recipe.getName()));

                    if (isUndone) {
                        recipe.setStatus(RecipeDefinition.RecipeStatus.RUN_UNDONE);
                    } else {
                        recipe.setStatus(RecipeDefinition.RecipeStatus.RUN_SUCCESS);
                    }
                }
            } else {
                recipe.setStatus(RecipeDefinition.RecipeStatus.NEVER_RUN);
            }
        }
    }

    @Override
    public RecipeExecutionResult applyRecipe(String recipeName, Path projectPath) {
        log.info("Applying recipe '{}' to project: {}", recipeName, projectPath);

        Optional<RecipeDefinition> recipeOpt = getRecipes(projectPath).stream()
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
        // Initialize history record at the very beginning
        RecipeExecutionHistory history = RecipeExecutionHistory.builder()
                .recipeName(recipe.getName())
                .executedAt(Instant.now())
                .success(false) // default until finished
                .isUndo(false)
                .build();

        long executionId = projectStore.saveRecipeExecution(projectPath, history);
        List<String> changedFiles = new ArrayList<>();
        int processedCount = 0;

        try {
            String patternStr = recipe.getPattern();
            String replacement = recipe.getReplacement();
            String filePattern = recipe.getFilePattern();

            if (patternStr == null || replacement == null) {
                String msg = "Regex recipe missing pattern or replacement";
                if (executionId != -1) {
                    history.setMessage(msg);
                    projectStore.updateRecipeExecution(history);
                }
                return new RecipeExecutionResult(false, 0, 0, Collections.emptyList(), msg,
                        executionId != -1 ? executionId : null);
            }

            Pattern pattern = Pattern.compile(patternStr);

            try (Stream<Path> paths = Files.walk(projectPath)) {
                List<Path> targetFiles = paths
                        .filter(Files::isRegularFile)
                        .filter(p -> {
                            Path relativePath = projectPath.relativize(p);
                            String pathStr = relativePath.toString().replace('\\', '/');
                            String patternGlob = (filePattern != null ? filePattern : "**/*").replace('\\', '/');

                            String regex = patternGlob
                                    .replace(".", "\\.")
                                    .replace("**/", "(.*/)?")
                                    .replace("*", "[^/]*")
                                    .replace("[^/]*[^/]*", ".*");

                            boolean matches = pathStr.matches(regex);
                            return matches;
                        })
                        .collect(Collectors.toList());

                processedCount = targetFiles.size();
                for (Path file : targetFiles) {
                    Path relativePath = projectPath.relativize(file);
                    String content = Files.readString(file);
                    if (pattern.matcher(content).find()) {
                        // Store original content for undo
                        if (executionId != -1) {
                            projectStore.saveRecipeChangedFile(executionId, relativePath.toString(), content);
                        }

                        String newContent = pattern.matcher(content).replaceAll(replacement);
                        Files.writeString(file, newContent);
                        changedFiles.add(relativePath.toString());
                    }
                }
            }

            // Update history success
            history.setSuccess(true);
            history.setMessage("Applied to " + changedFiles.size() + " files");
            if (executionId != -1) {
                projectStore.updateRecipeExecution(history);
            }

            return new RecipeExecutionResult(true, processedCount, changedFiles.size(), changedFiles, null,
                    executionId != -1 ? executionId : null);

        } catch (Exception e) {
            log.error("Error applying regex recipe: {}", e.getMessage(), e);
            if (executionId != -1) {
                history.setSuccess(false);
                history.setMessage("Error: " + e.getMessage());
                projectStore.updateRecipeExecution(history);
            }
            return new RecipeExecutionResult(false, processedCount, 0, Collections.emptyList(), e.getMessage(),
                    executionId != -1 ? executionId : null);
        }
    }

    private RecipeExecutionResult applyOpenRewriteRecipe(RecipeDefinition recipe, Path projectPath) {
        String recipeName = recipe.getOpenRewriteRecipeName();

        // 1. Initialize history record BEFORE anything else to ensure failures are
        // recorded
        RecipeExecutionHistory history = RecipeExecutionHistory.builder()
                .recipeName(recipe.getName())
                .executedAt(Instant.now())
                .success(false)
                .isUndo(false)
                .build();

        long executionId = projectStore.saveRecipeExecution(projectPath, history);

        if (recipeName == null || recipeName.isEmpty()) {
            String msg = "OpenRewrite recipe name missing in definition";
            if (executionId != -1) {
                history.setMessage(msg);
                projectStore.updateRecipeExecution(history);
            }
            return new RecipeExecutionResult(false, 0, 0, Collections.emptyList(), msg,
                    executionId != -1 ? executionId : null);
        }

        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            // 2. Extract URLs from our PluginClassLoader (represents our plugin's lib
            // folder)
            ClassLoader pluginClassLoader = getClass().getClassLoader();
            java.net.URL[] pluginUrls = extractUrls(pluginClassLoader);

            if (pluginUrls.length == 0) {
                throw new RuntimeException("Could not extract URLs from plugin classloader. Cannot isolate.");
            }

            // 3. Create strictly isolated classloader (parent is platform loader)
            IsolatedClassLoader isolatedLoader = new IsolatedClassLoader(pluginUrls);
            Thread.currentThread().setContextClassLoader(isolatedLoader);

            log.debug("Initializing OpenRewrite via IsolatedRecipeRunner for recipe: {}", recipeName);

            // 4. Load runner from isolated classloader and invoke via reflection
            Class<?> runnerClass = isolatedLoader
                    .loadClass("adrianmikula.jakartamigration.coderefactoring.service.util.IsolatedRecipeRunner");
            Object runner = runnerClass.getDeclaredConstructor().newInstance();
            java.lang.reflect.Method runMethod = runnerClass.getMethod("runRecipe", String.class, String.class);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = (Map<String, Object>) runMethod.invoke(runner, recipeName,
                    projectPath.toString());

            boolean success = (Boolean) response.get("success");
            if (!success) {
                String errorMsg = (String) response.get("errorMessage");
                log.error("Isolated execution failed: {}", errorMsg);
                if (executionId != -1) {
                    history.setSuccess(false);
                    history.setMessage("Error: " + errorMsg);
                    projectStore.updateRecipeExecution(history);
                }
                return new RecipeExecutionResult(false, 0, 0, Collections.emptyList(), errorMsg,
                        executionId != -1 ? executionId : null);
            }

            @SuppressWarnings("unchecked")
            List<Map<String, String>> changedFilesRaw = (List<Map<String, String>>) response.get("changedFiles");
            int resultsCount = (Integer) response.get("resultsCount");

            List<String> changedFiles = new ArrayList<>();
            for (Map<String, String> change : changedFilesRaw) {
                String relPath = change.get("path");
                String originalContent = change.get("originalContent");
                String newContent = change.get("newContent");

                // Store for undo
                if (executionId != -1) {
                    projectStore.saveRecipeChangedFile(executionId, relPath, originalContent);
                }

                // Apply change
                Files.writeString(projectPath.resolve(relPath), newContent);
                changedFiles.add(relPath);
            }

            // 5. Update history on success
            history.setSuccess(true);
            history.setMessage("OpenRewrite applied to " + changedFiles.size() + " files");
            if (executionId != -1) {
                projectStore.updateRecipeExecution(history);
            }

            return new RecipeExecutionResult(true, resultsCount, changedFiles.size(), changedFiles, null,
                    executionId != -1 ? executionId : null);

        } catch (Exception e) {
            log.error("Error applying OpenRewrite recipe via isolation: {}",
                    e.getClass().getSimpleName() + ": " + e.getMessage(), e);
            if (executionId != -1) {
                history.setSuccess(false);
                history.setMessage("Error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                projectStore.updateRecipeExecution(history);
            }
            return new RecipeExecutionResult(false, 0, 0, Collections.emptyList(),

                    e.getClass().getSimpleName() + ": " + e.getMessage(), executionId != -1 ? executionId : null);
        } finally {
            // Restore original classloader
            Thread.currentThread().setContextClassLoader(originalClassLoader);
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

    private java.net.URL[] extractUrls(ClassLoader classLoader) {
        try {
            // Try getURLs (standard for URLClassLoader)
            try {
                java.lang.reflect.Method getURLsMethod = classLoader.getClass().getMethod("getURLs");
                return (java.net.URL[]) getURLsMethod.invoke(classLoader);
            } catch (NoSuchMethodException e) {
                // Try getUrls (common in some IntelliJ versions/wrappers)
                java.lang.reflect.Method getUrlsMethod = classLoader.getClass().getMethod("getUrls");
                Object result = getUrlsMethod.invoke(classLoader);
                if (result instanceof List) {
                    @SuppressWarnings("unchecked")
                    List<java.net.URL> list = (List<java.net.URL>) result;
                    return list.toArray(new java.net.URL[0]);
                } else if (result instanceof java.net.URL[]) {
                    return (java.net.URL[]) result;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract URLs from classloader: {}", e.getMessage());
        }
        return new java.net.URL[0];
    }
}
