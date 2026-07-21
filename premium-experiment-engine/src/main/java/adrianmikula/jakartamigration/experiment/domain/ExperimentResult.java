package adrianmikula.jakartamigration.experiment.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;
import java.util.List;

public record ExperimentResult(
    String runId,
    String sequenceName,
    Instant startedAt,
    Instant finishedAt,
    ExperimentStatus status,
    List<StepResult> stepResults,
    TestOutcome testOutcome,
    String diffSummary,
    String errorMessage
) {
    @JsonCreator
    public ExperimentResult {
        if (stepResults == null) stepResults = List.of();
    }

    @JsonIgnore
    public boolean isSuccess() {
        return status == ExperimentStatus.SUCCESS;
    }

    @JsonIgnore
    public boolean isFailed() {
        return status == ExperimentStatus.FAILED;
    }
}
