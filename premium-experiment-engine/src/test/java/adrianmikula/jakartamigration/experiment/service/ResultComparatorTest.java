package adrianmikula.jakartamigration.experiment.service;

import adrianmikula.jakartamigration.experiment.domain.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ResultComparatorTest {

    @Test
    void compare_whenAHasFewerFailures_recommendsA() {
        ExperimentResult a = buildResult("runA", ExperimentStatus.SUCCESS, 2);
        ExperimentResult b = buildResult("runB", ExperimentStatus.FAILED, 15);

        ComparisonReport report = new ResultComparator().compare(a, b);

        assertEquals("A", report.recommended());
        assertTrue(report.reasons().stream().anyMatch(r -> r.metric().equals("testFailures") && r.winner().equals("A")));
    }

    @Test
    void compare_whenBHasFewerFailures_recommendsB() {
        ExperimentResult a = buildResult("runA", ExperimentStatus.FAILED, 15);
        ExperimentResult b = buildResult("runB", ExperimentStatus.SUCCESS, 2);

        ComparisonReport report = new ResultComparator().compare(a, b);

        assertEquals("B", report.recommended());
    }

    @Test
    void compare_whenBothSucceed_comparesFilesChanged() {
        ExperimentResult a = buildResult("runA", ExperimentStatus.SUCCESS, 0);
        a = new ExperimentResult(a.runId(), a.sequenceName(), a.startedAt(), a.finishedAt(), a.status(),
            List.of(new StepResult(true, 10, "ok", "")), a.testOutcome(), a.diffSummary(), a.errorMessage());

        ExperimentResult b = buildResult("runB", ExperimentStatus.SUCCESS, 0);
        b = new ExperimentResult(b.runId(), b.sequenceName(), b.startedAt(), b.finishedAt(), b.status(),
            List.of(new StepResult(true, 50, "ok", "")), b.testOutcome(), b.diffSummary(), b.errorMessage());

        ComparisonReport report = new ResultComparator().compare(a, b);

        assertEquals("A", report.recommended());
    }

    @Test
    void compare_whenBothFailed_prefersA() {
        ExperimentResult a = buildResult("runA", ExperimentStatus.FAILED, 0);
        ExperimentResult b = buildResult("runB", ExperimentStatus.FAILED, 0);

        ComparisonReport report = new ResultComparator().compare(a, b);
        assertEquals("A", report.recommended());
    }

    @Test
    void compare_detectsStepDivergence() {
        ExperimentResult a = buildResult("runA", ExperimentStatus.SUCCESS, 0);
        a = new ExperimentResult(a.runId(), a.sequenceName(), a.startedAt(), a.finishedAt(), a.status(),
            List.of(new StepResult(true, 5, "step A ok", "")), a.testOutcome(), a.diffSummary(), a.errorMessage());

        ExperimentResult b = buildResult("runB", ExperimentStatus.FAILED, 0);
        b = new ExperimentResult(b.runId(), b.sequenceName(), b.startedAt(), b.finishedAt(), b.status(),
            List.of(new StepResult(false, 0, "step B failed", "")), b.testOutcome(), b.diffSummary(), b.errorMessage());

        ComparisonReport report = new ResultComparator().compare(a, b);
        assertEquals(1, report.stepDivergence().size());
        assertEquals(0, report.stepDivergence().get(0).stepIndex());
    }

    private ExperimentResult buildResult(String runId, ExperimentStatus status, int testFailures) {
        TestOutcome outcome = new TestOutcome(20, 20 - testFailures, testFailures, 0, java.time.Duration.ofSeconds(30));
        return new ExperimentResult(
            runId,
            "seq1",
            Instant.now(),
            Instant.now(),
            status,
            List.of(),
            outcome,
            "diff summary",
            null
        );
    }
}
