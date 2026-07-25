package adrianmikula.jakartamigration.experiment.service;

import adrianmikula.jakartamigration.experiment.domain.*;
import adrianmikula.jakartamigration.experiment.execution.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExperimentRunner {
    private static final Logger LOG = LoggerFactory.getLogger(ExperimentRunner.class);

    private final SequenceService sequenceService;
    private final HistoryService historyService;
    private final TestContainerOrchestratorFactory containerFactory;
    private final String dockerImage;
    private final int testTimeoutSeconds;

    @FunctionalInterface
    public interface TestContainerOrchestratorFactory {
        TestContainerOrchestrator create(String imageName) throws IOException;
    }

    public ExperimentRunner(Path projectRoot, TestContainerOrchestratorFactory containerFactory, String dockerImage, int testTimeoutSeconds) {
        this.sequenceService = new SequenceService(projectRoot);
        this.historyService = new HistoryService(projectRoot);
        this.containerFactory = containerFactory;
        this.dockerImage = dockerImage;
        this.testTimeoutSeconds = testTimeoutSeconds;
    }

    public ExperimentResult run(String sequenceName, Path projectDir, Optional<String> gitRef) throws Exception {
        MigrationSequence sequence = sequenceService.loadSequence(sequenceName)
            .orElseThrow(() -> new NoSuchElementException("Sequence not found: " + sequenceName));

        String runId = UUID.randomUUID().toString();
        Instant startedAt = Instant.now();
        Path tempDir = Files.createTempDirectory("migration-experiment-");
        Path experimentsTmp = tempDir.resolve(".experiments/tmp/" + runId + "-");
        Files.createDirectories(experimentsTmp);

        try {
            Path snapshot = copyProjectToSnapshot(projectDir, experimentsTmp, gitRef);
            TestContainerOrchestrator container = containerFactory.create(dockerImage);
            container.start();
            copySnapshotIntoContainer(snapshot, container);

            try {
                List<StepResult> stepResults = new ArrayList<>();
                for (int i = 0; i < sequence.steps().size(); i++) {
                    SequenceStep step = sequence.steps().get(i);
                    StepExecutor executor = resolveExecutor(step);
                    StepResult stepResult = executor.execute(step, snapshot, container);
                    stepResults.add(stepResult);
                    if (!stepResult.success()) {
                        ExperimentResult failedResult = buildFailedResult(runId, sequenceName, startedAt, stepResults, "Step " + i + " failed: " + stepResult.message());
                        recordToHistory(failedResult);
                        return failedResult;
                    }
                }

                TestOutcome testOutcome = runTests(snapshot, container);
                Instant finishedAt = Instant.now();
                String diffSummary = buildDiffSummary(stepResults, testOutcome);

                ExperimentResult result = new ExperimentResult(
                    runId,
                    sequenceName,
                    startedAt,
                    finishedAt,
                    ExperimentStatus.SUCCESS,
                    stepResults,
                    testOutcome,
                    diffSummary,
                    null
                );

                recordToHistory(result);
                return result;

            } finally {
                try {
                    container.stop();
                } catch (Exception e) {
                    LOG.warn("Failed to stop container for run {}", runId, e);
                }
            }

        } catch (Exception e) {
            LOG.error("Experiment run {} failed", runId, e);
            Instant finishedAt = Instant.now();
            ExperimentResult result = new ExperimentResult(
                runId,
                sequenceName,
                startedAt,
                finishedAt,
                ExperimentStatus.FAILED,
                List.of(),
                null,
                null,
                e.getMessage()
            );
            recordToHistory(result);
            return result;
        }
    }

    private void recordToHistory(ExperimentResult result) {
        try {
            historyService.recordRun(result);
        } catch (Exception e) {
            LOG.error("Failed to record experiment run {} to history", result.runId(), e);
        }
    }

    private Path copyProjectToSnapshot(Path projectDir, Path tempDir, Optional<String> gitRef) throws Exception {
        Path snapshot = tempDir.resolve("workspace");
        Files.createDirectories(snapshot);

        if (gitRef.isPresent()) {
            Path archive = snapshot.resolve("archive.tar");
            ProcessBuilder archivePb = new ProcessBuilder("git", "archive", gitRef.get())
                .directory(projectDir.toFile())
                .redirectOutput(archive.toFile());
            int archiveExit = archivePb.start().waitFor();
            if (archiveExit != 0) {
                throw new IOException("git archive failed with exit code " + archiveExit);
            }

            ProcessBuilder extractPb = new ProcessBuilder("tar", "-xf", "archive.tar")
                .directory(snapshot.toFile());
            int extractExit = extractPb.start().waitFor();
            Files.deleteIfExists(archive);
            if (extractExit != 0) {
                throw new IOException("tar extract failed with exit code " + extractExit);
            }
        } else {
            copyDirectory(projectDir, snapshot);
        }

        return snapshot;
    }

    private TestOutcome runTests(Path projectDir, TestContainerOrchestrator container) throws Exception {
        String buildTool = detectBuildTool(projectDir);
        String command;
        if ("maven".equals(buildTool)) {
            command = "mvn test -B";
        } else if ("gradle".equals(buildTool)) {
            command = "gradle test --no-daemon";
        } else {
            return new TestOutcome(0, 0, 0, 0, Duration.ZERO);
        }

        ExecResult result = container.exec(command, projectDir, testTimeoutSeconds);
        if (!result.isSuccess()) {
            return new TestOutcome(0, 0, 1, 0, Duration.ZERO);
        }

        int total = countInReport(result.stdout(), "Tests run:");
        int failed = countInReport(result.stdout(), "Failures:");
        int skipped = countInReport(result.stdout(), "Skipped:");
        int passed = Math.max(0, total - failed - skipped);
        return new TestOutcome(total, passed, failed, skipped, Duration.ZERO);
    }

    private String detectBuildTool(Path projectDir) {
        if (projectDir.resolve("pom.xml").toFile().exists()) return "maven";
        if (projectDir.resolve("build.gradle").toFile().exists()) return "gradle";
        if (projectDir.resolve("build.gradle.kts").toFile().exists()) return "gradle";
        return "unknown";
    }

    private StepExecutor resolveExecutor(SequenceStep step) {
        return switch (step.type()) {
            case OPENREWRITE -> new OpenRewriteStepExecutor();
            case DEPENDENCY_UPGRADE -> new DependencyUpgradeStepExecutor();
            case ECLIPSE_TRANSFORMER -> new EclipseTransformerStepExecutor();
            case GRADLE_JAKARTA_PLUGIN -> new GradleJakartaPluginStepExecutor();
            case REGEX_REPLACEMENT -> new RegexStepExecutor();
        };
    }

    private ExperimentResult buildFailedResult(String runId, String sequenceName, Instant startedAt, List<StepResult> stepResults, String errorMessage) {
        return new ExperimentResult(
            runId, sequenceName, startedAt, Instant.now(),
            ExperimentStatus.FAILED, stepResults, null, null, errorMessage
        );
    }

    private String buildDiffSummary(List<StepResult> stepResults, TestOutcome testOutcome) {
        int totalFiles = stepResults.stream().mapToInt(StepResult::filesChanged).sum();
        StringBuilder sb = new StringBuilder();
        sb.append(totalFiles).append(" files modified");
        if (testOutcome != null) {
            sb.append(", ").append(testOutcome.failed()).append(" test failures");
        }
        return sb.toString();
    }

    private int countInReport(String output, String marker) {
        if (output == null || output.isEmpty()) return 0;
        String regex = Pattern.quote(marker) + "\\s*(\\d+)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(output);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
    }

    private void copySnapshotIntoContainer(Path snapshot, TestContainerOrchestrator container) throws IOException {
        try (var paths = Files.walk(snapshot)) {
            for (Path sourcePath : paths.toList()) {
                if (Files.isRegularFile(sourcePath)) {
                    Path relative = snapshot.relativize(sourcePath);
                    Path targetPath = Path.of("/workspace").resolve(relative);
                    LOG.debug("Copying {} into container at {}", sourcePath, targetPath);
                    container.copyInto(sourcePath, targetPath);
                }
            }
        }
    }

    private void copyDirectory(Path source, Path target) throws Exception {
        Files.walk(source).forEach(sourcePath -> {
            try {
                Path targetPath = target.resolve(source.relativize(sourcePath).toString());
                if (Files.isDirectory(sourcePath)) {
                    Files.createDirectories(targetPath);
                } else if (Files.exists(sourcePath)) {
                    Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                } else {
                    LOG.warn("Skipping missing file during copy: {}", sourcePath);
                }
            } catch (NoSuchFileException e) {
                LOG.warn("File disappeared during copy, skipping: {}", sourcePath);
            } catch (IOException e) {
                LOG.warn("Failed to copy file {}, skipping: {}", sourcePath, e.getMessage());
            }
        });
    }
}
