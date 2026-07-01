package adrianmikula.jakartamigration.experiment.execution;

import adrianmikula.jakartamigration.experiment.domain.SequenceStep;
import adrianmikula.jakartamigration.experiment.domain.StepResult;

import java.io.IOException;
import java.nio.file.Path;

public interface StepExecutor {
    StepResult execute(SequenceStep step, Path projectDir, CommandExecutor commandExecutor) throws IOException;
}
