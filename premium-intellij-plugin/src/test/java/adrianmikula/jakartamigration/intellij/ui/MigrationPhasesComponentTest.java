package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.model.DependencyInfo;
import adrianmikula.jakartamigration.intellij.model.DependencyMigrationStatus;
import adrianmikula.jakartamigration.intellij.ui.MigrationStrategyComponent.MigrationStrategy;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation")
public class MigrationPhasesComponentTest extends BasePlatformTestCase {

    private MigrationPhasesComponent phasesComponent;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        phasesComponent = new MigrationPhasesComponent(getProject());
    }

    public void testInitialization() {
        assertThat(phasesComponent.getPanel()).isNotNull();
        // Default is INCREMENTAL which has 4 phases
        assertThat(phasesComponent.getPhaseTabs().getTabCount()).isEqualTo(4);
        assertThat(phasesComponent.getSelectedStrategy()).isEqualTo(MigrationStrategy.INCREMENTAL);
    }

    public void testStrategySwitch() {
        phasesComponent.getStrategyComponent().setSelectedStrategy(MigrationStrategy.BIG_BANG);

        assertThat(phasesComponent.getSelectedStrategy()).isEqualTo(MigrationStrategy.BIG_BANG);
        // BIG_BANG has 1 phase
        assertThat(phasesComponent.getPhaseTabs().getTabCount()).isEqualTo(1);
        assertThat(phasesComponent.getPhaseTabs().getTitleAt(0)).isEqualTo("Complete Migration");
    }

    public void testSetDependencies() {
        List<DependencyInfo> deps = new ArrayList<>();
        deps.add(new DependencyInfo("org.hibernate", "hibernate-core", "5.6.0.Final", null,
                DependencyMigrationStatus.NEEDS_UPGRADE, false));

        phasesComponent.setDependencies(deps);

        // INCREMENTAL has phase-1 "Dependency Updates"
        SubtaskTableComponent phase1Table = phasesComponent.getPhaseSubtaskTables().get("phase-1");
        assertThat(phase1Table).isNotNull();

        // Check if dependency-specific subtask was added
        boolean found = false;
        for (int i = 0; i < phase1Table.getTableModel().getRowCount(); i++) {
            SubtaskTableComponent.SubtaskItem subtask = (SubtaskTableComponent.SubtaskItem) phase1Table.getTableModel()
                    .getValueAt(i, 0);
            String task = subtask.getName();
            if (task.contains("Migrate hibernate-core")) {
                found = true;
                break;
            }
        }
        assertThat(found).isTrue();
    }
}
