package adrianmikula.jakartamigration.experiment.mcp;

import adrianmikula.jakartamigration.experiment.domain.*;
import adrianmikula.jakartamigration.experiment.execution.TestContainerOrchestrator;
import adrianmikula.jakartamigration.experiment.service.ExperimentRunner;
import adrianmikula.jakartamigration.experiment.service.HistoryService;
import adrianmikula.jakartamigration.experiment.service.SequenceService;
import adrianmikula.jakartamigration.experiment.service.ResultComparator;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.NoSuchElementException;

public class ExperimentTools {
    private final SequenceService sequenceService;
    private final HistoryService historyService;
    private final ExperimentRunner experimentRunner;
    private final ResultComparator resultComparator;
    private final String dockerImage;

    public ExperimentTools(Path projectRoot, ExperimentRunner.TestContainerOrchestratorFactory containerFactory, String dockerImage, int testTimeoutSeconds) {
        this.sequenceService = new SequenceService(projectRoot);
        this.historyService = new HistoryService(projectRoot);
        this.experimentRunner = new ExperimentRunner(projectRoot, containerFactory, dockerImage, testTimeoutSeconds);
        this.resultComparator = new ResultComparator();
        this.dockerImage = dockerImage;
    }

    public MigrationSequence createMigrationSequence(String name, String description, List<SequenceStep> steps, List<String> tags) throws Exception {
        MigrationSequence sequence = new MigrationSequence(name, description, steps, null, tags);
        sequenceService.saveSequence(sequence);
        return sequence;
    }

    public MigrationSequence addSequenceStep(String sequenceName, SequenceStep step) throws Exception {
        return sequenceService.appendStep(sequenceName, step);
    }

    public List<MigrationSequence> listMigrationSequences(String tag) throws Exception {
        return sequenceService.listSequences(Optional.ofNullable(tag));
    }

    public Optional<MigrationSequence> getMigrationSequence(String name) throws Exception {
        return sequenceService.loadSequence(name);
    }

    public ExperimentResult runMigrationExperiment(String sequenceName, Path projectPath, Optional<String> gitRef) throws Exception {
        return experimentRunner.run(sequenceName, projectPath, gitRef);
    }

    public List<ExperimentResult> getExperimentHistory(String sequenceName, ExperimentStatus status, int limit) throws Exception {
        return historyService.listRuns(Optional.ofNullable(sequenceName), Optional.ofNullable(status), limit);
    }

    public Optional<ExperimentResult> getExperimentResult(String runId) throws Exception {
        return historyService.getRun(runId);
    }

    public ComparisonReport compareExperiments(String runIdA, String runIdB) throws Exception {
        ExperimentResult resultA = historyService.getRun(runIdA)
            .orElseThrow(() -> new NoSuchElementException("Run not found: " + runIdA));
        ExperimentResult resultB = historyService.getRun(runIdB)
            .orElseThrow(() -> new NoSuchElementException("Run not found: " + runIdB));
        return resultComparator.compare(resultA, resultB);
    }

    public MigrationSequence getMigrationSequenceResult(String runId) throws Exception {
        ExperimentResult result = historyService.getRun(runId)
            .orElseThrow(() -> new NoSuchElementException("Run not found: " + runId));
        return sequenceService.loadSequence(result.sequenceName())
            .orElseThrow(() -> new NoSuchElementException("Sequence not found: " + result.sequenceName()));
    }
}
