/*
 * Copyright © 2026 Adrian Mikula
 *
 * All rights reserved.
 *
 * This software is proprietary and may not be used, copied,
 * modified, or distributed except under the terms of a
 * separate commercial license agreement.
 */
package adrianmikula.jakartamigration.intellij.mcp;

import adrianmikula.jakartamigration.intellij.model.DependencyInfo;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response from migration impact analysis from TypeSpec: mcp-integration.tsp
 */
public class AnalyzeMigrationImpactResponse {
    @JsonProperty("dependencyImpact")
    private DependencyImpactDetails dependencyImpact;

    @JsonProperty("overallImpact")
    private ImpactAssessment overallImpact;

    @JsonProperty("estimatedEffort")
    private EffortEstimate estimatedEffort;

    public static class DependencyImpactDetails {
        @JsonProperty("affectedDependencies")
        private List<DependencyInfo> affectedDependencies;

        @JsonProperty("transitiveDependencyChanges")
        private int transitiveDependencyChanges;

        @JsonProperty("breakingChanges")
        private List<String> breakingChanges;

        public List<DependencyInfo> getAffectedDependencies() {
            return affectedDependencies;
        }

        public void setAffectedDependencies(List<DependencyInfo> affectedDependencies) {
            this.affectedDependencies = affectedDependencies;
        }

        public int getTransitiveDependencyChanges() {
            return transitiveDependencyChanges;
        }

        public void setTransitiveDependencyChanges(int transitiveDependencyChanges) {
            this.transitiveDependencyChanges = transitiveDependencyChanges;
        }

        public List<String> getBreakingChanges() {
            return breakingChanges;
        }

        public void setBreakingChanges(List<String> breakingChanges) {
            this.breakingChanges = breakingChanges;
        }
    }

    public static class ImpactAssessment {
        @JsonProperty("level")
        private String level;

        @JsonProperty("description")
        private String description;

        @JsonProperty("riskFactors")
        private List<String> riskFactors;

        public String getLevel() {
            return level;
        }

        public void setLevel(String level) {
            this.level = level;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public List<String> getRiskFactors() {
            return riskFactors;
        }

        public void setRiskFactors(List<String> riskFactors) {
            this.riskFactors = riskFactors;
        }
    }

    public static class EffortEstimate {
        @JsonProperty("estimatedHours")
        private int estimatedHours;

        @JsonProperty("confidence")
        private String confidence;

        public int getEstimatedHours() {
            return estimatedHours;
        }

        public void setEstimatedHours(int estimatedHours) {
            this.estimatedHours = estimatedHours;
        }

        public String getConfidence() {
            return confidence;
        }

        public void setConfidence(String confidence) {
            this.confidence = confidence;
        }
    }

    public DependencyImpactDetails getDependencyImpact() {
        return dependencyImpact;
    }

    public void setDependencyImpact(DependencyImpactDetails dependencyImpact) {
        this.dependencyImpact = dependencyImpact;
    }

    public ImpactAssessment getOverallImpact() {
        return overallImpact;
    }

    public void setOverallImpact(ImpactAssessment overallImpact) {
        this.overallImpact = overallImpact;
    }

    public EffortEstimate getEstimatedEffort() {
        return estimatedEffort;
    }

    public void setEstimatedEffort(EffortEstimate estimatedEffort) {
        this.estimatedEffort = estimatedEffort;
    }
}
