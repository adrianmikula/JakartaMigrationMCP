package adrianmikula.jakartamigration.experiment.service;

import adrianmikula.jakartamigration.experiment.execution.TestContainerOrchestrator;

import java.io.IOException;
import java.nio.file.Path;

public class NoOpOrchestratorFactory implements ExperimentRunner.TestContainerOrchestratorFactory {
    @Override
    public TestContainerOrchestrator create(String imageName) throws IOException {
        throw new UnsupportedOperationException(
            "Container orchestration is not yet configured. " +
            "The experiment history UI is functional, but running new experiments requires a TestContainerOrchestratorFactory implementation."
        );
    }
}
