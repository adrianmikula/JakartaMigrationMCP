package adrianmikula.jakartamigration.experiment.service;

import adrianmikula.jakartamigration.experiment.domain.*;
import adrianmikula.jakartamigration.experiment.execution.DockerTestContainerOrchestrator;
import adrianmikula.jakartamigration.experiment.execution.ExecResult;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.DockerClientFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
class ExperimentRunnerIntegrationTest {

    @Test
    void run_regex_sequence_in_real_docker_container(@TempDir Path tempDir) throws Exception {
        Assumptions.assumeTrue(dockerAvailable(), "Docker is required for this integration test");
        Path projectDir = tempDir.resolve("project");
        Files.createDirectories(projectDir.resolve("src/main/java"));
        Files.writeString(projectDir.resolve("pom.xml"), "<project></project>\n");
        Files.writeString(projectDir.resolve("src/main/java/Hello.java"), "import javax.persistence.Entity;\npublic class Hello {}\n");

        SequenceService sequenceService = new SequenceService(projectDir);
        MigrationSequence sequence = new MigrationSequence(
            "regex-seq",
            "Test",
            List.of(SequenceStep.regexReplacement("javax", "jakarta", "**/*.java")),
            Instant.now(),
            List.of()
        );
        sequenceService.saveSequence(sequence);

        ExperimentRunner runner = new ExperimentRunner(projectDir, new DockerOrchestratorFactory(), "eclipse-temurin:17-jdk", 120);
        ExperimentResult result = runner.run("regex-seq", projectDir, Optional.empty());

        assertEquals(ExperimentStatus.SUCCESS, result.status());
        assertEquals(1, result.stepResults().size());
        assertTrue(result.stepResults().get(0).success());
        assertTrue(result.stepResults().get(0).filesChanged() > 0);
    }

    @Test
    void detects_docker_unavailable_gracefully(@TempDir Path tempDir) throws Exception {
        Assumptions.assumeFalse(dockerAvailable(), "Docker is available, skipping missing-Docker test");

        Path projectDir = tempDir.resolve("project");
        Files.createDirectories(projectDir);
        Files.writeString(projectDir.resolve("pom.xml"), "<project></project>\n");

        SequenceService sequenceService = new SequenceService(projectDir);
        MigrationSequence sequence = new MigrationSequence(
            "unavailable-seq",
            "Test",
            List.of(SequenceStep.regexReplacement("javax", "jakarta", "**/*.java")),
            Instant.now(),
            List.of()
        );
        sequenceService.saveSequence(sequence);

        ExperimentRunner runner = new ExperimentRunner(projectDir, new DockerOrchestratorFactory(), "eclipse-temurin:17-jdk", 30);
        ExperimentResult result = runner.run("unavailable-seq", projectDir, Optional.empty());

        assertEquals(ExperimentStatus.FAILED, result.status());
        assertNotNull(result.errorMessage());
        assertTrue(
            result.errorMessage().contains("Could not find a valid Docker environment") ||
            result.errorMessage().contains("Previous attempts to find a Docker environment failed") ||
            result.errorMessage().contains("Docker daemon not running"),
            "Unexpected error message: " + result.errorMessage()
        );
    }

    @Test
    void docker_test_container_orchestrator_executes_command(@TempDir Path tempDir) throws Exception {
        Assumptions.assumeTrue(dockerAvailable(), "Docker is required for this integration test");
        DockerTestContainerOrchestrator container = new DockerTestContainerOrchestrator("eclipse-temurin:17-jdk");
        try {
            container.start();

            ExecResult result = container.exec("echo hello", tempDir, 30);

            assertTrue(result.isSuccess());
            assertTrue(result.stdout().contains("hello"));
        } finally {
            container.stop();
        }
    }

    private boolean dockerAvailable() {
        try {
            DockerClientFactory.instance().client();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
