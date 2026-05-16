package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.model.DependencyInfo;
import adrianmikula.jakartamigration.intellij.model.DependencyMigrationStatus;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for DependencyGraphComponent to ensure it handles null values gracefully.
 * Tests the fix for NullPointerException when currentVersion is null.
 */
public class DependencyGraphComponentTest extends BasePlatformTestCase {

    private DependencyGraphComponent graphComponent;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        graphComponent = new DependencyGraphComponent(getProject());
    }

    public void testInitialization() {
        assertThat(graphComponent.getPanel()).isNotNull();
    }

    /**
     * Test that updateGraph handles dependencies with null currentVersion gracefully.
     * This is a regression test for the NullPointerException that occurred when
     * Artifact constructor received a null version parameter.
     */
    public void testUpdateGraphWithNullCurrentVersion() {
        List<DependencyInfo> deps = new ArrayList<>();
        
        // Create a dependency with null currentVersion
        DependencyInfo depWithNullVersion = new DependencyInfo(
            "org.example", "test-lib", null, 
            null, null, null,
            "Unknown", null, 
            DependencyMigrationStatus.UNKNOWN, false, false);
        deps.add(depWithNullVersion);

        // This should not throw NullPointerException
        graphComponent.updateGraph(deps);
        
        // Verify the panel is still valid (graph was updated without crashing)
        assertThat(graphComponent.getPanel()).isNotNull();
    }

    /**
     * Test that updateGraph handles a mix of dependencies with valid and null versions.
     */
    public void testUpdateGraphWithMixedVersions() {
        List<DependencyInfo> deps = new ArrayList<>();
        
        // Add dependency with valid version
        deps.add(new DependencyInfo(
            "org.example", "valid-lib", "1.0.0",
            null, null, null,
            "Compatible", null,
            DependencyMigrationStatus.COMPATIBLE, false, false));
        
        // Add dependency with null version
        deps.add(new DependencyInfo(
            "org.example", "null-lib", null,
            null, null, null,
            "Unknown", null,
            DependencyMigrationStatus.UNKNOWN, false, false));

        // This should not throw NullPointerException
        graphComponent.updateGraph(deps);
        
        // Verify the panel is still valid
        assertThat(graphComponent.getPanel()).isNotNull();
    }

    /**
     * Test that updateGraph handles empty dependency list gracefully.
     */
    public void testUpdateGraphWithEmptyList() {
        List<DependencyInfo> deps = new ArrayList<>();
        
        graphComponent.updateGraph(deps);
        
        // Verify the panel is still valid
        assertThat(graphComponent.getPanel()).isNotNull();
    }

    /**
     * Test that updateGraph handles null dependency list gracefully.
     */
    public void testUpdateGraphWithNullList() {
        graphComponent.updateGraph(null);
        
        // Verify the panel is still valid
        assertThat(graphComponent.getPanel()).isNotNull();
    }
}
