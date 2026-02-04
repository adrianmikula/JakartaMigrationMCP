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

    @Test
    public void testDependencyMigrationStatusEnumMatchesTypeSpec() {
        // Verify all TypeSpec enum values are present with exact strings
        assertThat(DependencyMigrationStatus.COMPATIBLE.getValue()).isEqualTo("COMPATIBLE");
        assertThat(DependencyMigrationStatus.NEEDS_UPGRADE.getValue()).isEqualTo("NEEDS_UPGRADE");
        assertThat(DependencyMigrationStatus.NO_JAKARTA_VERSION.getValue()).isEqualTo("NO_JAKARTA_VERSION");
        assertThat(DependencyMigrationStatus.REQUIRES_MANUAL_MIGRATION.getValue()).isEqualTo("REQUIRES_MANUAL_MIGRATION");
        assertThat(DependencyMigrationStatus.MIGRATED.getValue()).isEqualTo("MIGRATED");

        // Verify no additional enum values
        assertThat(DependencyMigrationStatus.values()).hasSize(5);
    }

    @Test
    public void testDependencyInfoHasAllTypeSpecFields() {
        DependencyInfo info = new DependencyInfo();

        // Test all TypeSpec-defined fields
        info.setGroupId("org.springframework");
        info.setArtifactId("spring-beans");
        info.setCurrentVersion("5.3.27");
        info.setRecommendedVersion("6.0.9");
        info.setMigrationStatus(DependencyMigrationStatus.NEEDS_UPGRADE);
        info.setTransitive(false);

        // Verify all getters work
        assertThat(info.getGroupId()).isEqualTo("org.springframework");
        assertThat(info.getArtifactId()).isEqualTo("spring-beans");
        assertThat(info.getCurrentVersion()).isEqualTo("5.3.27");
        assertThat(info.getRecommendedVersion()).isEqualTo("6.0.9");
        assertThat(info.getMigrationStatus()).isEqualTo(DependencyMigrationStatus.NEEDS_UPGRADE);
        assertThat(info.isTransitive()).isFalse();
        assertThat(info.getDisplayName()).isEqualTo("org.springframework:spring-beans");
    }

    @Test
    public void testDependencyInfoConstructor() {
        DependencyInfo info = new DependencyInfo(
            "javax.xml.bind",
            "jaxb-api",
            "2.3.1",
            null,
            DependencyMigrationStatus.NO_JAKARTA_VERSION,
            true
        );

        assertThat(info.getGroupId()).isEqualTo("javax.xml.bind");
        assertThat(info.getArtifactId()).isEqualTo("jaxb-api");
        assertThat(info.getCurrentVersion()).isEqualTo("2.3.1");
        assertThat(info.getRecommendedVersion()).isNull();
        assertThat(info.getMigrationStatus()).isEqualTo(DependencyMigrationStatus.NO_JAKARTA_VERSION);
        assertThat(info.isTransitive()).isTrue();
    }

    @Test
    public void testPhaseStatusEnumMatchesTypeSpec() {
        // Verify all TypeSpec enum values are present with exact strings
        assertThat(PhaseStatus.NOT_STARTED.getValue()).isEqualTo("NOT_STARTED");
        assertThat(PhaseStatus.IN_PROGRESS.getValue()).isEqualTo("IN_PROGRESS");
        assertThat(PhaseStatus.COMPLETED.getValue()).isEqualTo("COMPLETED");
        assertThat(PhaseStatus.FAILED.getValue()).isEqualTo("FAILED");
        assertThat(PhaseStatus.SKIPPED.getValue()).isEqualTo("SKIPPED");

        // Verify no additional enum values
        assertThat(PhaseStatus.values()).hasSize(5);
    }
}
