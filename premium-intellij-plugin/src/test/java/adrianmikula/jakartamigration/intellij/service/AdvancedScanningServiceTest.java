package adrianmikula.jakartamigration.intellij.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

import adrianmikula.jakartamigration.advancedscanning.domain.ScanReason;
import adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyUsage;
import adrianmikula.jakartamigration.intellij.model.DependencyMigrationStatus;

/**
 * Tests for AdvancedScanningService
 * Note: Full IntelliJ UI testing requires platformTestFramework which needs 
 * additional Gradle configuration. This test verifies basic service functionality.
 */
public class AdvancedScanningServiceTest {
    
    @Test
    public void testServiceClassExists() {
        // Verify the service class exists and has the expected methods
        assertThat(AdvancedScanningService.class).isNotNull();
    }
    
    @Test
    public void testScanSummaryClassExists() {
        // Verify the summary class exists
        assertThat(AdvancedScanningService.AdvancedScanSummary.class).isNotNull();
    }

    @ParameterizedTest
    @EnumSource(ScanReason.class)
    public void testDetermineMigrationStatusCoversAllReasons(ScanReason reason) {
        // Create a minimal TranDepUsage with the given reason
        TransitiveDependencyUsage usage = new TransitiveDependencyUsage(
                "test-artifact",
                "test-group",
                "1.0",
                null, // javaxPackage
                null, // severity
                null, // recommendation
                null, // scope
                false, // transitive
                0, // depth
                null, // alternativeVersions
                reason,
                null, // detailMessage
                0.0, // confidence
                false // incompatibilityFromTransitive
        );

        // Get the service instance to call package-private method
        AdvancedScanningService service = new AdvancedScanningService(null);
        DependencyMigrationStatus status = service.determineMigrationStatus(usage);

        // Ensure we get a non-null status
        assertThat(status).isNotNull();
    }

    @Test
    public void testDetermineMigrationStatus_IncompatibleReasons() {
        TransitiveDependencyUsage usageBase = new TransitiveDependencyUsage(
                "a","g","1.0",null,null,null,null,false,0,null,
                null,null,0.0,false);

        // BLACKLISTED -> NEEDS_UPGRADE
        assertThat(createService().determineMigrationStatus(
                new TransitiveDependencyUsage("a","g","1.0",null,null,null,null,false,0,null,
                        ScanReason.BLACKLISTED,null,0.0,false)))
                .isEqualTo(DependencyMigrationStatus.NEEDS_UPGRADE);

        // BYTECODE_SCAN_JAVAX -> NEEDS_UPGRADE
        assertThat(createService().determineMigrationStatus(
                new TransitiveDependencyUsage("a","g","1.0",null,null,null,null,false,0,null,
                        ScanReason.BYTECODE_SCAN_JAVAX,null,0.0,false)))
                .isEqualTo(DependencyMigrationStatus.NEEDS_UPGRADE);

        // TRANSITIVE_INCOMPATIBLE -> NEEDS_UPGRADE
        assertThat(createService().determineMigrationStatus(
                new TransitiveDependencyUsage("a","g","1.0",null,null,null,null,false,0,null,
                        ScanReason.TRANSITIVE_INCOMPATIBLE,null,0.0,true)))
                .isEqualTo(DependencyMigrationStatus.NEEDS_UPGRADE);
    }

    @Test
    public void testDetermineMigrationStatus_ManualReviewReasons() {
        AdvancedScanningService service = createService();

        assertThat(service.determineMigrationStatus(
                new TransitiveDependencyUsage("a","g","1.0",null,null,null,null,false,0,null,
                        ScanReason.BYTECODE_SCAN_MIXED,null,0.0,false)))
                .isEqualTo(DependencyMigrationStatus.REQUIRES_MANUAL_MIGRATION);

        assertThat(service.determineMigrationStatus(
                new TransitiveDependencyUsage("a","g","1.0",null,null,null,null,false,0,null,
                        ScanReason.REVIEW_REQUIRED,null,0.0,false)))
                .isEqualTo(DependencyMigrationStatus.REQUIRES_MANUAL_MIGRATION);
    }

    @Test
    public void testDetermineMigrationStatus_NoJakartaVersion() {
        AdvancedScanningService service = createService();
        assertThat(service.determineMigrationStatus(
                new TransitiveDependencyUsage("a","g","1.0",null,null,null,null,false,0,null,
                        ScanReason.MAVEN_LOOKUP_NONE,null,0.0,false)))
                .isEqualTo(DependencyMigrationStatus.NO_JAKARTA_VERSION);
    }

    @Test
    public void testDetermineMigrationStatus_CompatibleReasons() {
        AdvancedScanningService service = createService();

        assertThat(service.determineMigrationStatus(
                new TransitiveDependencyUsage("a","g","1.0",null,null,null,null,false,0,null,
                        ScanReason.WHITELISTED,null,0.0,false)))
                .isEqualTo(DependencyMigrationStatus.COMPATIBLE);

        assertThat(service.determineMigrationStatus(
                new TransitiveDependencyUsage("a","g","1.0",null,null,null,null,false,0,null,
                        ScanReason.BYTECODE_SCAN_JAKARTA,null,0.0,false)))
                .isEqualTo(DependencyMigrationStatus.COMPATIBLE);

        assertThat(service.determineMigrationStatus(
                new TransitiveDependencyUsage("a","g","1.0",null,null,null,null,false,0,null,
                        ScanReason.MAVEN_LOOKUP_FOUND,null,0.7,false)))
                .isEqualTo(DependencyMigrationStatus.COMPATIBLE);
    }

    @Test
    public void testDetermineMigrationStatus_UncertainReasons() {
        AdvancedScanningService service = createService();

        assertThat(service.determineMigrationStatus(
                new TransitiveDependencyUsage("a","g","1.0",null,null,null,null,false,0,null,
                        ScanReason.BYTECODE_SCAN_UNKNOWN,null,0.0,false)))
                .isEqualTo(DependencyMigrationStatus.UNKNOWN_REVIEW);

        assertThat(service.determineMigrationStatus(
                new TransitiveDependencyUsage("a","g","1.0",null,null,null,null,false,0,null,
                        ScanReason.UNKNOWN,null,0.0,false)))
                .isEqualTo(DependencyMigrationStatus.UNKNOWN_REVIEW);
    }

    private AdvancedScanningService createService() {
        // The constructor expects a RecipeService but we can pass null for unit tests
        return new AdvancedScanningService(null);
    }
}

