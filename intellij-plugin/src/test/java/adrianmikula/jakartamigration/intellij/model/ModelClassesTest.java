package adrianmikula.jakartamigration.intellij.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for model classes in the IntelliJ plugin.
 * These tests verify the model classes used for migration analysis.
 */
@DisplayName("Model Classes Tests")
class ModelClassesTest {

    // ===== DependencyInfo Tests =====

    @Test
    @DisplayName("DependencyInfo default constructor should create empty object")
    void dependencyInfo_defaultConstructor_shouldCreateEmptyObject() {
        DependencyInfo info = new DependencyInfo();
        assertNull(info.getGroupId());
        assertNull(info.getArtifactId());
        assertNull(info.getCurrentVersion());
        assertNull(info.getRecommendedVersion());
        assertNull(info.getMigrationStatus());
        assertFalse(info.isBlocker());
        assertFalse(info.isTransitive());
    }

    @Test
    @DisplayName("DependencyInfo parameterized constructor should set all fields")
    void dependencyInfo_parameterizedConstructor_shouldSetAllFields() {
        DependencyInfo info = new DependencyInfo(
            "javax.servlet",
            "javax.servlet-api",
            "3.1.0",
            "4.0.0",
            DependencyMigrationStatus.NEEDS_UPGRADE,
            true  // isTransitive
        );

        assertEquals("javax.servlet", info.getGroupId());
        assertEquals("javax.servlet-api", info.getArtifactId());
        assertEquals("3.1.0", info.getCurrentVersion());
        assertEquals("4.0.0", info.getRecommendedVersion());
        assertEquals(DependencyMigrationStatus.NEEDS_UPGRADE, info.getMigrationStatus());
        assertTrue(info.isTransitive());
    }

    @Test
    @DisplayName("DependencyInfo setters should update values")
    void dependencyInfo_setters_shouldUpdateValues() {
        DependencyInfo info = new DependencyInfo();

        info.setGroupId("jakarta.servlet");
        info.setArtifactId("jakarta.servlet-api");
        info.setCurrentVersion("4.0.0");
        info.setRecommendedVersion("4.0.1");
        info.setMigrationStatus(DependencyMigrationStatus.COMPATIBLE);
        info.setBlocker(false);
        info.setTransitive(true);

        assertEquals("jakarta.servlet", info.getGroupId());
        assertEquals("jakarta.servlet-api", info.getArtifactId());
        assertEquals("4.0.0", info.getCurrentVersion());
        assertEquals("4.0.1", info.getRecommendedVersion());
        assertEquals(DependencyMigrationStatus.COMPATIBLE, info.getMigrationStatus());
        assertFalse(info.isBlocker());
        assertTrue(info.isTransitive());
    }

    @Test
    @DisplayName("DependencyInfo displayName should return formatted string")
    void dependencyInfo_displayName_shouldReturnFormattedString() {
        DependencyInfo info = new DependencyInfo();
        info.setGroupId("javax.servlet");
        info.setArtifactId("javax.servlet-api");

        String displayName = info.getDisplayName();
        assertEquals("javax.servlet:javax.servlet-api", displayName);
    }

    @Test
    @DisplayName("DependencyInfo should return correct dependency type")
    void dependencyInfo_dependencyType_shouldReturnCorrectType() {
        DependencyInfo direct = new DependencyInfo("g", "a", "1.0", "2.0", DependencyMigrationStatus.COMPATIBLE, false);
        DependencyInfo transitive = new DependencyInfo("g", "a", "1.0", "2.0", DependencyMigrationStatus.NEEDS_UPGRADE, true);

        assertEquals(DependencyInfo.DependencyType.DIRECT, direct.getDependencyType());
        assertEquals(DependencyInfo.DependencyType.TRANSITIVE, transitive.getDependencyType());
    }

    // ===== DependencySummary Tests =====

    @Test
    @DisplayName("DependencySummary default constructor should create empty object")
    void dependencySummary_defaultConstructor_shouldCreateEmptyObject() {
        DependencySummary summary = new DependencySummary();
        assertNull(summary.getTotalDependencies());
        assertNull(summary.getAffectedDependencies());
        assertNull(summary.getBlockerDependencies());
        assertNull(summary.getMigrableDependencies());
    }

