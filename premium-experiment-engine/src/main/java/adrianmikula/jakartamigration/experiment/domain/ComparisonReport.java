package adrianmikula.jakartamigration.experiment.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ComparisonReport(
    String comparisonId,
    ExperimentResultSummary runA,
    ExperimentResultSummary runB,
    String recommended,
    List<ComparisonReason> reasons,
    List<StepDivergence> stepDivergence
) {
    @JsonCreator
    public ComparisonReport {
        if (reasons == null) reasons = List.of();
        if (stepDivergence == null) stepDivergence = List.of();
    }

    public record ExperimentResultSummary(
        String runId,
        String sequenceName,
        ExperimentStatus status
    ) {
        @JsonCreator
        public ExperimentResultSummary {
        }
    }

    public record ComparisonReason(
        String metric,
        Object valueA,
        Object valueB,
        String winner
    ) {
        @JsonCreator
        public ComparisonReason {
        }
    }

    public record StepDivergence(
        int stepIndex,
        StepResult runAResult,
        StepResult runBResult
    ) {
        @JsonCreator
        public StepDivergence {
        }
    }
}
