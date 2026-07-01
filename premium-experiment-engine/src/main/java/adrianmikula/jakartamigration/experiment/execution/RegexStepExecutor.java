package adrianmikula.jakartamigration.experiment.execution;

import adrianmikula.jakartamigration.experiment.domain.SequenceStep;
import adrianmikula.jakartamigration.experiment.domain.StepResult;

import java.io.IOException;
import java.nio.file.Path;

public class RegexStepExecutor implements StepExecutor {
    private static final String FIND_CMD = "find";

    @Override
    public StepResult execute(SequenceStep step, Path projectDir, CommandExecutor commandExecutor) throws IOException {
        String pattern = step.pattern();
        String replacement = step.replacement();
        String filePattern = step.filePattern() != null ? step.filePattern() : "**/*.java";
        String flags = step.flags() != null ? step.flags() : "g";

        if (pattern == null || replacement == null) {
            return new StepResult(false, 0, "Regex step requires pattern and replacement", "");
        }

        int beforeCount = countMatches(projectDir, pattern, filePattern, commandExecutor);

        String sedFlags = "g".equals(flags) ? "''" : "'" + flags + "'";
        String cmd = String.format(
            "find /workspace -type f \\( -name '*.java' -o -name '*.xml' -o -name '*.properties' \\) -exec sed -i %s 's/%s/%s/g' {} +",
            sedFlags, escapeSed(pattern), escapeSed(replacement)
        );

        ExecResult result = commandExecutor.exec(cmd, projectDir, 120);
        int afterCount = countMatches(projectDir, pattern, filePattern, commandExecutor);
        int filesChanged = Math.max(0, beforeCount - afterCount);

        return new StepResult(result.isSuccess(), filesChanged, result.stdout(), truncateLog(result.stdout()));
    }

    private int countMatches(Path projectDir, String pattern, String filePattern, CommandExecutor commandExecutor) throws IOException {
        String cmd = String.format("grep -r -c '%s' /workspace --include='*.java' --include='*.xml' --include='*.properties' 2>/dev/null || echo 0", pattern);
        ExecResult result = commandExecutor.exec(cmd, projectDir, 30);
        if (!result.isSuccess()) return 0;

        String[] lines = result.stdout().split("\n");
        int total = 0;
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.equals("0")) continue;
            String[] parts = line.split(":");
            if (parts.length >= 2) {
                try {
                    total += Integer.parseInt(parts[parts.length - 1].trim());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return total;
    }

    private String escapeSed(String s) {
        return s.replace("/", "\\/").replace("&", "\\&");
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
