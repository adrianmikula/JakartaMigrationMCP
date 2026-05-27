package adrianmikula.jakartamigration.intellij.ui;

import com.intellij.openapi.project.Project;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

/**
 * Minimal UI tests focused on core component initialization and robustness.
 * Simplified to avoid complex setup and ensure maintainability.
 *
 * NOTE: These tests require full IntelliJ Platform environment.
 */
@SuppressWarnings("deprecation")
public class UIPreventiveTests extends BasePlatformTestCase {
    public void testDashboardComponentInitialization() {
        Project project = getProject();
        try {
            DashboardComponent dashboard = new DashboardComponent(project, null, null);
            if (dashboard.getPanel() == null) {
                throw new RuntimeException("Panel should not be null");
            }
        } catch (Exception e) {
            throw new RuntimeException("DashboardComponent initialization failed: " + e.getMessage(), e);
        }
    }
    public void testDependenciesTableComponentInitialization() {
        Project project = getProject();
        try {
            DependenciesTableComponent component = new DependenciesTableComponent(project);
            if (component.getPanel() == null) {
                throw new RuntimeException("Panel should not be null");
            }
        } catch (Exception e) {
            throw new RuntimeException("DependenciesTableComponent initialization failed: " + e.getMessage(), e);
        }
    }
    public void testPlatformsTabComponentInitialization() {
        Project project = getProject();
        try {
            PlatformsTabComponent component = new PlatformsTabComponent(project);
            if (component.getPanel() == null) {
                throw new RuntimeException("Panel should not be null");
            }
        } catch (Exception e) {
            throw new RuntimeException("PlatformsTabComponent initialization failed: " + e.getMessage(), e);
        }
    }
    public void testNullServiceHandling() {
        Project project = getProject();
        try {
            // Components should handle null services without crashing
            new DashboardComponent(project, null, null);
            new DependenciesTableComponent(project);
        } catch (Exception e) {
            throw new RuntimeException("Null service handling failed: " + e.getMessage(), e);
        }
    }
}
