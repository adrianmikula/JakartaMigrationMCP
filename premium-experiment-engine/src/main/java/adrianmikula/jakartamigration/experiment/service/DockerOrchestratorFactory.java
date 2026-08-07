package adrianmikula.jakartamigration.experiment.service;

import adrianmikula.jakartamigration.experiment.execution.DockerTestContainerOrchestrator;
import adrianmikula.jakartamigration.experiment.execution.TestContainerOrchestrator;

import java.io.IOException;

public class DockerOrchestratorFactory implements ExperimentRunner.TestContainerOrchestratorFactory {
    @Override
    public TestContainerOrchestrator create(String imageName) throws IOException {
        return new DockerTestContainerOrchestrator(imageName);
    }
}
