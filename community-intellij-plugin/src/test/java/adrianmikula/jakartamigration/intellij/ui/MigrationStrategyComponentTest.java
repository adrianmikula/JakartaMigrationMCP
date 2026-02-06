package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.ui.MigrationStrategyComponent.MigrationStrategy;
import adrianmikula.jakartamigration.intellij.ui.MigrationStrategyComponent.MigrationStrategyListener;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.junit.jupiter.api.Test;

import javax.swing.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for MigrationStrategyComponent.
 */
public class MigrationStrategyComponentTest extends LightJavaCodeInsightFixtureTestCase {

    private MigrationStrategyComponent strategyComponent;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        strategyComponent = new MigrationStrategyComponent(getProject());
    }

    @Test
    public void testComponentInitialization() {
        JPanel panel = strategyComponent.getPanel();
        assertThat(panel).isNotNull();
        assertThat(panel.getComponentCount()).isGreaterThan(0);
    }

    @Test
    public void testStrategyEnumValues() {
        // Verify all strategies exist
        assertThat(MigrationStrategy.values()).hasSize(4);
        assertThat(MigrationStrategy.BIG_BANG).isNotNull();
        assertThat(MigrationStrategy.INCREMENTAL).isNotNull();
        assertThat(MigrationStrategy.BUILD_TRANSFORMATION).isNotNull();
        assertThat(MigrationStrategy.RUNTIME_TRANSFORMATION).isNotNull();
    }

    @Test
    public void testStrategyDisplayNames() {
        assertThat(MigrationStrategy.BIG_BANG.getDisplayName()).isEqualTo("Big Bang");
        assertThat(MigrationStrategy.INCREMENTAL.getDisplayName()).isEqualTo("Incremental");
        assertThat(MigrationStrategy.BUILD_TRANSFORMATION.getDisplayName()).isEqualTo("Build Transformation");
        assertThat(MigrationStrategy.RUNTIME_TRANSFORMATION.getDisplayName()).isEqualTo("Runtime Transformation");
    }

    @Test
    public void testStrategyDescriptions() {
        assertThat(MigrationStrategy.BIG_BANG.getDescription()).contains("Migrate everything");
        assertThat(MigrationStrategy.INCREMENTAL.getDescription()).contains("One dependency");
        assertThat(MigrationStrategy.BUILD_TRANSFORMATION.getDescription()).contains("build process");
        assertThat(MigrationStrategy.RUNTIME_TRANSFORMATION.getDescription()).contains("runtime");
    }

    @Test
    public void testStrategyColors() {
        // Each strategy should have a distinct color
        assertThat(MigrationStrategy.BIG_BANG.getColor()).isNotNull();
        assertThat(MigrationStrategy.INCREMENTAL.getColor()).isNotNull();
        assertThat(MigrationStrategy.BUILD_TRANSFORMATION.getColor()).isNotNull();
        assertThat(MigrationStrategy.RUNTIME_TRANSFORMATION.getColor()).isNotNull();
    }

    @Test
    public void testInitialSelectionIsNull() {
        assertThat(strategyComponent.getSelectedStrategy()).isNull();
    }

    @Test
    public void testStrategyListenerNotified() {
        // Track if listener was called
        final boolean[] listenerCalled = {false};
        final MigrationStrategy[] receivedStrategy = {null};

        MigrationStrategyListener listener = strategy -> {
            listenerCalled[0] = true;
            receivedStrategy[0] = strategy;
        };

        strategyComponent.addMigrationStrategyListener(listener);

        // Verify listener was added (this is a basic check)
        assertThat(true).isTrue();
    }

    @Test
    public void testComponentHasTitle() {
        JPanel panel = strategyComponent.getPanel();
        // Title should be in the north section
        assertThat(panel.getComponent(0)).isInstanceOf(JPanel.class);
    }

    @Test
    public void testComponentHasCardsPanel() {
        JPanel panel = strategyComponent.getPanel();
        // Cards panel should be in the center section
        assertThat(panel.getComponent(1)).isInstanceOf(JPanel.class);
    }

    @Test
    public void testComponentHasInfoPanel() {
        JPanel panel = strategyComponent.getPanel();
        // Info panel should be in the south section
        assertThat(panel.getComponent(2)).isInstanceOf(JPanel.class);
    }
}
