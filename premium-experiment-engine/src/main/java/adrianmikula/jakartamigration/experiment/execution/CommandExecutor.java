package adrianmikula.jakartamigration.experiment.execution;

import java.io.IOException;
import java.nio.file.Path;

public interface CommandExecutor {
    ExecResult exec(String command, Path workingDir) throws IOException;
    ExecResult exec(String command, Path workingDir, int timeoutSeconds) throws IOException;
}
