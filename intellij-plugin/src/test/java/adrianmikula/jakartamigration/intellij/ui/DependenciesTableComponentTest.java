package adrianmikula.jakartamigration.intellij.ui;

import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import javax.swing.*;
import javax.swing.table.TableModel;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UI tests for DependenciesTableComponent based on TypeSpec: plugin-components.tsp
 */
public class DependenciesTableComponentTest extends LightJavaCodeInsightFixtureTestCase {

    private DependenciesTableComponent tableComponent;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        tableComponent = new DependenciesTableComponent(getProject());
    }

    @Test
    public void testTableHasTypeSpecColumns() {
        JPanel panel = tableComponent.getPanel();
        JTable table = findTable(panel);
        
        assertThat(table).isNotNull();
        
        // Verify TypeSpec-defined columns from DependencyTableColumn enum
        TableModel model = table.getModel();
        assertThat(model.getColumnCount()).isEqualTo(8);
        assertThat(model.getColumnName(0)).isEqualTo("Group ID");
        assertThat(model.getColumnName(1)).isEqualTo("Artifact ID");
        assertThat(model.getColumnName(2)).isEqualTo("Current Version");
        assertThat(model.getColumnName(3)).isEqualTo("Recommended Version");
        assertThat(model.getColumnName(4)).isEqualTo("Migration Status");
        assertThat(model.getColumnName(5)).isEqualTo("Is Blocker");
        assertThat(model.getColumnName(6)).isEqualTo("Risk Level");
        assertThat(model.getColumnName(7)).isEqualTo("Migration Impact");
    }

    @Test
    public void testAddDependencyWithTypeSpecData() {
        // Test adding dependency with TypeSpec-compliant data
        tableComponent.addDependency(
            "com.example", "test-lib", "1.0.0", "2.0.0",
            "NEEDS_UPGRADE", true, "MEDIUM", "Breaking changes"
        );
        
        JTable table = findTable(tableComponent.getPanel());
        TableModel model = table.getModel();
        
        assertThat(model.getRowCount()).isEqualTo(1);
        assertThat(model.getValueAt(0, 0)).isEqualTo("com.example");
        assertThat(model.getValueAt(0, 1)).isEqualTo("test-lib");
        assertThat(model.getValueAt(0, 4)).isEqualTo("NEEDS_UPGRADE");
        assertThat(model.getValueAt(0, 5)).isEqualTo("Yes");
        assertThat(model.getValueAt(0, 6)).isEqualTo("MEDIUM");
    }

    @Test
    public void testTableFiltersPresent() {
        JPanel panel = tableComponent.getPanel();
        
        // Verify TypeSpec-defined filters are present
        assertThat(findComponentByType(panel, JTextField.class)).isNotNull(); // Search field
        assertThat(findComponentByType(panel, JComboBox.class)).isNotNull(); // Status filter
        assertThat(findComponentByType(panel, JCheckBox.class)).isNotNull(); // Blockers only
    }

    private JTable findTable(JPanel panel) {
        return findComponentByType(panel, JTable.class);
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