package adrianmikula.jakartamigration.intellij.ui;

import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for DependencyGraphComponent UI component
 */
public class DependencyGraphComponentTest extends LightJavaCodeInsightFixtureTestCase {

    private DependencyGraphComponent graphComponent;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        graphComponent = new DependencyGraphComponent(getProject());
    }

    @Test
    public void testGraphComponentInitialization() {
        JPanel panel = graphComponent.getPanel();
        assertThat(panel).isNotNull();
        assertThat(panel.getComponentCount()).isGreaterThan(0);
    }

    @Test
    public void testGraphComponentHasHeader() {
        JPanel panel = graphComponent.getPanel();
        // Find the header panel (first component)
        assertThat(panel.getComponent(0)).isInstanceOf(JPanel.class);
    }

    @Test
    public void testGraphComponentHasGraphPanel() {
        JPanel panel = graphComponent.getPanel();
        // Find JPanel with BorderLayout (the graph visualization area)
        JPanel graphPanel = findGraphPanel(panel);
        assertThat(graphPanel).isNotNull();
        assertThat(graphPanel.getBorder()).isNotNull();
    }

    @Test
    public void testGraphComponentHasControls() {
        JPanel panel = graphComponent.getPanel();
        // Find controls panel (should contain buttons)
        JButton zoomInButton = findButtonByText(panel, "Zoom In");
        JButton zoomOutButton = findButtonByText(panel, "Zoom Out");
        JButton resetViewButton = findButtonByText(panel, "Reset View");
        
        assertThat(zoomInButton).isNotNull();
        assertThat(zoomOutButton).isNotNull();
        assertThat(resetViewButton).isNotNull();
    }

    @Test
    public void testGraphComponentHasLayoutSelector() {
        JPanel panel = graphComponent.getPanel();
        JComboBox<?> layoutCombo = findComboBox(panel);
        assertThat(layoutCombo).isNotNull();
    }

    @Test
    public void testGraphComponentHasCriticalPathOption() {
        JPanel panel = graphComponent.getPanel();
        JCheckBox criticalPathCheckbox = findCheckBoxByText(panel, "Highlight Critical Path");
        assertThat(criticalPathCheckbox).isNotNull();
        assertThat(criticalPathCheckbox.isSelected()).isTrue();
    }

    @Test
    public void testGraphComponentHasOrgDependenciesOption() {
        JPanel panel = graphComponent.getPanel();
        JCheckBox orgDepCheckbox = findCheckBoxByText(panel, "Show Org Dependencies");
        assertThat(orgDepCheckbox).isNotNull();
        assertThat(orgDepCheckbox.isSelected()).isFalse();
    }

    @Test
    public void testLayoutSelectorHasOptions() {
        JPanel panel = graphComponent.getPanel();
        @SuppressWarnings("unchecked")
        JComboBox<String> layoutCombo = (JComboBox<String>) findComboBox(panel);
        assertThat(layoutCombo).isNotNull();
        assertThat(layoutCombo.getItemCount()).isGreaterThan(0);
    }

    private JPanel findGraphPanel(JPanel parent) {
        for (int i = 0; i < parent.getComponentCount(); i++) {
            java.awt.Component component = parent.getComponent(i);
            if (component instanceof JPanel) {
                JPanel childPanel = (JPanel) component;
                if (childPanel.getLayout() instanceof BorderLayout) {
                    return childPanel;
                }
                JPanel found = findGraphPanel(childPanel);
                if (found != null) return found;
            }
        }
        return null;
    }

    private JButton findButtonByText(JPanel parent, String text) {
        for (int i = 0; i < parent.getComponentCount(); i++) {
            java.awt.Component component = parent.getComponent(i);
            if (component instanceof JButton && text.equals(((JButton) component).getText())) {
                return (JButton) component;
            }
            if (component instanceof JPanel) {
                JButton found = findButtonByText((JPanel) component, text);
                if (found != null) return found;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private JComboBox<String> findComboBox(JPanel parent) {
        for (int i = 0; i < parent.getComponentCount(); i++) {
            java.awt.Component component = parent.getComponent(i);
            if (component instanceof JComboBox) {
                return (JComboBox<String>) component;
            }
            if (component instanceof JPanel) {
                JComboBox<String> found = findComboBox((JPanel) component);
                if (found != null) return found;
            }
        }
        return null;
    }

    private JCheckBox findCheckBoxByText(JPanel parent, String text) {
        for (int i = 0; i < parent.getComponentCount(); i++) {
            java.awt.Component component = parent.getComponent(i);
            if (component instanceof JCheckBox && text.equals(((JCheckBox) component).getText())) {
                return (JCheckBox) component;
            }
            if (component instanceof JPanel) {
                JCheckBox found = findCheckBoxByText((JPanel) component, text);
                if (found != null) return found;
            }
        }
        return null;
    }
}
