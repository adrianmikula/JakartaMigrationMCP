package adrianmikula.jakartamigration.experiment.execution;

import adrianmikula.jakartamigration.experiment.domain.SequenceStep;
import adrianmikula.jakartamigration.experiment.domain.StepResult;
import adrianmikula.jakartamigration.experiment.execution.ExecResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class GradleJakartaPluginStepExecutor implements StepExecutor {
    private static final String GRADLE_TOOL = "gradle";

    @Override
    public StepResult execute(SequenceStep step, Path projectDir, CommandExecutor commandExecutor) throws IOException {
        String pluginVersion = step.pluginVersion();
        String tasks = step.tasks() != null && !step.tasks().isEmpty()
            ? String.join(" ", step.tasks())
            : "jakartaTransform";

        if (pluginVersion != null && !pluginVersion.isEmpty()) {
            injectPluginIfMissing(projectDir, pluginVersion, commandExecutor);
        }

        String command = String.format("%s %s", GRADLE_TOOL, tasks);
        ExecResult result = commandExecutor.exec(command, projectDir, 600);

        int filesChanged = result.isSuccess() ? estimateChangedFiles(result.stdout()) : 0;
        return new StepResult(result.isSuccess(), filesChanged, result.stdout(), truncateLog(result.stdout()));
    }

    private void injectPluginIfMissing(Path projectDir, String pluginVersion, CommandExecutor commandExecutor) throws IOException {
        Path buildGradle = projectDir.resolve("build.gradle.kts");
        if (!buildGradle.toFile().exists()) {
            buildGradle = projectDir.resolve("build.gradle");
        }
        if (!buildGradle.toFile().exists()) return;

        String content = Files.readString(buildGradle);
        if (content.contains("org.gradlex.jakartaee")) return;

        String pluginBlock = "plugins {\n    id(\"org.gradlex.jakartaee\") version \"" + pluginVersion + "\"\n}\n";
        try {
            Files.writeString(buildGradle, pluginBlock + content);
        } catch (IOException e) {
            throw new IOException("Failed to inject Gradle Jakarta plugin into build script", e);
        }
    }

    private int estimateChangedFiles(String stdout) {
        if (stdout == null || stdout.isEmpty()) return 0;
        String[] lines = stdout.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.contains("UP-TO-DATE") || line.contains("NO-SOURCE")) continue;
            if (line.contains("BUILD")) continue;
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
