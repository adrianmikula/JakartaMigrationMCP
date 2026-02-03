package adrianmikula.jakartamigration.intellij.model;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TypeSpec compliance tests to validate all models match specifications exactly
 * Based on TypeSpec: intellij-plugin-ui.tsp
 */
public class TypeSpecComplianceTest {

    @Test
    public void testMigrationStatusEnumMatchesTypeSpec() {
        // Verify all TypeSpec enum values are present with exact strings
        assertThat(MigrationStatus.NOT_ANALYZED.getValue()).isEqualTo("NOT_ANALYZED");
        assertThat(MigrationStatus.READY.getValue()).isEqualTo("READY");
        assertThat(MigrationStatus.HAS_BLOCKERS.getValue()).isEqualTo("HAS_BLOCKERS");
        assertThat(MigrationStatus.IN_PROGRESS.getValue()).isEqualTo("IN_PROGRESS");
        assertThat(MigrationStatus.COMPLETED.getValue()).isEqualTo("COMPLETED");
        assertThat(MigrationStatus.FAILED.getValue()).isEqualTo("FAILED");
        
        // Verify no additional enum values
        assertThat(MigrationStatus.values()).hasSize(6);
    }

    @Test
    public void testMigrationDashboardHasAllTypeSpecFields() {
        MigrationDashboard dashboard = new MigrationDashboard();
        
        // Test all TypeSpec-defined fields are accessible
        dashboard.setReadinessScore(85);
        dashboard.setStatus(MigrationStatus.READY);
        dashboard.setDependencySummary(new DependencySummary());
        dashboard.setCurrentPhase(new MigrationPhase());
        dashboard.setLastAnalyzed(Instant.now());
        
        // Verify all getters work
        assertThat(dashboard.getReadinessScore()).isEqualTo(85);
        assertThat(dashboard.getStatus()).isEqualTo(MigrationStatus.READY);
        assertThat(dashboard.getDependencySummary()).isNotNull();
        assertThat(dashboard.getCurrentPhase()).isNotNull();
        assertThat(dashboard.getLastAnalyzed()).isNotNull();
    }

    @Test
    public void testDependencySummaryHasAllTypeSpecFields() {
        DependencySummary summary = new DependencySummary();
        
        // Test all TypeSpec-defined fields
        summary.setTotalDependencies(50);
        summary.setAffectedDependencies(12);
        summary.setBlockerDependencies(2);
        summary.setMigrableDependencies(10);
        
        // Verify all getters work
        assertThat(summary.getTotalDependencies()).isEqualTo(50);
        assertThat(summary.getAffectedDependencies()).isEqualTo(12);
        assertThat(summary.getBlockerDependencies()).isEqualTo(2);
        assertThat(summary.getMigrableDependencies()).isEqualTo(10);
    }
}