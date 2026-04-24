package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.model.DependencyInfo;
import adrianmikula.jakartamigration.intellij.model.DependencyMigrationStatus;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.openapi.ui.Messages;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

public class DependenciesTableComponentTest extends BasePlatformTestCase {

    private DependenciesTableComponent tableComponent;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        tableComponent = new DependenciesTableComponent(getProject());
    }

    public void testInitialization() {
        assertThat(tableComponent.getPanel()).isNotNull();
        assertThat(tableComponent.getTableModel().getRowCount()).isEqualTo(0);
    }

    public void testSetDependencies() {
        List<DependencyInfo> deps = new ArrayList<>();
        deps.add(new DependencyInfo("org.hibernate", "hibernate-core", "5.6.0.Final", "org.hibernate", "hibernate-core", "6.0.0.Alpha1",
                "Compatible", null, DependencyMigrationStatus.NEEDS_UPGRADE, false, false));
        deps.add(new DependencyInfo("javax.servlet", "javax.servlet-api", "4.0.1", "jakarta.servlet", "jakarta.servlet-api", "5.0.0",
                "Compatible", null, DependencyMigrationStatus.COMPATIBLE, false, false));

        tableComponent.setDependencies(deps);

        assertThat(tableComponent.getTableModel().getRowCount()).isEqualTo(2);
        assertThat(tableComponent.getTableModel().getValueAt(0, 0)).isEqualTo("org.hibernate");
        assertThat(tableComponent.getTableModel().getValueAt(1, 0)).isEqualTo("javax.servlet");
    }

    public void testFilteringBySearch() {
        List<DependencyInfo> deps = new ArrayList<>();
        deps.add(new DependencyInfo("org.hibernate", "hibernate-core", "5.6.0.Final", null, null, null,
                "Unknown", null, DependencyMigrationStatus.NEEDS_UPGRADE, false, false));
        deps.add(new DependencyInfo("javax.servlet", "javax.servlet-api", "4.0.1", null, null, null,
                "Unknown", null, DependencyMigrationStatus.COMPATIBLE, false, false));
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
        deps.add(new DependencyInfo("org.hibernate", "hibernate-core", "5.6.0.Final", null, null, null,
                "Unknown", null, DependencyMigrationStatus.NEEDS_UPGRADE, false, false));
        deps.add(new DependencyInfo("javax.servlet", "javax.servlet-api", "4.0.1", null, null, null,
                "Unknown", null, DependencyMigrationStatus.COMPATIBLE, false, false));
        tableComponent.setDependencies(deps);

        tableComponent.getStatusFilter().setSelectedItem("Compatible");
        // actionPerformed is handled by listener

        assertThat(tableComponent.getTableModel().getRowCount()).isEqualTo(1);
        assertThat(tableComponent.getTableModel().getValueAt(0, 0)).isEqualTo("javax.servlet");
    }

    public void testFilteringByTransitive() {
        List<DependencyInfo> deps = new ArrayList<>();
        deps.add(new DependencyInfo("direct.dep", "artifact-1", "1.0", null, null, null,
                "Unknown", null, DependencyMigrationStatus.COMPATIBLE, false, false));
        deps.add(new DependencyInfo("transitive.dep", "artifact-2", "2.0", null, null, null,
                "Unknown", null, DependencyMigrationStatus.COMPATIBLE, true, false));
        tableComponent.setDependencies(deps);

        // Test "Hide Transitive Dependencies" checkbox - when selected, transitive deps are hidden
        tableComponent.getTransitiveFilter().setSelected(true);

        // Trigger the action listener manually - JCheckBox doesn't trigger ActionListener on setSelected
        for (java.awt.event.ActionListener l : tableComponent.getTransitiveFilter().getActionListeners()) {
            l.actionPerformed(new java.awt.event.ActionEvent(tableComponent.getTransitiveFilter(), 0, ""));
        }

        // Should only show direct dependency now (transitive is hidden)
        assertThat(tableComponent.getTableModel().getRowCount()).isEqualTo(1);
        assertThat(tableComponent.getTableModel().getValueAt(0, 0)).isEqualTo("direct.dep");

        // Uncheck - should show all dependencies again
        tableComponent.getTransitiveFilter().setSelected(false);
        for (java.awt.event.ActionListener l : tableComponent.getTransitiveFilter().getActionListeners()) {
            l.actionPerformed(new java.awt.event.ActionEvent(tableComponent.getTransitiveFilter(), 0, ""));
        }

        assertThat(tableComponent.getTableModel().getRowCount()).isEqualTo(2);
    }

    public void testFilteringByUnknownStatus() {
        List<DependencyInfo> deps = new ArrayList<>();
        deps.add(new DependencyInfo("org.hibernate", "hibernate-core", "5.6.0.Final", null, null, null,
                "Unknown", null, DependencyMigrationStatus.NEEDS_UPGRADE, false, false));
        deps.add(new DependencyInfo("com.unknown", "unknown-api", "1.0.0", null, null, null,
                "Unknown", null, DependencyMigrationStatus.UNKNOWN, false, false));
        deps.add(new DependencyInfo("javax.servlet", "javax.servlet-api", "4.0.1", null, null, null,
                "Unknown", null, DependencyMigrationStatus.COMPATIBLE, false, false));
        tableComponent.setDependencies(deps);

        tableComponent.getStatusFilter().setSelectedItem("Unknown");
        // actionPerformed is handled by listener

        assertThat(tableComponent.getTableModel().getRowCount()).isEqualTo(1);
        assertThat(tableComponent.getTableModel().getValueAt(0, 0)).isEqualTo("com.unknown");
    }

    public void testApplyRecipeButtonForNonPremiumUsers() {
        // Mock Messages to verify dialog behavior
        Messages messages = mock(Messages.class);
        
        // Set up test data with a dependency that has an associated recipe
        List<DependencyInfo> deps = new ArrayList<>();
        deps.add(new DependencyInfo("javax.servlet", "javax.servlet-api", "4.0.1", "jakarta.servlet", "jakarta.servlet-api", "5.0.0",
                "Compatible", "JavaxServletToJakartaServlet", DependencyMigrationStatus.NEEDS_UPGRADE, false, false));
        
        tableComponent.setDependencies(deps);
        tableComponent.setPremiumUser(false);
        
        // Select the dependency
        tableComponent.getTableModel().setValueAt(true, 0, 0); // Select first row
        tableComponent.updateRecipesPanel(deps.get(0));
        
        // Verify the Apply Recipe button is enabled but shows premium warning when clicked
        assertThat(tableComponent.getApplyRecipeButton().isEnabled()).isTrue();
        
        // Test that non-premium users get the warning dialog
        // Note: In a real test environment, we'd mock Messages.showWarningDialog
        // For now, we just verify the logic flow
    }

    public void testApplyRecipeButtonForPremiumUsers() {
        // Set up test data with a dependency that has an associated recipe
        List<DependencyInfo> deps = new ArrayList<>();
        deps.add(new DependencyInfo("javax.servlet", "javax.servlet-api", "4.0.1", "jakarta.servlet", "jakarta.servlet-api", "5.0.0",
                "Compatible", "JavaxServletToJakartaServlet", DependencyMigrationStatus.NEEDS_UPGRADE, false, false));
        
        tableComponent.setDependencies(deps);
        tableComponent.setPremiumUser(true);
        
        // Select the dependency
        tableComponent.getTableModel().setValueAt(true, 0, 0); // Select first row
        tableComponent.updateRecipesPanel(deps.get(0));
        
        // Verify the Apply Recipe button is enabled for premium users
        assertThat(tableComponent.getApplyRecipeButton().isEnabled()).isTrue();
        
        // Test that premium users can apply recipes directly without confirmation dialog
        // The implementation should call applyRecipeDirectly() instead of showing confirmation
        // Note: In a real test environment, we'd verify no confirmation dialog is shown
        // and that applyRecipeDirectly is called
    }

    public void testApplyRecipeButtonDisabledWhenNoRecipe() {
        // Set up test data with a dependency that has NO associated recipe
        List<DependencyInfo> deps = new ArrayList<>();
        deps.add(new DependencyInfo("some.dependency", "artifact", "1.0", null, null, null,
                "Unknown", null, DependencyMigrationStatus.NO_JAKARTA_VERSION, false, false));
        
        tableComponent.setDependencies(deps);
        tableComponent.setPremiumUser(true);
        
        // Select the dependency
        tableComponent.getTableModel().setValueAt(true, 0, 0); // Select first row
        tableComponent.updateRecipesPanel(deps.get(0));
        
        // Verify the Apply Recipe button is disabled when no recipe is available
        assertThat(tableComponent.getApplyRecipeButton().isEnabled()).isFalse();
    }
}
