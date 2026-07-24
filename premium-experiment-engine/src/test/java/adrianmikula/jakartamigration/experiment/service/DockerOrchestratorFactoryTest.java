package adrianmikula.jakartamigration.experiment.service;

import adrianmikula.jakartamigration.experiment.execution.DockerTestContainerOrchestrator;
import adrianmikula.jakartamigration.experiment.execution.TestContainerOrchestrator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DockerOrchestratorFactoryTest {

    @Test
    void create_returnsDockerTestContainerOrchestrator() throws Exception {
        DockerOrchestratorFactory factory = new DockerOrchestratorFactory();
        TestContainerOrchestrator orchestrator = factory.create("eclipse-temurin:17-jdk");

        assertNotNull(orchestrator);
        assertInstanceOf(DockerTestContainerOrchestrator.class, orchestrator);
    }

    @Test
    void create_implementsTestContainerOrchestratorFactory() {
        DockerOrchestratorFactory factory = new DockerOrchestratorFactory();
        assertInstanceOf(ExperimentRunner.TestContainerOrchestratorFactory.class, factory);
    }
}
