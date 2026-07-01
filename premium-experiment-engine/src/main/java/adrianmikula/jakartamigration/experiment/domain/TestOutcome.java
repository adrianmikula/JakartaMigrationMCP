package adrianmikula.jakartamigration.experiment.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Duration;

public record TestOutcome(
    int total,
    int passed,
    int failed,
    int skipped,
    Duration duration
) {
    @JsonCreator
    public TestOutcome {
        if (duration == null) duration = Duration.ZERO;
    }

    @JsonIgnore
    public boolean isAllPassed() {
        return failed == 0 && total > 0;
    }

    public double failureRate() {
        if (total == 0) return 0.0;
        return (double) failed / total;
    }
}
