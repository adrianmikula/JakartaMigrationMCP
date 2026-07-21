package adrianmikula.jakartamigration.experiment.execution;

import adrianmikula.jakartamigration.experiment.execution.ExecResult;

import java.io.IOException;
import java.nio.file.Path;

public interface TestContainerOrchestrator extends CommandExecutor {
    void start() throws IOException;
    void stop();
    void copyInto(Path localSource, Path containerTarget) throws IOException;
    void copyOut(Path containerSource, Path localTarget) throws IOException;
    boolean isRunning();
}
