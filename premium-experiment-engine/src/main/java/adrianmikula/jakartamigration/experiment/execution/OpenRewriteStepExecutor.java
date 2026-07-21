package adrianmikula.jakartamigration.experiment.execution;

import adrianmikula.jakartamigration.experiment.domain.SequenceStep;
import adrianmikula.jakartamigration.experiment.domain.StepResult;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

public class OpenRewriteStepExecutor implements StepExecutor {
    private static final String MAVEN_TOOL = "mvn";
    private static final String GRADLE_TOOL = "gradle";

    @Override
    public StepResult execute(SequenceStep step, Path projectDir, CommandExecutor commandExecutor) throws IOException {
        String recipe = Optional.ofNullable(step.recipe()).orElseThrow(() -> new IllegalArgumentException("OpenRewrite step requires recipe"));
        String recipeVersion = step.recipeVersion();

        String buildTool = detectBuildTool(projectDir);
        String command;
        if ("maven".equals(buildTool)) {
            String recipeArg = recipeVersion != null && !recipeVersion.isEmpty()
                ? String.format("org.openrewrite.maven:rewrite-maven-plugin:run -Drewrite.activeRecipes=%s", recipe)
                : String.format("org.openrewrite.maven:rewrite-maven-plugin:run -Drewrite.activeRecipes=%s", recipe);
            command = String.format("%s %s", MAVEN_TOOL, recipeArg);
        } else if ("gradle".equals(buildTool)) {
            command = String.format("%s rewriteRun -Drewrite.activeRecipes=%s", GRADLE_TOOL, recipe);
        } else {
            return new StepResult(false, 0, "Unsupported build tool for OpenRewrite: " + buildTool, "");
        }

        ExecResult result = commandExecutor.exec(command, projectDir, 600);
        int filesChanged = parseFilesChanged(result.stdout());
        return new StepResult(result.isSuccess(), filesChanged, result.stdout(), truncateLog(result.stdout()));
    }

    private String detectBuildTool(Path projectDir) {
        if (projectDir.resolve("pom.xml").toFile().exists()) return "maven";
        if (projectDir.resolve("build.gradle").toFile().exists()) return "gradle";
        if (projectDir.resolve("build.gradle.kts").toFile().exists()) return "gradle";
        return "unknown";
    }

    private int parseFilesChanged(String stdout) {
        if (stdout == null || stdout.isEmpty()) return 0;
        String[] lines = stdout.split("\n");
        for (String line : lines) {
            if (line.contains("Building")) {
                String[] parts = line.split("\\s+");
                for (String part : parts) {
                    try {
                        int val = Integer.parseInt(part);
                        if (val > 0) return val;
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        return 0;
    }

    private String truncateLog(String stdout) {
        if (stdout == null) return "";
        String[] lines = stdout.split("\n");
        if (lines.length <= 200) return stdout;
        StringBuilder sb = new StringBuilder();
        for (int i = Math.max(0, lines.length - 200); i < lines.length; i++) {
            sb.append(lines[i]).append("\n");
        }
        return sb.toString();
    }
}
