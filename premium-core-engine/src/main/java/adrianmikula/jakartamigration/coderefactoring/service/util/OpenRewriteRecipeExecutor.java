package adrianmikula.jakartamigration.coderefactoring.service.util;

import lombok.extern.slf4j.Slf4j;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Result;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Helper class for executing OpenRewrite recipes programmatically.
 */
@Slf4j
public class OpenRewriteRecipeExecutor {

    /**
     * Executes an OpenRewrite recipe on a project.
     * 
     * @param recipe      The OpenRewrite recipe to run
     * @param projectPath The root path of the project
     * @return List of results containing changed files
     */
    public List<Result> runRecipe(org.openrewrite.Recipe recipe, Path projectPath) {
        log.info("Running OpenRewrite recipe: {} on {}", recipe.getName(), projectPath);

        ExecutionContext ctx = new InMemoryExecutionContext(t -> log.error("OpenRewrite error: ", t));

        try {
            // 1. Find Java files
            List<Path> javaFiles;
            try (Stream<Path> walk = Files.walk(projectPath)) {
                javaFiles = walk.filter(p -> p.toString().endsWith(".java"))
                        .collect(Collectors.toList());
            }

            if (javaFiles.isEmpty()) {
                log.warn("No Java files found in {}", projectPath);
                return Collections.emptyList();
            }

            // 2. Parse Java files
            JavaParser parser = JavaParser.fromJavaVersion()
                    .logCompilationWarningsAndErrors(true)
                    .build();

            List<SourceFile> sourceFiles = parser.parse(javaFiles, projectPath, ctx)
                    .collect(Collectors.toList());

            // 3. Run recipe
            List<Result> results = recipe.run(new InMemoryLargeSourceSet(sourceFiles), ctx).getChangeset()
                    .getAllResults();

            return results;

        } catch (IOException e) {
            log.error("Failed to run OpenRewrite recipe: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Saves changes from OpenRewrite results to disk.
     */
    public void applyResults(List<Result> results) {
        for (Result result : results) {
            if (result.getAfter() != null) {
                Path path = result.getAfter().getSourcePath();
                try {
                    Files.writeString(path, result.getAfter().printAll());
                } catch (IOException e) {
                    log.error("Failed to write changed file: {}", path, e);
                }
            }
        }
    }
}