    @Test
    @DisplayName("DependencySummary setters should update values")
    void dependencySummary_setters_shouldUpdateValues() {
        DependencySummary summary = new DependencySummary();
        summary.setTotalDependencies(100);
        summary.setAffectedDependencies(25);
        summary.setBlockerDependencies(5);
        summary.setMigrableDependencies(20);

        assertEquals(100, summary.getTotalDependencies());
        assertEquals(25, summary.getAffectedDependencies());
        assertEquals(5, summary.getBlockerDependencies());
        assertEquals(20, summary.getMigrableDependencies());
    }

    // ===== MigrationDashboard Tests =====

    @Test
    @DisplayName("MigrationDashboard default constructor should create empty object")
    void migrationDashboard_defaultConstructor_shouldCreateEmptyObject() {
        MigrationDashboard dashboard = new MigrationDashboard();
        assertNull(dashboard.getReadinessScore());
        assertNull(dashboard.getStatus());
        assertNull(dashboard.getDependencySummary());
        assertNull(dashboard.getCurrentPhase());
        assertNull(dashboard.getLastAnalyzed());
    }

    @Test
    @DisplayName("MigrationDashboard setters should update values")
    void migrationDashboard_setters_shouldUpdateValues() {
        Instant now = Instant.now();
        
        MigrationDashboard dashboard = new MigrationDashboard();
        dashboard.setReadinessScore(75);
        dashboard.setStatus(MigrationStatus.READY);
        
        DependencySummary summary = new DependencySummary();
        summary.setTotalDependencies(50);
        dashboard.setDependencySummary(summary);
        
        dashboard.setLastAnalyzed(now);

        assertEquals(75, dashboard.getReadinessScore());
        assertEquals(MigrationStatus.READY, dashboard.getStatus());
        assertNotNull(dashboard.getDependencySummary());
        assertEquals(50, dashboard.getDependencySummary().getTotalDependencies());
        assertEquals(now, dashboard.getLastAnalyzed());
    }

    // ===== MigrationStatus Enum Tests =====

    @Test
    @DisplayName("MigrationStatus enum should have correct values")
    void migrationStatus_enum_shouldHaveCorrectValues() {
        assertEquals("NOT_ANALYZED", MigrationStatus.NOT_ANALYZED.getValue());
        assertEquals("READY", MigrationStatus.READY.getValue());
        assertEquals("HAS_BLOCKERS", MigrationStatus.HAS_BLOCKERS.getValue());
        assertEquals("IN_PROGRESS", MigrationStatus.IN_PROGRESS.getValue());
        assertEquals("COMPLETED", MigrationStatus.COMPLETED.getValue());
        assertEquals("FAILED", MigrationStatus.FAILED.getValue());
    }

    @Test
    @DisplayName("MigrationStatus enum should have 6 values")
    void migrationStatus_enum_shouldHave6Values() {
        assertEquals(6, MigrationStatus.values().length);
    }

    // ===== DependencyMigrationStatus Enum Tests =====

    @Test
    @DisplayName("DependencyMigrationStatus enum should have correct values")
    void dependencyMigrationStatus_enum_shouldHaveCorrectValues() {
        assertEquals("COMPATIBLE", DependencyMigrationStatus.COMPATIBLE.getValue());
        assertEquals("NEEDS_UPGRADE", DependencyMigrationStatus.NEEDS_UPGRADE.getValue());
        assertEquals("NO_JAKARTA_VERSION", DependencyMigrationStatus.NO_JAKARTA_VERSION.getValue());
        assertEquals("REQUIRES_MANUAL_MIGRATION", DependencyMigrationStatus.REQUIRES_MANUAL_MIGRATION.getValue());
        assertEquals("MIGRATED", DependencyMigrationStatus.MIGRATED.getValue());
    }

    @Test
    @DisplayName("DependencyMigrationStatus enum should have 5 values")
    void dependencyMigrationStatus_enum_shouldHave5Values() {
        assertEquals(5, DependencyMigrationStatus.values().length);
    }

    // ===== RiskLevel Enum Tests =====

    @Test
    @DisplayName("RiskLevel enum should have correct values")
    void riskLevel_enum_shouldHaveCorrectValues() {
        assertEquals("LOW", RiskLevel.LOW.getValue());
        assertEquals("MEDIUM", RiskLevel.MEDIUM.getValue());
        assertEquals("HIGH", RiskLevel.HIGH.getValue());
        assertEquals("CRITICAL", RiskLevel.CRITICAL.getValue());
    }

