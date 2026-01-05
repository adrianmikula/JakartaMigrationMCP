package com.bugbounty.jakartamigration.dependencyanalysis.domain;

import java.util.List;

/**
 * Risk assessment for Jakarta migration.
 */
public record RiskAssessment(
    double riskScore,  // 0.0 to 1.0
    List<String> riskFactors,
    List<String> mitigationSuggestions
) {
    public RiskAssessment {
        if (riskScore < 0.0 || riskScore > 1.0) {
            throw new IllegalArgumentException("Risk score must be between 0.0 and 1.0");
        }
    }
}

