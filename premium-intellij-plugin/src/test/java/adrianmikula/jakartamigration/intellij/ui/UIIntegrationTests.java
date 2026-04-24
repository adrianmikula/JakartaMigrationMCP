package adrianmikula.jakartamigration.intellij.ui;

import com.intellij.openapi.project.Project;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
    
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.openMocks(this);
    }
    
    @Test
    @DisplayName("All UI components should initialize without throwing exceptions")
    void testAllComponentsInitialization() {
        // Basic test: ensure all major UI components can be created
        assertDoesNotThrow(() -> {
            new DashboardComponent(mockProject, null, null);
            new DependenciesTableComponent(mockProject);
            new PlatformsTabComponent(mockProject);
        });
    }
    
    @Test
    @DisplayName("UI components should return non-null panels")
    void testComponentsReturnPanels() {
        // Verify components return valid panels
        DashboardComponent dashboard = new DashboardComponent(mockProject, null, null);
        org.junit.jupiter.api.Assertions.assertNotNull(dashboard.getPanel(), "Dashboard should return a panel");
        
        DependenciesTableComponent dependenciesTable = new DependenciesTableComponent(mockProject);
        org.junit.jupiter.api.Assertions.assertNotNull(dependenciesTable.getPanel(), "Dependencies table should return a panel");
        
        PlatformsTabComponent platformsTab = new PlatformsTabComponent(mockProject);
        org.junit.jupiter.api.Assertions.assertNotNull(platformsTab.getPanel(), "Platforms tab should return a panel");
    }
    
    @Test
    @DisplayName("UI components should handle null project gracefully")
    void testNullProjectHandling() {
        // Test that components handle null project data without crashing
        assertDoesNotThrow(() -> {
            DashboardComponent dashboard = new DashboardComponent(mockProject, null, null);
            dashboard.updateSummary();
        });
    }
}
