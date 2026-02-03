package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.model.MigrationDashboard;
import adrianmikula.jakartamigration.intellij.model.MigrationStatus;
import adrianmikula.jakartamigration.intellij.model.DependencySummary;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import javax.swing.*;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UI tests for MigrationToolWindow based on TypeSpec: plugin-components.tsp
 */
public class MigrationToolWindowTest extends BaseUITest {

    private MigrationToolWindow.MigrationToolWindowContent toolWindowContent;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUpTest();
        toolWindowContent = new MigrationToolWindow.MigrationToolWindowContent(getProject());
    }

    @Test
    public void testToolWindowHasRequiredTabs() {
        JPanel contentPanel = toolWindowContent.getContentPanel();
        assertThat(contentPanel).isNotNull();
        
        // Find JTabbedPane component
        JTabbedPane tabbedPane = findTabbedPane(contentPanel);
        assertThat(tabbedPane).isNotNull();
        
        // Verify all TypeSpec-defined tabs are present
        assertThat(tabbedPane.getTabCount()).isEqualTo(4);
        assertThat(tabbedPane.getTitleAt(0)).isEqualTo("Dashboard");
        assertThat(tabbedPane.getTitleAt(1)).isEqualTo("Dependencies");
        assertThat(tabbedPane.getTitleAt(2)).isEqualTo("Dependency Graph");
        assertThat(tabbedPane.getTitleAt(3)).isEqualTo("Migration Phases");
    }

    private JTabbedPane findTabbedPane(JPanel panel) {
        for (int i = 0; i < panel.getComponentCount(); i++) {
            if (panel.getComponent(i) instanceof JTabbedPane) {
                return (JTabbedPane) panel.getComponent(i);
            }
        }
        return null;
    }
}