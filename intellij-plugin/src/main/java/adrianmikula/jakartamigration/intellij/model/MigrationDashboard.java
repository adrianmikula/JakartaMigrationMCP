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
import java.time.Instant;

/**
 * Migration dashboard model from TypeSpec: intellij-plugin-ui.tsp
 */
public class MigrationDashboard {
    @JsonProperty("readinessScore")
    private Integer readinessScore;

    @JsonProperty("status")
    private MigrationStatus status;

    @JsonProperty("dependencySummary")
    private DependencySummary dependencySummary;

    @JsonProperty("currentPhase")
    private MigrationPhase currentPhase;

    @JsonProperty("lastAnalyzed")
    private Instant lastAnalyzed;

    // Getters and setters
    public Integer getReadinessScore() { return readinessScore; }
    public void setReadinessScore(Integer readinessScore) { this.readinessScore = readinessScore; }

    public MigrationStatus getStatus() { return status; }
    public void setStatus(MigrationStatus status) { this.status = status; }

    public DependencySummary getDependencySummary() { return dependencySummary; }
    public void setDependencySummary(DependencySummary dependencySummary) { this.dependencySummary = dependencySummary; }

    public MigrationPhase getCurrentPhase() { return currentPhase; }
    public void setCurrentPhase(MigrationPhase currentPhase) { this.currentPhase = currentPhase; }

    public Instant getLastAnalyzed() { return lastAnalyzed; }
    public void setLastAnalyzed(Instant lastAnalyzed) { this.lastAnalyzed = lastAnalyzed; }
}