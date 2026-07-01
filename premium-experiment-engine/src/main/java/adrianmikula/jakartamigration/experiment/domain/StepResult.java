package adrianmikula.jakartamigration.experiment.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record StepResult(
    boolean success,
    int filesChanged,
    String message,
    String executionLog
) {
    @JsonCreator
    public StepResult {
        if (executionLog == null) executionLog = "";
    }
}
