package adrianmikula.jakartamigration.experiment.execution;

import adrianmikula.jakartamigration.experiment.domain.SequenceStep;
import adrianmikula.jakartamigration.experiment.domain.StepResult;
import adrianmikula.jakartamigration.experiment.execution.ExecResult;

import java.io.IOException;
import java.nio.file.Path;

public class DependencyUpgradeStepExecutor implements StepExecutor {
    private static final String MAVEN_TOOL = "mvn";

    @Override
    public StepResult execute(SequenceStep step, Path projectDir, CommandExecutor commandExecutor) throws IOException {
        String groupId = step.groupId();
        String artifactId = step.artifactId();
        String version = step.version();
        String scope = step.scope();

        if (groupId == null || artifactId == null || version == null) {
            return new StepResult(false, 0, "DependencyUpgrade requires groupId, artifactId, and version", "");
        }

        StringBuilder cmd = new StringBuilder();
        cmd.append(MAVEN_TOOL).append(" versions:use-dep-version");
        cmd.append(" -DgroupId=").append(groupId);
        cmd.append(" -DartifactId=").append(artifactId);
        cmd.append(" -Dversion=").append(version);
        cmd.append(" -DprocessAllModules=true");
        if (scope != null && !scope.isEmpty()) {
            cmd.append(" -Ddep.version.scope=").append(scope);
        }

        ExecResult result = commandExecutor.exec(cmd.toString(), projectDir, 300);

        int filesChanged = 0;
        if (result.isSuccess()) {
            ExecResult treeResult = commandExecutor.exec(MAVEN_TOOL + " dependency:tree -DoutputType=text", projectDir, 120);
            if (treeResult.stdout().contains(groupId + ":" + artifactId + ":" + version)) {
                filesChanged = 1;
            }
        }

        return new StepResult(result.isSuccess(), filesChanged, result.stdout(), truncateLog(result.stdout()));
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