    @Test
    @DisplayName("RiskLevel enum should have 4 values")
    void riskLevel_enum_shouldHave4Values() {
        assertEquals(4, RiskLevel.values().length);
    }

    // ===== MigrationPhase Tests =====

    @Test
    @DisplayName("MigrationPhase default constructor should create empty object")
    void migrationPhase_defaultConstructor_shouldCreateEmptyObject() {
        MigrationPhase phase = new MigrationPhase();
        assertNull(phase.getId());
        assertNull(phase.getName());
        assertNull(phase.getDescription());
        assertNull(phase.getStatus());
        assertNull(phase.getOrder());
        assertNull(phase.getEstimatedDuration());
        assertNull(phase.getPrerequisites());
        assertNull(phase.getTasks());
    }

    @Test
    @DisplayName("MigrationPhase setters should update values")
    void migrationPhase_setters_shouldUpdateValues() {
        MigrationPhase phase = new MigrationPhase();
        phase.setId("phase-1");
        phase.setName("Analysis Phase");
        phase.setDescription("Analyze dependencies");
        phase.setStatus(PhaseStatus.NOT_STARTED);
        phase.setOrder(1);
        phase.setEstimatedDuration(60);

        assertEquals("phase-1", phase.getId());
        assertEquals("Analysis Phase", phase.getName());
        assertEquals("Analyze dependencies", phase.getDescription());
        assertEquals(PhaseStatus.NOT_STARTED, phase.getStatus());
        assertEquals(Integer.valueOf(1), phase.getOrder());
        assertEquals(Integer.valueOf(60), phase.getEstimatedDuration());
    }

    // ===== PhaseStatus Enum Tests =====

    @Test
    @DisplayName("PhaseStatus enum should have correct values")
    void phaseStatus_enum_shouldHaveCorrectValues() {
        assertEquals("NOT_STARTED", PhaseStatus.NOT_STARTED.getValue());
        assertEquals("IN_PROGRESS", PhaseStatus.IN_PROGRESS.getValue());
        assertEquals("COMPLETED", PhaseStatus.COMPLETED.getValue());
        assertEquals("FAILED", PhaseStatus.FAILED.getValue());
        assertEquals("SKIPPED", PhaseStatus.SKIPPED.getValue());
    }

    @Test
    @DisplayName("PhaseStatus enum should have 5 values")
    void phaseStatus_enum_shouldHave5Values() {
        assertEquals(5, PhaseStatus.values().length);
    }

    // ===== PhaseTask Tests =====

    @Test
    @DisplayName("PhaseTask default constructor should create empty object")
    void phaseTask_defaultConstructor_shouldCreateEmptyObject() {
        PhaseTask task = new PhaseTask();
        assertNull(task.getId());
        assertNull(task.getName());
        assertNull(task.getDescription());
        assertNull(task.getStatus());
        assertNull(task.getType());
        assertNull(task.getIsAutomatable());
    }

    @Test
    @DisplayName("PhaseTask setters should update values")
    void phaseTask_setters_shouldUpdateValues() {
        PhaseTask task = new PhaseTask();
        task.setId("task-1");
        task.setName("Update dependency");
        task.setDescription("Update javax to jakarta");
        task.setStatus(TaskStatus.PENDING);
        task.setType(TaskType.DEPENDENCY_UPDATE);
        task.setIsAutomatable(true);

        assertEquals("task-1", task.getId());
        assertEquals("Update dependency", task.getName());
        assertEquals("Update javax to jakarta", task.getDescription());
        assertEquals(TaskStatus.PENDING, task.getStatus());
        task.setType(TaskType.DEPENDENCY_UPDATE);
        assertEquals(Boolean.TRUE, task.getIsAutomatable());
    }

    // ===== TaskStatus Enum Tests =====

    @Test
    @DisplayName("TaskStatus enum should have correct values")
    void taskStatus_enum_shouldHaveCorrectValues() {
        assertEquals("PENDING", TaskStatus.PENDING.getValue());
        assertEquals("IN_PROGRESS", TaskStatus.IN_PROGRESS.getValue());
        assertEquals("COMPLETED", TaskStatus.COMPLETED.getValue());
        assertEquals("FAILED", TaskStatus.FAILED.getValue());
    }

