package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.model.MigrationDashboard;
import adrianmikula.jakartamigration.intellij.model.MigrationStatus;
import adrianmikula.jakartamigration.intellij.model.DependencySummary;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import javax.swing.*;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UI tests for DashboardComponent based on TypeSpec: plugin-components.tsp
 */
public class DashboardComponentTest extends LightJavaCodeInsightFixtureTestCase {

    private DashboardComponent dashboardComponent;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        dashboardComponent = new DashboardComponent(getProject());
    }

    @Test
    public void testDashboardComponentStructure() {
        JPanel panel = dashboardComponent.getPanel();
        assertThat(panel).isNotNull();
        
        // Verify TypeSpec-required components are present
        assertThat(panel.getComponentCount()).isGreaterThan(0);
        
        // Check for header, content, and actions panels
        assertThat(findComponentByText(panel, "Migration Dashboard")).isNotNull();
        assertThat(findComponentByText(panel, "Readiness Score:")).isNotNull();
        assertThat(findComponentByText(panel, "Status:")).isNotNull();
        assertThat(findComponentByText(panel, "Refresh Analysis")).isNotNull();
        assertThat(findComponentByText(panel, "Start Migration")).isNotNull();
    }

    @Test
    public void testUpdateDashboardWithTypeSpecData() {
        // Create TypeSpec-compliant dashboard data
        MigrationDashboard dashboard = new MigrationDashboard();
        dashboard.setReadinessScore(85);
        dashboard.setStatus(MigrationStatus.READY);
        dashboard.setLastAnalyzed(Instant.now());
        
        DependencySummary summary = new DependencySummary();
        summary.setTotalDependencies(50);
        summary.setAffectedDependencies(12);
        summary.setBlockerDependencies(2);
        summary.setMigrableDependencies(10);
        dashboard.setDependencySummary(summary);

        // Test update method
        dashboardComponent.updateDashboard(dashboard);
    }

    private JComponent findComponentByText(JPanel panel, String text) {
        for (int i = 0; i < panel.getComponentCount(); i++) {
            JComponent component = findComponentByTextRecursive(panel.getComponent(i), text);
            if (component != null) return component;
        }
        return null;
    }

    private JComponent findComponentByTextRecursive(java.awt.Component component, String text) {
        if (component instanceof JLabel && text.equals(((JLabel) component).getText())) {
            return (JComponent) component;
        }
        if (component instanceof JButton && text.equals(((JButton) component).getText())) {
            return (JComponent) component;
        }
        if (component instanceof java.awt.Container) {
            java.awt.Container container = (java.awt.Container) component;
            for (int i = 0; i < container.getComponentCount(); i++) {
                JComponent found = findComponentByTextRecursive(container.getComponent(i), text);
                if (found != null) return found;
            }
        }
        return null;
    }
}