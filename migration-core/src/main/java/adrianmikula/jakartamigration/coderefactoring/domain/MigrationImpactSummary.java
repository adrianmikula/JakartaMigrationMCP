package adrianmikula.jakartamigration.coderefactoring.domain;

import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyAnalysisReport;
import adrianmikula.jakartamigration.dependencyanalysis.domain.RiskAssessment;
import adrianmikula.jakartamigration.sourcecodescanning.domain.SourceCodeAnalysisResult;

import java.time.Duration;
import java.util.Objects;

/**
 * Comprehensive migration impact summary combining dependency analysis and source code scanning.
 */
public record MigrationImpactSummary(
    DependencyAnalysisReport dependencyAnalysis,
    SourceCodeAnalysisResult sourceCodeAnalysis,
    int totalFilesToMigrate,
    int totalJavaxImports,
    int totalBlockers,
    int totalRecommendations,
    Duration estimatedEffort,
    RiskAssessment overallRisk,
    MigrationComplexity complexity
) {
    public MigrationImpactSummary {
        Objects.requireNonNull(dependencyAnalysis, "dependencyAnalysis cannot be null");
        Objects.requireNonNull(sourceCodeAnalysis, "sourceCodeAnalysis cannot be null");
        Objects.requireNonNull(overallRisk, "overallRisk cannot be null");
        Objects.requireNonNull(complexity, "complexity cannot be null");
        if (totalFilesToMigrate < 0) {
            throw new IllegalArgumentException("totalFilesToMigrate cannot be negative");
        }
        if (totalJavaxImports < 0) {
            throw new IllegalArgumentException("totalJavaxImports cannot be negative");
        }
        if (totalBlockers < 0) {
            throw new IllegalArgumentException("totalBlockers cannot be negative");
        }
        if (totalRecommendations < 0) {
            throw new IllegalArgumentException("totalRecommendations cannot be negative");
        }
        if (estimatedEffort == null || estimatedEffort.isNegative()) {
            throw new IllegalArgumentException("estimatedEffort cannot be null or negative");
        }
    }
    
    /**
     * Creates a migration impact summary from dependency analysis and source code analysis.
     */
    public static MigrationImpactSummary from(
            DependencyAnalysisReport dependencyAnalysis,
            SourceCodeAnalysisResult sourceCodeAnalysis) {
        
        int totalFilesToMigrate = sourceCodeAnalysis.totalFilesWithJavaxUsage();
        int totalJavaxImports = sourceCodeAnalysis.totalJavaxImports();
        int totalBlockers = dependencyAnalysis.blockers().size();
        int totalRecommendations = dependencyAnalysis.recommendations().size();
        
        // Estimate effort: 2 minutes per file + 1 minute per import + blocker resolution time
        Duration fileEffort = Duration.ofMinutes(totalFilesToMigrate * 2L);
        Duration importEffort = Duration.ofMinutes(totalJavaxImports);
        Duration blockerEffort = Duration.ofMinutes(totalBlockers * 30L); // 30 min per blocker
        Duration estimatedEffort = fileEffort.plus(importEffort).plus(blockerEffort);
        
        RiskAssessment overallRisk = dependencyAnalysis.riskAssessment();
        
        // Determine complexity based on metrics
        MigrationComplexity complexity = determineComplexity(
            totalFilesToMigrate,
            totalJavaxImports,
            totalBlockers,
            overallRisk.riskScore()
        );
        
        return new MigrationImpactSummary(
            dependencyAnalysis,
            sourceCodeAnalysis,
            totalFilesToMigrate,
            totalJavaxImports,
            totalBlockers,
            totalRecommendations,
            estimatedEffort,
            overallRisk,
            complexity
        );
    }
    
    private static MigrationComplexity determineComplexity(
            int fileCount,
            int importCount,
            int blockerCount,
            double riskScore) {
        
        if (blockerCount > 5 || riskScore > 0.8) {
            return MigrationComplexity.HIGH;
        }
        
        if (fileCount > 50 || importCount > 100 || blockerCount > 2 || riskScore > 0.5) {
            return MigrationComplexity.MEDIUM;
        }
        
        return MigrationComplexity.LOW;
    }
    
    /**
     * Migration complexity levels.
     */
    public enum MigrationComplexity {
        LOW,
        MEDIUM,
        HIGH
    }
}

