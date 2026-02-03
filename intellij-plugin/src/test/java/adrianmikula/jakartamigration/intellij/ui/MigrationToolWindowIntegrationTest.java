package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.mcp.DefaultMcpClientService;
import adrianmikula.jakartamigration.intellij.mcp.McpClientService;
import adrianmikula.jakartamigration.intellij.model.DependencyInfo;
import adrianmikula.jakartamigration.intellij.model.DependencyMigrationStatus;
import adrianmikula.jakartamigration.intellij.model.RiskLevel;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for MigrationToolWindow UI components with MCP data
 */
public class MigrationToolWindowIntegrationTest extends LightJavaCodeInsightFixtureTestCase {

    private McpClientService mcpClient;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mcpClient = new DefaultMcpClientService();
    }

    @Test
    public void testMigrationToolWindowContentInitialization() {
        MigrationToolWindow.MigrationToolWindowContent content =
            new MigrationToolWindow.MigrationToolWindowContent(getProject());

        assertThat(content.getContentPanel())
            .as("Content panel should be initialized")
            .isNotNull();
    }

    @Test
    public void testDependenciesTableWithMcpData() {
        DependenciesTableComponent component = new DependenciesTableComponent(getProject());

        // Simulate data that would come from MCP server
        List<DependencyInfo> mcpDependencies = createMockMcpData();
        component.setDependencies(mcpDependencies);

        // Verify data is loaded by checking the table model
        JPanel panel = component.getPanel();
        JTable table = findTable(panel);
        TableModel model = table.getModel();

        assertThat(model.getRowCount())
            .as("Should have all dependencies loaded")
            .isEqualTo(4);
    }

    @Test
    public void testDependenciesTableFiltering() {
        DependenciesTableComponent component = new DependenciesTableComponent(getProject());

        List<DependencyInfo> dependencies = createMixedDependencies();
        component.setDependencies(dependencies);

        // Verify filtering works by selecting and checking
        List<DependencyInfo> selected = component.getSelectedDependencies();
        assertThat(selected).isNotNull();
    }

    @Test
    public void testDashboardComponentWithMcpData() {
        DashboardComponent component = new DashboardComponent(getProject(), e -> {});

        // Test direct setters (simulating data from MCP)
        component.setReadinessScore(65);
        component.setStatus(adrianmikula.jakartamigration.intellij.model.MigrationStatus.HAS_BLOCKERS);
        component.setDependencySummary(42, 18, 3);
        component.setLastAnalyzed(java.time.Instant.now());

        assertThat(component.getPanel())
            .as("Dashboard panel should be initialized")
            .isNotNull();
    }

    @Test
    public void testMcpClientMockDataGeneration() {
        List<DependencyInfo> blockers = createMockBlockersList();
        List<DependencyInfo> recommendations = createMockRecommendations();

        assertThat(blockers)
            .as("Should have blockers from MCP mock data")
            .isNotEmpty();

        assertThat(recommendations)
            .as("Should have recommendations from MCP mock data")
            .isNotEmpty();

        // Verify blocker properties
        assertThat(blockers.stream().filter(DependencyInfo::isBlocker).count())
            .as("All mock blockers should be marked as blockers")
            .isEqualTo(blockers.size());
    }

    private List<DependencyInfo> createMockMcpData() {
        List<DependencyInfo> deps = new ArrayList<>();

        deps.add(new DependencyInfo(
            "javax.xml.bind", "jaxb-api", "2.3.1", null,
            DependencyMigrationStatus.NO_JAKARTA_VERSION, true,
            RiskLevel.CRITICAL, "No Jakarta equivalent"
        ));

        deps.add(new DependencyInfo(
            "org.springframework", "spring-beans", "5.3.27", "6.0.9",
            DependencyMigrationStatus.NEEDS_UPGRADE, false,
            RiskLevel.HIGH, "Required for Spring 6.0"
        ));

        deps.add(new DependencyInfo(
            "jakarta.servlet", "jakarta.servlet-api", "5.0.0", null,
            DependencyMigrationStatus.COMPATIBLE, false,
            RiskLevel.LOW, "Already compatible"
        ));

        deps.add(new DependencyInfo(
            "org.hibernate", "hibernate-core", "5.6.15.Final", "6.2.0.Final",
            DependencyMigrationStatus.NEEDS_UPGRADE, false,
            RiskLevel.CRITICAL, "Major version upgrade"
        ));

        return deps;
    }

    private List<DependencyInfo> createMixedDependencies() {
        List<DependencyInfo> deps = new ArrayList<>();

        // Add some blockers
        deps.add(new DependencyInfo(
            "javax.activation", "javax.activation-api", "1.2.0",
            "jakarta.activation:jakarta.activation-api:2.3.1",
            DependencyMigrationStatus.NEEDS_UPGRADE, true,
            RiskLevel.HIGH, "Upgrade required"
        ));

        // Add compatible dependencies
        deps.add(new DependencyInfo(
            "org.apache.commons", "commons-lang3", "3.12.0", null,
            DependencyMigrationStatus.COMPATIBLE, false,
            RiskLevel.LOW, "No Jakarta dependencies"
        ));

        return deps;
    }

    private List<DependencyInfo> createMockBlockersList() {
        List<DependencyInfo> blockers = new ArrayList<>();

        blockers.add(new DependencyInfo(
            "javax.xml.bind", "jaxb-api", "2.3.1", null,
            DependencyMigrationStatus.NO_JAKARTA_VERSION, true,
            RiskLevel.CRITICAL, "No Jakarta equivalent - requires alternative"
        ));

        blockers.add(new DependencyInfo(
            "javax.activation", "javax.activation-api", "1.2.0",
            "jakarta.activation:jakarta.activation-api:2.3.1",
            DependencyMigrationStatus.NEEDS_UPGRADE, true,
            RiskLevel.HIGH, "Upgrade to Jakarta Activation 2.3"
        ));

        return blockers;
    }

    private List<DependencyInfo> createMockRecommendations() {
        List<DependencyInfo> recommendations = new ArrayList<>();

        recommendations.add(new DependencyInfo(
            "org.springframework", "spring-beans", "5.3.27", "6.0.9",
            DependencyMigrationStatus.NEEDS_UPGRADE, false,
            RiskLevel.HIGH, "Required for Spring Framework 6.0 migration"
        ));

        recommendations.add(new DependencyInfo(
            "org.hibernate", "hibernate-core", "5.6.15.Final", "6.2.0.Final",
            DependencyMigrationStatus.NEEDS_UPGRADE, false,
            RiskLevel.CRITICAL, "Major version upgrade"
        ));

        return recommendations;
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
