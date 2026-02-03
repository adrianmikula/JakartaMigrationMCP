package adrianmikula.jakartamigration.intellij.ui;

import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import javax.swing.*;
import javax.swing.table.TableModel;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UI tests for MigrationPhasesComponent based on TypeSpec: plugin-components.tsp
 */
public class MigrationPhasesComponentTest extends LightJavaCodeInsightFixtureTestCase {

    private MigrationPhasesComponent phasesComponent;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        phasesComponent = new MigrationPhasesComponent(getProject());
    }

    @Test
    public void testPhasesTableHasTypeSpecColumns() {
        JPanel panel = phasesComponent.getPanel();
        JTable table = findTable(panel);
        
        assertThat(table).isNotNull();
        
        // Verify TypeSpec-defined columns from MigrationPhase model
        TableModel model = table.getModel();
        assertThat(model.getColumnCount()).isEqualTo(7);
        assertThat(model.getColumnName(0)).isEqualTo("Phase");
        assertThat(model.getColumnName(1)).isEqualTo("Name");
        assertThat(model.getColumnName(2)).isEqualTo("Status");
        assertThat(model.getColumnName(3)).isEqualTo("Order");
        assertThat(model.getColumnName(4)).isEqualTo("Duration (hrs)");
        assertThat(model.getColumnName(5)).isEqualTo("Prerequisites");
        assertThat(model.getColumnName(6)).isEqualTo("Tasks");
    }

    @Test
    public void testAddPhaseWithTypeSpecData() {
        // Test adding phase with TypeSpec-compliant data
        phasesComponent.addPhase(
            "phase-1", "Dependency Analysis", "NOT_STARTED", 
            1, 4, "None", 3
        );
        
        JTable table = findTable(phasesComponent.getPanel());
        TableModel model = table.getModel();
        
        assertThat(model.getRowCount()).isEqualTo(1);
        assertThat(model.getValueAt(0, 0)).isEqualTo("phase-1");
        assertThat(model.getValueAt(0, 1)).isEqualTo("Dependency Analysis");
        assertThat(model.getValueAt(0, 2)).isEqualTo("NOT_STARTED");
        assertThat(model.getValueAt(0, 3)).isEqualTo(1);
        assertThat(model.getValueAt(0, 4)).isEqualTo("4");
        assertThat(model.getValueAt(0, 6)).isEqualTo("3 tasks");
    }

    @Test
    public void testPhaseActionsPresent() {
        JPanel panel = phasesComponent.getPanel();
        
        // Verify TypeSpec-defined PhaseAction buttons are present
        assertThat(findButtonByText(panel, "Start Phase")).isNotNull();
        assertThat(findButtonByText(panel, "Pause Phase")).isNotNull();
        assertThat(findButtonByText(panel, "Skip Phase")).isNotNull();
        assertThat(findButtonByText(panel, "View Details")).isNotNull();
    }

    @Test
    public void testProgressBarPresent() {
        JPanel panel = phasesComponent.getPanel();
        JProgressBar progressBar = findComponentByType(panel, JProgressBar.class);
        
        assertThat(progressBar).isNotNull();
        assertThat(progressBar.isStringPainted()).isTrue();
        assertThat(progressBar.getString()).isEqualTo("0% Complete");
    }

    private JTable findTable(JPanel panel) {
        return findComponentByType(panel, JTable.class);
    }

    private JButton findButtonByText(JPanel panel, String text) {
        return findComponentByTextAndType(panel, text, JButton.class);
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

    @SuppressWarnings("unchecked")
    private <T extends JComponent> T findComponentByTextAndType(java.awt.Container container, String text, Class<T> type) {
        for (int i = 0; i < container.getComponentCount(); i++) {
            java.awt.Component component = container.getComponent(i);
            if (type.isInstance(component)) {
                if (component instanceof JButton && text.equals(((JButton) component).getText())) {
                    return (T) component;
                }
            }
            if (component instanceof java.awt.Container) {
                T found = findComponentByTextAndType((java.awt.Container) component, text, type);
                if (found != null) return found;
            }
        }
        return null;
    }
}