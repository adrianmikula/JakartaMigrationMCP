package adrianmikula.jakartamigration.intellij.model;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Phase status enumeration from TypeSpec: intellij-plugin-ui.tsp
 */
public enum PhaseStatus {
    NOT_STARTED("NOT_STARTED"),
    IN_PROGRESS("IN_PROGRESS"),
    COMPLETED("COMPLETED"),
    FAILED("FAILED"),
    SKIPPED("SKIPPED");

    private final String value;

    PhaseStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}