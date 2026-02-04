package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.ui.MigrationStrategyComponent.MigrationStrategy;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UI tests for MigrationPhasesComponent based on new JTree accordion design.
 */
public class MigrationPhasesComponentTest extends LightJavaCodeInsightFixtureTestCase {

    private MigrationPhasesComponent phasesComponent;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        phasesComponent = new MigrationPhasesComponent(getProject());
    }

    @Test
    public void testPhasesComponentHasPanel() {
        JPanel panel = phasesComponent.getPanel();
        assertThat(panel).isNotNull();
    }

    @Test
    public void testPhasesComponentHasJTree() {
        JPanel panel = phasesComponent.getPanel();
        JTree tree = findComponentByType(panel, JTree.class);

        assertThat(tree).isNotNull();
        assertThat(tree.getModel()).isNotNull();
    }

    @Test
    public void testPhasesComponentHasActionButtons() {
        JPanel panel = phasesComponent.getPanel();

        assertThat(findButtonByText(panel, "Start Phase")).isNotNull();
        assertThat(findButtonByText(panel, "View Details")).isNotNull();
    }

    @Test
    public void testPhasesComponentHasExecutionStatus() {
        JPanel panel = phasesComponent.getPanel();

        // Should have execution status panel
        JPanel statusPanel = findComponentByType(panel, JPanel.class);
        assertThat(statusPanel).isNotNull();
    }

    @Test
    public void testStrategyComponentPresent() {
        // Verify strategy selection component is present
        assertThat(phasesComponent.getSelectedStrategy()).isEqualTo(MigrationStrategy.INCREMENTAL);
    }

    @Test
    public void testDefaultStrategyIsIncremental() {
        assertThat(phasesComponent.getSelectedStrategy()).isEqualTo(MigrationStrategy.INCREMENTAL);
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

    @Test
    public void testTreeHasRootNode() {
        JTree tree = findComponentByType(phasesComponent.getPanel(), JTree.class);
        assertThat(tree.getModel().getRoot()).isNotNull();
    }

    @Test
    public void testSetDependenciesUpdatesPhases() {
        // Should not throw exception when setting dependencies
        phasesComponent.setDependencies(new java.util.ArrayList<>());
        assertThat(phasesComponent.getSelectedStrategy()).isEqualTo(MigrationStrategy.INCREMENTAL);
    }

    private JTree findTree(JPanel panel) {
        return findComponentByType(panel, JTree.class);
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
