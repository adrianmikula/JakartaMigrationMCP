package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.model.DependencyInfo;
import adrianmikula.jakartamigration.intellij.model.DependencyMigrationStatus;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

public class DependenciesTableComponentTest extends BasePlatformTestCase {

    private DependenciesTableComponent tableComponent;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        tableComponent = new DependenciesTableComponent(getProject());
    }

    public void testInitialization() {
        assertThat(tableComponent.getPanel()).isNotNull();
        assertThat(tableComponent.getTableModel().getRowCount()).isEqualTo(0);
    }

    public void testSetDependencies() {
        List<DependencyInfo> deps = new ArrayList<>();
        deps.add(new DependencyInfo("org.hibernate", "hibernate-core", "5.6.0.Final", "6.0.0.Alpha1",
                DependencyMigrationStatus.NEEDS_UPGRADE, false));
        deps.add(new DependencyInfo("javax.servlet", "javax.servlet-api", "4.0.1", "5.0.0",
                DependencyMigrationStatus.COMPATIBLE, false));

        tableComponent.setDependencies(deps);

        assertThat(tableComponent.getTableModel().getRowCount()).isEqualTo(2);
        assertThat(tableComponent.getTableModel().getValueAt(0, 0)).isEqualTo("org.hibernate");
        assertThat(tableComponent.getTableModel().getValueAt(1, 0)).isEqualTo("javax.servlet");
    }

    public void testFilteringBySearch() {
        List<DependencyInfo> deps = new ArrayList<>();
        deps.add(new DependencyInfo("org.hibernate", "hibernate-core", "5.6.0.Final", null,
                DependencyMigrationStatus.NEEDS_UPGRADE, false));
        deps.add(new DependencyInfo("javax.servlet", "javax.servlet-api", "4.0.1", null,
                DependencyMigrationStatus.COMPATIBLE, false));
        tableComponent.setDependencies(deps);

        tableComponent.getSearchField().setText("servlet");
        // filterDependencies is called by DocumentListener, but in tests we might need
        // to trigger or simulate event
        // In this implementation, it's triggered on
        // insertUpdate/removeUpdate/changedUpdate

        assertThat(tableComponent.getTableModel().getRowCount()).isEqualTo(1);
        assertThat(tableComponent.getTableModel().getValueAt(0, 0)).isEqualTo("javax.servlet");
    }

    public void testFilteringByStatus() {
        List<DependencyInfo> deps = new ArrayList<>();
        deps.add(new DependencyInfo("org.hibernate", "hibernate-core", "5.6.0.Final", null,
                DependencyMigrationStatus.NEEDS_UPGRADE, false));
        deps.add(new DependencyInfo("javax.servlet", "javax.servlet-api", "4.0.1", null,
                DependencyMigrationStatus.COMPATIBLE, false));
        tableComponent.setDependencies(deps);

        tableComponent.getStatusFilter().setSelectedItem("Compatible");
        // actionPerformed is handled by listener

        assertThat(tableComponent.getTableModel().getRowCount()).isEqualTo(1);
        assertThat(tableComponent.getTableModel().getValueAt(0, 0)).isEqualTo("javax.servlet");
    }

    public void testFilteringByTransitive() {
        List<DependencyInfo> deps = new ArrayList<>();
        deps.add(new DependencyInfo("direct.dep", "artifact-1", "1.0", null, DependencyMigrationStatus.COMPATIBLE,
                false));
        deps.add(new DependencyInfo("transitive.dep", "artifact-2", "2.0", null, DependencyMigrationStatus.COMPATIBLE,
                true));
        tableComponent.setDependencies(deps);

        tableComponent.getTransitiveFilter().setSelected(true);
        // trigger the action listener manually if needed, or check ifsetSelected
        // triggers it
        // Actually JCheckBox doesn't trigger ActionListener on setSelected(boolean),
        // need to call logic or simulate event

        // Let's re-run filtering manually if setSelected doesn't trigger it in unit
        // tests
        for (java.awt.event.ActionListener l : tableComponent.getTransitiveFilter().getActionListeners()) {
            l.actionPerformed(new java.awt.event.ActionEvent(tableComponent.getTransitiveFilter(), 0, ""));
        }

        assertThat(tableComponent.getTableModel().getRowCount()).isEqualTo(1);
        assertThat(tableComponent.getTableModel().getValueAt(0, 0)).isEqualTo("transitive.dep");
    }
}
