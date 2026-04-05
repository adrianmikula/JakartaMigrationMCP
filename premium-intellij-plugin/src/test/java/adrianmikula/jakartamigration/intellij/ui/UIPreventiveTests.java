package adrianmikula.jakartamigration.intellij.ui;

import com.intellij.openapi.project.Project;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Minimal UI tests focused on core component initialization and robustness.
 * Simplified to avoid complex setup and ensure maintainability.
 */
public class UIPreventiveTests extends BasePlatformTestCase {
    
    @Test
    @DisplayName("DashboardComponent should initialize without exceptions")
    void testDashboardComponentInitialization() {
        Project project = getProject();
        assertDoesNotThrow(() -> {
            DashboardComponent dashboard = new DashboardComponent(project, null, null);
            assertNotNull(dashboard.getPanel());
        });
    }
    
    @Test
    @DisplayName("DependenciesTableComponent should initialize without exceptions")
    void testDependenciesTableComponentInitialization() {
        Project project = getProject();
        assertDoesNotThrow(() -> {
            DependenciesTableComponent component = new DependenciesTableComponent(project);
            assertNotNull(component.getPanel());
        });
    }
    
    @Test
    @DisplayName("PlatformsTabComponent should initialize without exceptions")
    void testPlatformsTabComponentInitialization() {
        Project project = getProject();
        assertDoesNotThrow(() -> {
            PlatformsTabComponent component = new PlatformsTabComponent(project);
            assertNotNull(component.getPanel());
        });
    }
    
    @Test
    @DisplayName("ComprehensiveReportsTabComponent should initialize without exceptions")
    void testComprehensiveReportsTabComponentInitialization() {
        Project project = getProject();
        assertDoesNotThrow(() -> {
            ComprehensiveReportsTabComponent component = new ComprehensiveReportsTabComponent(project);
            assertNotNull(component.getPanel());
        });
    }
    
    @Test
    @DisplayName("UI components should handle null services gracefully")
    void testNullServiceHandling() {
        Project project = getProject();
        assertDoesNotThrow(() -> {
            // Components should handle null services without crashing
            new DashboardComponent(project, null, null);
            new DependenciesTableComponent(project);
        });
    }
}
