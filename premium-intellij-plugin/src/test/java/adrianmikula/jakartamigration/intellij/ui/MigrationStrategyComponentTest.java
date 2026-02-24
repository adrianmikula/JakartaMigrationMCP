package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.ui.MigrationStrategyComponent.MigrationStrategy;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

public class MigrationStrategyComponentTest extends BasePlatformTestCase {

    private MigrationStrategyComponent strategyComponent;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        strategyComponent = new MigrationStrategyComponent(getProject());
    }

    public void testInitialization() {
        assertThat(strategyComponent.getPanel()).isNotNull();
        // Initial selected strategy is null by default
        assertThat(strategyComponent.getSelectedStrategy()).isNull();
    }

    public void testStrategySelection() {
        AtomicReference<MigrationStrategy> selectedStrategy = new AtomicReference<>();
        strategyComponent.addMigrationStrategyListener(selectedStrategy::set);

        strategyComponent.setSelectedStrategy(MigrationStrategy.INCREMENTAL);

        assertThat(strategyComponent.getSelectedStrategy()).isEqualTo(MigrationStrategy.INCREMENTAL);
        assertThat(selectedStrategy.get()).isEqualTo(MigrationStrategy.INCREMENTAL);
    }

    public void testStrategyDetailsUpdate() {
        strategyComponent.setSelectedStrategy(MigrationStrategy.BIG_BANG);

        assertThat(strategyComponent.getBenefitsText().getText()).contains("Migrate all javax dependencies");
        assertThat(strategyComponent.getRisksText().getText()).contains("Higher risk");

        strategyComponent.setSelectedStrategy(MigrationStrategy.RUNTIME_TRANSFORMATION);

        assertThat(strategyComponent.getBenefitsText().getText()).contains("No code changes required");
        assertThat(strategyComponent.getRisksText().getText()).contains("Performance overhead");
    }
}
