package adrianmikula.jakartamigration.intellij.ui;

import com.intellij.openapi.project.Project;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Basic integration tests to validate UI components work together without runtime errors.
 * Simplified test suite focused on core robustness - avoiding complex unmaintainable tests.
 *
 * NOTE: These tests require full IntelliJ Platform environment.
 */
@org.junit.jupiter.api.Disabled("Requires full IntelliJ Platform environment - run in IDE")
public class UIIntegrationTests extends BasePlatformTestCase {
    
    @Mock
    private Project mockProject;
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.openMocks(this);
    }
    void testAllComponentsInitialization() {
        // Basic test: ensure all major UI components can be created
        try {
            new DashboardComponent(mockProject, null, null);
            new DependenciesTableComponent(mockProject);
            new PlatformsTabComponent(mockProject);
        } catch (Exception e) {
            throw new RuntimeException("Components should initialize without errors", e);
        }
    }
    void testComponentsReturnPanels() {
        // Verify components return valid panels
        DashboardComponent dashboard = new DashboardComponent(mockProject, null, null);
        if (dashboard.getPanel() == null) {
            throw new RuntimeException("Dashboard should return a panel");
        }
        
        DependenciesTableComponent dependenciesTable = new DependenciesTableComponent(mockProject);
        if (dependenciesTable.getPanel() == null) {
            throw new RuntimeException("Dependencies table should return a panel");
        }
        
        PlatformsTabComponent platformsTab = new PlatformsTabComponent(mockProject);
        if (platformsTab.getPanel() == null) {
            throw new RuntimeException("Platforms tab should return a panel");
        }
    }
    void testNullProjectHandling() {
        // Test that components handle null project data without crashing
        try {
            DashboardComponent dashboard = new DashboardComponent(mockProject, null, null);
            dashboard.updateSummary();
        } catch (Exception e) {
            throw new RuntimeException("Should handle null project data", e);
        }
    }
}
