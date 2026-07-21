package adrianmikula.jakartamigration.experiment.execution;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record ExecResult(
    int exitCode,
    String stdout,
    String stderr
) {
    @JsonCreator
    public ExecResult {
    }

    public boolean isSuccess() {
        return exitCode == 0;
    }
}
