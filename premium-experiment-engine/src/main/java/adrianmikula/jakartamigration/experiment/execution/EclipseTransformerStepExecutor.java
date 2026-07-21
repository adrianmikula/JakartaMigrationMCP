package adrianmikula.jakartamigration.experiment.execution;

import adrianmikula.jakartamigration.experiment.domain.SequenceStep;
import adrianmikula.jakartamigration.experiment.domain.StepResult;
import adrianmikula.jakartamigration.experiment.execution.ExecResult;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class EclipseTransformerStepExecutor implements StepExecutor {
    private static final String ECLIPSE_TRANSFORMER_JAR = "/opt/eclipse-transformer.jar";

    @Override
    public StepResult execute(SequenceStep step, Path projectDir, CommandExecutor commandExecutor) throws IOException {
        String sourceLevel = step.sourceLevel() != null ? step.sourceLevel() : "8";
        String targetLevel = step.targetLevel() != null ? step.targetLevel() : "11";
        List<String> transformations = step.transformations() != null ? step.transformations() : List.of("CLASSFILES", "XML", "PROPERTIES");

        String transformationsArg = String.join(",", transformations);
        String cmd = String.format(
            "java -jar %s -s %s -t %s -r %s -input /workspace -output /workspace-transformed && rsync -a /workspace-transformed/ /workspace/",
            ECLIPSE_TRANSFORMER_JAR, sourceLevel, targetLevel, transformationsArg
        );

        ExecResult result = commandExecutor.exec(cmd, projectDir, 300);
        int filesChanged = result.isSuccess() ? countTransformedFiles(projectDir, commandExecutor) : 0;
        return new StepResult(result.isSuccess(), filesChanged, result.stdout(), truncateLog(result.stdout()));
    }

    private int countTransformedFiles(Path projectDir, CommandExecutor commandExecutor) throws IOException {
        try {
            ExecResult countResult = commandExecutor.exec("find /workspace -type f -newer /workspace/pom.xml 2>/dev/null | wc -l", projectDir, 30);
            if (countResult.isSuccess()) {
                try {
                    return Integer.parseInt(countResult.stdout().trim());
                } catch (NumberFormatException ignored) {
                }
            }
        } catch (Exception ignored) {
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
