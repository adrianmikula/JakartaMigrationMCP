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
 * Migration status for individual dependencies from TypeSpec: intellij-plugin-ui.tsp
 */
public enum DependencyMigrationStatus {
    COMPATIBLE("COMPATIBLE"),
    NEEDS_UPGRADE("NEEDS_UPGRADE"),
    NO_JAKARTA_VERSION("NO_JAKARTA_VERSION"),
    REQUIRES_MANUAL_MIGRATION("REQUIRES_MANUAL_MIGRATION"),
    MIGRATED("MIGRATED");

    private final String value;

    DependencyMigrationStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
