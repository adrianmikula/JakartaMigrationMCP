/*
 * Copyright © 2026 Adrian Mikula
 *
 * All rights reserved.
 *
 * This software is proprietary and may not be used, copied,
 * modified, or distributed except under the terms of a
 * separate commercial license agreement.
 */
package adrianmikula.jakartamigration.intellij.model;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Risk level enumeration from TypeSpec: intellij-plugin-ui.tsp
 */
public enum RiskLevel {
    LOW("LOW"),
    MEDIUM("MEDIUM"),
    HIGH("HIGH"),
    CRITICAL("CRITICAL");

    private final String value;

    RiskLevel(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