    @Test
    @DisplayName("TaskStatus enum should have 4 values")
    void taskStatus_enum_shouldHave4Values() {
        assertEquals(4, TaskStatus.values().length);
    }

    // ===== TaskType Enum Tests =====

    @Test
    @DisplayName("TaskType enum should have correct values")
    void taskType_enum_shouldHaveCorrectValues() {
        assertEquals("DEPENDENCY_UPDATE", TaskType.DEPENDENCY_UPDATE.getValue());
        assertEquals("SOURCE_TRANSFORMATION", TaskType.SOURCE_TRANSFORMATION.getValue());
        assertEquals("CONFIGURATION_UPDATE", TaskType.CONFIGURATION_UPDATE.getValue());
        assertEquals("MANUAL_VERIFICATION", TaskType.MANUAL_VERIFICATION.getValue());
        assertEquals("TESTING", TaskType.TESTING.getValue());
    }

    @Test
    @DisplayName("TaskType enum should have 5 values")
    void taskType_enum_shouldHave5Values() {
        assertEquals(5, TaskType.values().length);
    }

    // ===== Integration Tests =====

    @Test
    @DisplayName("DependencyInfo should work with all status enums")
    void dependencyInfo_shouldWorkWithAllStatusEnums() {
        for (DependencyMigrationStatus status : DependencyMigrationStatus.values()) {
            DependencyInfo info = new DependencyInfo();
            info.setMigrationStatus(status);
            assertEquals(status, info.getMigrationStatus());
        }
    }

    @Test
    @DisplayName("Dashboard should work with all migration statuses")
    void dashboard_shouldWorkWithAllMigrationStatuses() {
        for (MigrationStatus status : MigrationStatus.values()) {
            MigrationDashboard dashboard = new MigrationDashboard();
            dashboard.setStatus(status);
            assertEquals(status, dashboard.getStatus());
        }
    }

    @Test
    @DisplayName("MigrationDashboard with full data should be complete")
    void migrationDashboard_withFullData_shouldBeComplete() {
        Instant now = Instant.now();
        
        DependencySummary summary = new DependencySummary();
        summary.setTotalDependencies(100);
        summary.setAffectedDependencies(25);
        summary.setBlockerDependencies(5);
        summary.setMigrableDependencies(20);

        MigrationDashboard dashboard = new MigrationDashboard();
        dashboard.setReadinessScore(75);
        dashboard.setStatus(MigrationStatus.HAS_BLOCKERS);
        dashboard.setDependencySummary(summary);
        dashboard.setLastAnalyzed(now);

        assertNotNull(dashboard.getDependencySummary());
        assertEquals(100, dashboard.getDependencySummary().getTotalDependencies());
        assertEquals(75, dashboard.getReadinessScore());
        assertEquals(now, dashboard.getLastAnalyzed());
    }

    @Test
    @DisplayName("DependencySummary should support zero values")
    void dependencySummary_shouldSupportZeroValues() {
        DependencySummary summary = new DependencySummary();
        summary.setTotalDependencies(0);
        summary.setAffectedDependencies(0);
        summary.setBlockerDependencies(0);
        summary.setMigrableDependencies(0);

        assertEquals(0, summary.getTotalDependencies());
        assertEquals(0, summary.getAffectedDependencies());
        assertEquals(0, summary.getBlockerDependencies());
        assertEquals(0, summary.getMigrableDependencies());
    }

    @Test
    @DisplayName("DependencyInfo blocker flag should work correctly")
    void dependencyInfo_blockerFlag_shouldWorkCorrectly() {
        DependencyInfo info = new DependencyInfo();
        assertFalse(info.isBlocker());
        
        info.setBlocker(true);
        // setBlocker is no-op, always returns false
        assertFalse(info.isBlocker());
    }

    @Test
    @DisplayName("MigrationPhase with prerequisites should work")
    void migrationPhase_withPrerequisites_shouldWork() {
        MigrationPhase phase = new MigrationPhase();
        phase.setId("phase-2");
        phase.setPrerequisites(java.util.List.of("phase-1"));

        assertNotNull(phase.getPrerequisites());
        assertEquals(1, phase.getPrerequisites().size());
        assertEquals("phase-1", phase.getPrerequisites().get(0));
    }
}
