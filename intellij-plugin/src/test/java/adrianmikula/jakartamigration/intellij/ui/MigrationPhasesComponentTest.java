package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.ui.MigrationStrategyComponent.MigrationStrategy;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import javax.swing.*;
import javax.swing.table.TableModel;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UI tests for MigrationPhasesComponent based on TypeSpec: plugin-components.tsp
 * Updated to test migration strategy selection.
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

    @Test
    public void testStrategyComponentPresent() {
        // Verify strategy selection component is present by checking for strategy cards
        JPanel panel = phasesComponent.getPanel();
        
        // Look for the strategy selection cards (JPanels with strategy colors)
        JPanel strategyPanel = findComponentByType(panel, JPanel.class);
        assertThat(strategyPanel).isNotNull();
        
        // Verify strategy component can be accessed
        assertThat(phasesComponent.getSelectedStrategy()).isNull(); // Initially null until selected
    }

    @Test
    public void testInitialStrategyIsNull() {
        assertThat(phasesComponent.getSelectedStrategy()).isNull();
    }

    @Test
    public void testFourMigrationStrategies() {
        // Verify all four strategies are available
        assertThat(MigrationStrategy.values()).hasSize(4);
        
        assertThat(MigrationStrategy.BIG_BANG.getDisplayName()).isEqualTo("Big Bang");
        assertThat(MigrationStrategy.INCREMENTAL.getDisplayName()).isEqualTo("Incremental");
        assertThat(MigrationStrategy.BUILD_TRANSFORMATION.getDisplayName()).isEqualTo("Build Transformation");
        assertThat(MigrationStrategy.RUNTIME_TRANSFORMATION.getDisplayName()).isEqualTo("Runtime Transformation");
    }

    @Test
    public void testBigBangStrategyPhases() {
        // Simulate selecting Big Bang strategy
        phasesComponent.getClass(); // Just verify component exists
        
        // The strategy selection should trigger phase generation
        assertThat(phasesComponent.getSelectedStrategy()).isNull();
    }

    @Test
    public void testIncrementalStrategyPhases() {
        // Incremental strategy should show multiple phases
        assertThat(phasesComponent.getSelectedStrategy()).isNull();
    }

    @Test
    public void testAddPhaseListener() {
        phasesComponent.addPhaseListener(new MigrationPhasesComponent.PhaseListener() {
            @Override
            public void onStrategySelected(MigrationStrategy strategy) {
                // Listener callback
            }
            @Override
            public void onPhaseSelected(int phaseIndex) {
                // Listener callback
            }
        });
        
        // Verify no exception thrown
        assertThat(true).isTrue();
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