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
 * Dependency summary model from TypeSpec: intellij-plugin-ui.tsp
 */
public class DependencySummary {
    @JsonProperty("totalDependencies")
    private Integer totalDependencies;

    @JsonProperty("affectedDependencies")
    private Integer affectedDependencies;

    @JsonProperty("blockerDependencies")
    private Integer blockerDependencies;

    @JsonProperty("migrableDependencies")
    private Integer migrableDependencies;

    // Getters and setters
    public Integer getTotalDependencies() { return totalDependencies; }
    public void setTotalDependencies(Integer totalDependencies) { this.totalDependencies = totalDependencies; }

    public Integer getAffectedDependencies() { return affectedDependencies; }
    public void setAffectedDependencies(Integer affectedDependencies) { this.affectedDependencies = affectedDependencies; }

    public Integer getBlockerDependencies() { return blockerDependencies; }
    public void setBlockerDependencies(Integer blockerDependencies) { this.blockerDependencies = blockerDependencies; }

    public Integer getMigrableDependencies() { return migrableDependencies; }
    public void setMigrableDependencies(Integer migrableDependencies) { this.migrableDependencies = migrableDependencies; }
}