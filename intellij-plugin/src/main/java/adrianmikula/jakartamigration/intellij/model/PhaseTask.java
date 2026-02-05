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

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Phase task model from TypeSpec: intellij-plugin-ui.tsp
 */
public class PhaseTask {
    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("status")
    private TaskStatus status;

    @JsonProperty("type")
    private TaskType type;

    @JsonProperty("isAutomatable")
    private Boolean isAutomatable;

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }

    public TaskType getType() { return type; }
    public void setType(TaskType type) { this.type = type; }

    public Boolean getIsAutomatable() { return isAutomatable; }
    public void setIsAutomatable(Boolean isAutomatable) { this.isAutomatable = isAutomatable; }
}

/**
 * Task status enumeration from TypeSpec: intellij-plugin-ui.tsp
 */
enum TaskStatus {
    PENDING("PENDING"),
    IN_PROGRESS("IN_PROGRESS"),
    COMPLETED("COMPLETED"),
    FAILED("FAILED");

    private final String value;
    TaskStatus(String value) { this.value = value; }
    public String getValue() { return value; }
}

/**
 * Task type enumeration from TypeSpec: intellij-plugin-ui.tsp
 */
enum TaskType {
    DEPENDENCY_UPDATE("DEPENDENCY_UPDATE"),
    SOURCE_TRANSFORMATION("SOURCE_TRANSFORMATION"),
    CONFIGURATION_UPDATE("CONFIGURATION_UPDATE"),
    MANUAL_VERIFICATION("MANUAL_VERIFICATION"),
    TESTING("TESTING");

    private final String value;
    TaskType(String value) { this.value = value; }
    public String getValue() { return value; }
}