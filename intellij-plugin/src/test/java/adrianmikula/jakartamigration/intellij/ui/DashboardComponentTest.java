package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.model.MigrationDashboard;
import adrianmikula.jakartamigration.intellij.model.MigrationStatus;
import adrianmikula.jakartamigration.intellij.model.DependencySummary;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.time.Instant;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UI tests for DashboardComponent based on TypeSpec: plugin-components.tsp
 */
public class DashboardComponentTest extends LightJavaCodeInsightFixtureTestCase {

    private DashboardComponent dashboardComponent;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        dashboardComponent = new DashboardComponent(getProject(), e -> {});
    }

    @Test
    public void testDashboardComponentStructure() {
        JPanel panel = dashboardComponent.getPanel();
        assertThat(panel).isNotNull();

        // Verify TypeSpec-required components are present
        assertThat(panel.getComponentCount()).isGreaterThan(0);

        // Check for header and action buttons
        assertThat(findComponentByText(panel, "Migration Summary")).isNotNull();
        assertThat(findComponentByText(panel, "▶ Analyze Project")).isNotNull(); // Analyze button
        assertThat(findComponentByType(panel, JButton.class)).isNotNull(); // Refresh button
        // Check for MCP tool buttons
        assertThat(findComponentByText(panel, "Analyze Readiness")).isNotNull(); // MCP tool button
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

    @Test
    public void testDirectSetters() {
        // Test direct setters that were added for easier MCP integration
        dashboardComponent.setReadinessScore(75);
        dashboardComponent.setStatus(MigrationStatus.HAS_BLOCKERS);
        dashboardComponent.setDependencySummary(100, 25, 5);
        dashboardComponent.setLastAnalyzed(Instant.now());
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

    @SuppressWarnings("unchecked")
    private <T> T findComponentByType(java.awt.Container container, Class<T> type) {
        for (int i = 0; i < container.getComponentCount(); i++) {
            java.awt.Component component = container.getComponent(i);
            if (type.isInstance(component)) {
                return (T) component;
            }
            if (component instanceof java.awt.Container) {
                T found = findComponentByType((java.awt.Container) component, type);
                if (found != null) return found;
            }
        }
        return null;
    }
}
