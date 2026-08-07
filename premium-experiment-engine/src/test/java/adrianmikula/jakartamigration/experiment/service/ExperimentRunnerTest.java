package adrianmikula.jakartamigration.experiment.service;

import adrianmikula.jakartamigration.experiment.domain.*;
import adrianmikula.jakartamigration.experiment.execution.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExperimentRunnerTest {

    @Test
    void run_sequence_success(@TempDir Path tempDir) throws Exception {
        Path projectDir = tempDir.resolve("project");
        projectDir.toFile().mkdirs();
        projectDir.resolve("pom.xml").toFile().createNewFile();

        SequenceService sequenceService = new SequenceService(projectDir);
        MigrationSequence sequence = new MigrationSequence(
            "test-seq",
            "Test",
            List.of(SequenceStep.regexReplacement("javax", "jakarta", "**/*.java")),
            Instant.now(),
            List.of()
        );
        sequenceService.saveSequence(sequence);

        TestContainerOrchestrator mockContainer = mock(TestContainerOrchestrator.class);
        when(mockContainer.exec(anyString(), any(), anyInt())).thenAnswer(invocation -> {
            String cmd = invocation.getArgument(0);
            if (cmd.contains("grep")) {
                return new ExecResult(0, "0\n", "");
            }
            if (cmd.contains("sed")) {
                return new ExecResult(0, "", "");
            }
            if (cmd.contains("mvn test")) {
                return new ExecResult(0, "Tests run: 5, Failures: 0, Skipped: 0\n", "");
            }
            return new ExecResult(0, "", "");
        });

        ExperimentRunner runner = new ExperimentRunner(projectDir, imageName -> mockContainer, "migration-lab:test", 120);
        ExperimentResult result = runner.run("test-seq", projectDir, Optional.empty());

        assertNotNull(result);
        assertEquals(ExperimentStatus.SUCCESS, result.status());
        assertEquals(1, result.stepResults().size());
        assertTrue(result.stepResults().get(0).success());
    }

    @Test
    void run_sequence_step_failure(@TempDir Path tempDir) throws Exception {
        Path projectDir = tempDir.resolve("project");
        projectDir.toFile().mkdirs();
        projectDir.resolve("pom.xml").toFile().createNewFile();

        SequenceService sequenceService = new SequenceService(projectDir);
        MigrationSequence sequence = new MigrationSequence(
            "fail-seq",
            "Test",
            List.of(SequenceStep.regexReplacement("javax", "jakarta", "**/*.java")),
            Instant.now(),
            List.of()
        );
        sequenceService.saveSequence(sequence);

        TestContainerOrchestrator mockContainer = mock(TestContainerOrchestrator.class);
        when(mockContainer.exec(anyString(), any(), anyInt())).thenReturn(new ExecResult(1, "", "sed: failed"));

        ExperimentRunner runner = new ExperimentRunner(projectDir, imageName -> mockContainer, "migration-lab:test", 120);
        ExperimentResult result = runner.run("fail-seq", projectDir, Optional.empty());

        assertNotNull(result);
        assertEquals(ExperimentStatus.FAILED, result.status());
        assertTrue(result.errorMessage().contains("Step 0 failed"));
    }

    @Test
    void run_sequence_computes_passed_from_test_report(@TempDir Path tempDir) throws Exception {
        Path projectDir = tempDir.resolve("project");
        projectDir.toFile().mkdirs();
        projectDir.resolve("pom.xml").toFile().createNewFile();

        SequenceService sequenceService = new SequenceService(projectDir);
        MigrationSequence sequence = new MigrationSequence(
            "test-seq",
            "Test",
            List.of(SequenceStep.regexReplacement("javax", "jakarta", "**/*.java")),
            Instant.now(),
            List.of()
        );
        sequenceService.saveSequence(sequence);

        TestContainerOrchestrator mockContainer = mock(TestContainerOrchestrator.class);
        when(mockContainer.exec(anyString(), any(), anyInt())).thenAnswer(invocation -> {
            String cmd = invocation.getArgument(0);
            if (cmd.contains("grep")) {
                return new ExecResult(0, "0\n", "");
            }
            if (cmd.contains("sed")) {
                return new ExecResult(0, "", "");
            }
            if (cmd.contains("mvn test")) {
                return new ExecResult(0, "Tests run: 10, Failures: 2, Skipped: 1\n", "");
            }
            return new ExecResult(0, "", "");
        });

        ExperimentRunner runner = new ExperimentRunner(projectDir, imageName -> mockContainer, "migration-lab:test", 120);
        ExperimentResult result = runner.run("test-seq", projectDir, Optional.empty());

        assertNotNull(result);
        assertEquals(ExperimentStatus.SUCCESS, result.status());
        assertNotNull(result.testOutcome());
        assertEquals(10, result.testOutcome().total());
        assertEquals(7, result.testOutcome().passed());
        assertEquals(2, result.testOutcome().failed());
        assertEquals(1, result.testOutcome().skipped());
    }

    @Test
    void run_sequence_container_stop_failure_does_not_prevent_result(@TempDir Path tempDir) throws Exception {
        Path projectDir = tempDir.resolve("project");
        projectDir.toFile().mkdirs();
        projectDir.resolve("pom.xml").toFile().createNewFile();

        SequenceService sequenceService = new SequenceService(projectDir);
        MigrationSequence sequence = new MigrationSequence(
            "stop-fail-seq",
            "Test",
            List.of(SequenceStep.regexReplacement("javax", "jakarta", "**/*.java")),
            Instant.now(),
            List.of()
        );
        sequenceService.saveSequence(sequence);

        TestContainerOrchestrator mockContainer = mock(TestContainerOrchestrator.class);
        when(mockContainer.exec(anyString(), any(), anyInt())).thenAnswer(invocation -> {
            String cmd = invocation.getArgument(0);
            if (cmd.contains("grep")) {
                return new ExecResult(0, "0\n", "");
            }
            if (cmd.contains("sed")) {
                return new ExecResult(0, "", "");
            }
            if (cmd.contains("mvn test")) {
                return new ExecResult(0, "Tests run: 3, Failures: 0, Skipped: 0\n", "");
            }
            return new ExecResult(0, "", "");
        });
        doThrow(new RuntimeException("Docker stop failed")).when(mockContainer).stop();

        ExperimentRunner runner = new ExperimentRunner(projectDir, imageName -> mockContainer, "migration-lab:test", 120);
        ExperimentResult result = runner.run("stop-fail-seq", projectDir, Optional.empty());

        assertNotNull(result);
        assertEquals(ExperimentStatus.SUCCESS, result.status());
    }

    @Test
    void run_sequence_step_failure_records_to_history(@TempDir Path tempDir) throws Exception {
        Path projectDir = tempDir.resolve("project");
        projectDir.toFile().mkdirs();
        projectDir.resolve("pom.xml").toFile().createNewFile();

        SequenceService sequenceService = new SequenceService(projectDir);
        MigrationSequence sequence = new MigrationSequence(
            "hist-fail-seq",
            "Test",
            List.of(SequenceStep.regexReplacement("javax", "jakarta", "**/*.java")),
            Instant.now(),
            List.of()
        );
        sequenceService.saveSequence(sequence);

        TestContainerOrchestrator mockContainer = mock(TestContainerOrchestrator.class);
        when(mockContainer.exec(anyString(), any(), anyInt())).thenReturn(new ExecResult(1, "", "step failed"));

        ExperimentRunner runner = new ExperimentRunner(projectDir, imageName -> mockContainer, "migration-lab:test", 120);
        ExperimentResult result = runner.run("hist-fail-seq", projectDir, Optional.empty());

        assertEquals(ExperimentStatus.FAILED, result.status());

        HistoryService historyService = new HistoryService(projectDir);
        Optional<ExperimentResult> recorded = historyService.getRun(result.runId());
        assertTrue(recorded.isPresent());
        assertEquals(ExperimentStatus.FAILED, recorded.get().status());
    }

    @Test
    void run_sequence_exception_returns_failed_result_and_records_to_history(@TempDir Path tempDir) throws Exception {
        Path projectDir = tempDir.resolve("project");
        projectDir.toFile().mkdirs();
        projectDir.resolve("pom.xml").toFile().createNewFile();

        SequenceService sequenceService = new SequenceService(projectDir);
        MigrationSequence sequence = new MigrationSequence(
            "ex-seq",
            "Test",
            List.of(SequenceStep.regexReplacement("javax", "jakarta", "**/*.java")),
            Instant.now(),
            List.of()
        );
        sequenceService.saveSequence(sequence);

        ExperimentRunner.TestContainerOrchestratorFactory failingFactory = imageName -> {
            throw new IOException("Docker daemon not running");
        };

        ExperimentRunner runner = new ExperimentRunner(projectDir, failingFactory, "migration-lab:test", 120);
        ExperimentResult result = runner.run("ex-seq", projectDir, Optional.empty());

        assertEquals(ExperimentStatus.FAILED, result.status());
        assertTrue(result.errorMessage().contains("Docker daemon not running"));

        HistoryService historyService = new HistoryService(projectDir);
        Optional<ExperimentResult> recorded = historyService.getRun(result.runId());
        assertTrue(recorded.isPresent());
        assertEquals(ExperimentStatus.FAILED, recorded.get().status());
    }

    @Test
    void copyDirectory_skipsDeletedFiles(@TempDir Path tempDir) throws Exception {
        Path sourceDir = tempDir.resolve("source");
        sourceDir.toFile().mkdirs();
        Files.writeString(sourceDir.resolve("keep.txt"), "keep");
        Files.writeString(sourceDir.resolve("delete.txt"), "delete");

        Path targetDir = tempDir.resolve("target");
        targetDir.toFile().mkdirs();

        Files.delete(sourceDir.resolve("delete.txt"));

        ExperimentRunner runner = new ExperimentRunner(tempDir, imageName -> null, "test-image", 120);
        Method copyMethod = ExperimentRunner.class.getDeclaredMethod("copyDirectory", Path.class, Path.class);
        copyMethod.setAccessible(true);
        copyMethod.invoke(runner, sourceDir, targetDir);

        assertTrue(Files.exists(targetDir.resolve("keep.txt")));
        assertFalse(Files.exists(targetDir.resolve("delete.txt")));
    }
}
