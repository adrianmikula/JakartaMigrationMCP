package adrianmikula.jakartamigration.intellij.model;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Migration status enumeration from TypeSpec: intellij-plugin-ui.tsp
 */
public enum MigrationStatus {
    NOT_ANALYZED("NOT_ANALYZED"),
    READY("READY"),
    HAS_BLOCKERS("HAS_BLOCKERS"),
    IN_PROGRESS("IN_PROGRESS"),
    COMPLETED("COMPLETED"),
    FAILED("FAILED");

    private final String value;

    MigrationStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}