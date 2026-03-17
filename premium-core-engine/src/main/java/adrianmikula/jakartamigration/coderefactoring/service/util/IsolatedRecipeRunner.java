package adrianmikula.jakartamigration.coderefactoring.service.util;

import org.openrewrite.Result;
import org.openrewrite.config.Environment;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class is designed to be loaded by an isolated ClassLoader.
 * It performs OpenRewrite operations without any knowledge of the IDE's
 * classloader.
 */
public class IsolatedRecipeRunner {

    /**
     * Executes an OpenRewrite recipe on a given project path.
     * Returns a map of results to avoid classloading issues with OpenRewrite types.
     * 
     * @param recipeName     The full name of the recipe to execute.
     * @param projectPathStr The string path to the project.
     * @return A map containing "success" (Boolean), "errorMessage" (String), and
     *         "changedFiles" (List<String>).
     */
    public Map<String, Object> runRecipe(String recipeName, String projectPathStr) {
        Map<String, Object> response = new HashMap<>();
        try {
            Path projectPath = Paths.get(projectPathStr);

            // 1. Initialize environment using the current classloader (which should be
            // isolated)
            Environment env = Environment.builder()
                    .scanRuntimeClasspath()
                    .build();

            // 2. Activate recipe
            org.openrewrite.Recipe recipe;
            try {
                recipe = env.activateRecipes(recipeName);
            } catch (Exception e) {
                List<String> available = env.listRecipes().stream()
                        .map(org.openrewrite.Recipe::getName)
                        .sorted()
                        .limit(50)
                        .collect(Collectors.toList());

                response.put("success", false);
                response.put("errorMessage", "Recipe not found: " + recipeName + ". Discovered " +
                        env.listRecipes().size() + " recipes. Top ones: " + available);
                return response;
            }

            // 3. Run recipe
            OpenRewriteRecipeExecutor executor = new OpenRewriteRecipeExecutor();
            List<Result> results = executor.runRecipe(recipe, projectPath);

            // 4. Collect changed files
            List<Map<String, String>> changedFiles = new ArrayList<>();
            for (Result result : results) {
                if (result.getAfter() != null && result.getBefore() != null) {
                    Path relPath = projectPath.relativize(result.getAfter().getSourcePath());

                    Map<String, String> change = new HashMap<>();
                    change.put("path", relPath.toString());
                    change.put("originalContent", result.getBefore().printAll());
                    change.put("newContent", result.getAfter().printAll());

                    changedFiles.add(change);
                }
            }

            response.put("success", true);
            response.put("changedFiles", changedFiles);
            response.put("resultsCount", results.size());

        } catch (Exception e) {
            response.put("success", false);
            response.put("errorMessage", e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        return response;
    }
}
