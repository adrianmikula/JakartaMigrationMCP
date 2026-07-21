package adrianmikula.jakartamigration.experiment.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

public record MigrationSequence(
    String name,
    String description,
    List<SequenceStep> steps,
    Instant createdAt,
    List<String> tags
) {
    private static final Instant DEFAULT_CREATED_AT = Instant.now();

    public MigrationSequence {
        if (createdAt == null) createdAt = DEFAULT_CREATED_AT;
        if (steps == null) steps = List.of();
        if (tags == null) tags = List.of();
    }
}
