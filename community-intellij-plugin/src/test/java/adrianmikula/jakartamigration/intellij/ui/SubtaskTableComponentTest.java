package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.model.DependencyInfo;
import adrianmikula.jakartamigration.intellij.model.DependencyMigrationStatus;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UI tests for SubtaskTableComponent with dependency update actions.
 * Tests the complete flow of creating subtasks with dependencies and verifying upgrade actions work.
 */
public class SubtaskTableComponentTest extends LightJavaCodeInsightFixtureTestCase {

    private SubtaskTableComponent subtaskTableComponent;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        subtaskTableComponent = new SubtaskTableComponent(getProject());
    }

    @Test
    public void testSubtaskTableComponentHasPanel() {
        JPanel panel = subtaskTableComponent.getPanel();
        assertThat(panel).isNotNull();
    }

    @Test
    public void testSubtaskTableHasCorrectColumns() {
        JTable table = findSubtaskTable(subtaskTableComponent.getPanel());
        assertThat(table).isNotNull();
        assertThat(table.getColumnCount()).isEqualTo(4);
        assertThat(table.getColumnName(0)).isEqualTo("Status");
        assertThat(table.getColumnName(1)).isEqualTo("Subtask");
        assertThat(table.getColumnName(2)).isEqualTo("Dependency");
        assertThat(table.getColumnName(3)).isEqualTo("Action");
    }

    @Test
    public void testCreateSubtasksWithDependencyUpdateAction() {
        // Create mock dependencies with recommendedVersion populated
        List<DependencyInfo> dependencies = new ArrayList<>();

        DependencyInfo dep1 = new DependencyInfo();
        dep1.setGroupId("javax.activation");
        dep1.setArtifactId("javax.activation-api");
        dep1.setCurrentVersion("1.2.0");
        dep1.setRecommendedVersion("2.0.1"); // Jakarta EE version
        dep1.setMigrationStatus(DependencyMigrationStatus.NEEDS_UPGRADE);
        dep1.setTransitive(false);
        dependencies.add(dep1);

        DependencyInfo dep2 = new DependencyInfo();
        dep2.setGroupId("org.eclipse.persistence");
        dep2.setArtifactId("eclipselink");
        dep2.setCurrentVersion("2.7.10");
        dep2.setRecommendedVersion("3.0.0"); // Jakarta EE version
        dep2.setMigrationStatus(DependencyMigrationStatus.NEEDS_UPGRADE);
        dep2.setTransitive(false);
        dependencies.add(dep2);

        // Create subtasks from dependencies
        List<SubtaskTableComponent.SubtaskItem> subtasks = SubtaskTableComponent.createSubtasks(
            new String[]{"Analyze imports", "Refactor code"},
            dependencies,
            DependencyMigrationStatus.NEEDS_UPGRADE
        );

        // Should have 2 phase subtasks + 2 dependency subtasks = 4 subtasks
        assertThat(subtasks).hasSize(4);

        // First two are phase subtasks (no automation for import refactoring)
        assertThat(subtasks.get(0).getName()).isEqualTo("Analyze imports");
        assertThat(subtasks.get(0).getDependency()).isNull();

        assertThat(subtasks.get(1).getName()).isEqualTo("Refactor code");
        assertThat(subtasks.get(1).getDependency()).isNull();

        // Last two are dependency upgrade subtasks
        assertThat(subtasks.get(2).getName()).isEqualTo("Migrate javax.activation:javax.activation-api");
        assertThat(subtasks.get(2).getDependency()).isNotNull();
        assertThat(subtasks.get(2).getDependency().getGroupId()).isEqualTo("javax.activation");
        assertThat(subtasks.get(2).hasAutomation()).isTrue();

        assertThat(subtasks.get(3).getName()).isEqualTo("Migrate org.eclipse.persistence:eclipselink");
        assertThat(subtasks.get(3).getDependency()).isNotNull();
        assertThat(subtasks.get(3).getDependency().getGroupId()).isEqualTo("org.eclipse.persistence");
        assertThat(subtasks.get(3).hasAutomation()).isTrue();
    }

    @Test
    public void testDependencyUpdateSubtaskHasCorrectAutomationType() {
        // Create a single dependency with recommendedVersion
        List<DependencyInfo> dependencies = new ArrayList<>();

        DependencyInfo dep = new DependencyInfo();
        dep.setGroupId("jakarta.xml.bind");
        dep.setArtifactId("jaxb-api");
        dep.setCurrentVersion("2.3.1");
        dep.setRecommendedVersion("3.0.0"); // Jakarta EE version
        dep.setMigrationStatus(DependencyMigrationStatus.NEEDS_UPGRADE);
        dep.setTransitive(false);
        dependencies.add(dep);

        List<SubtaskTableComponent.SubtaskItem> subtasks = SubtaskTableComponent.createSubtasks(
            new String[]{},
            dependencies,
            DependencyMigrationStatus.NEEDS_UPGRADE
        );

        assertThat(subtasks).hasSize(1);
        SubtaskTableComponent.SubtaskItem subtask = subtasks.get(0);
        assertThat(subtask.getName()).isEqualTo("Migrate jakarta.xml.bind:jaxb-api");
        assertThat(subtask.hasAutomation()).isTrue();
        assertThat(subtask.getDependency()).isNotNull();
        assertThat(subtask.getDependency().getRecommendedVersion()).isEqualTo("3.0.0");
    }

    @Test
    public void testSubtaskItemDependencyName() {
        DependencyInfo dep = new DependencyInfo();
        dep.setGroupId("javax.servlet");
        dep.setArtifactId("javax.servlet-api");
        dep.setCurrentVersion("4.0.1");
        dep.setMigrationStatus(DependencyMigrationStatus.NEEDS_UPGRADE);

        SubtaskTableComponent.SubtaskItem subtask = new SubtaskTableComponent.SubtaskItem(
            "Test subtask", "", dep, "dependency-update"
        );

        assertThat(subtask.getDependencyName()).isEqualTo("javax.servlet-api");
        assertThat(subtask.hasAutomation()).isTrue();
    }

    @Test
    public void testSubtaskWithoutDependency() {
        SubtaskTableComponent.SubtaskItem subtask = new SubtaskTableComponent.SubtaskItem(
            "Manual task", "Description", null, null
        );

        assertThat(subtask.getDependency()).isNull();
        assertThat(subtask.getDependencyName()).isEmpty();
        assertThat(subtask.hasAutomation()).isFalse();
    }

    @Test
    public void testSetSubtasksUpdatesTable() {
        List<SubtaskTableComponent.SubtaskItem> subtasks = new ArrayList<>();

        DependencyInfo dep = new DependencyInfo();
        dep.setGroupId("test");
        dep.setArtifactId("artifact");
        dep.setCurrentVersion("1.0.0");
        dep.setRecommendedVersion("2.0.0");
        dep.setMigrationStatus(DependencyMigrationStatus.NEEDS_UPGRADE);

        subtasks.add(new SubtaskTableComponent.SubtaskItem(
            "Task 1", "", dep, "dependency-update"
        ));
        subtasks.add(new SubtaskTableComponent.SubtaskItem(
            "Task 2", "", null, null
        ));

        subtaskTableComponent.setSubtasks(subtasks);

        JTable table = findSubtaskTable(subtaskTableComponent.getPanel());
        assertThat(table).isNotNull();
        assertThat(table.getRowCount()).isEqualTo(2);
    }

    @Test
    public void testEmptySubtasksList() {
        subtaskTableComponent.setSubtasks(new ArrayList<>());

        JTable table = findSubtaskTable(subtaskTableComponent.getPanel());
        assertThat(table).isNotNull();
        assertThat(table.getRowCount()).isEqualTo(0);
    }

    @Test
    public void testNullSubtasksList() {
        subtaskTableComponent.setSubtasks(null);

        JTable table = findSubtaskTable(subtaskTableComponent.getPanel());
        assertThat(table).isNotNull();
        assertThat(table.getRowCount()).isEqualTo(0);
    }

    @Test
    public void testPhaseSubtasksDoNotHaveDependencyAutomation() {
        // Phase subtasks should not have dependency automation type
        List<SubtaskTableComponent.SubtaskItem> subtasks = SubtaskTableComponent.createSubtasks(
            new String[]{"Phase 1: Discovery", "Phase 2: Assessment"},
            new ArrayList<>(), // No dependencies
            DependencyMigrationStatus.NEEDS_UPGRADE
        );

        assertThat(subtasks).hasSize(2);
        assertThat(subtasks.get(0).getName()).isEqualTo("Phase 1: Discovery");
        assertThat(subtasks.get(0).hasAutomation()).isFalse();

        assertThat(subtasks.get(1).getName()).isEqualTo("Phase 2: Assessment");
        assertThat(subtasks.get(1).hasAutomation()).isFalse();
    }

    @Test
    public void testSubtasksLimitedToTenDependencies() {
        // Create more than 10 dependencies
        List<DependencyInfo> dependencies = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            DependencyInfo dep = new DependencyInfo();
            dep.setGroupId("group" + i);
            dep.setArtifactId("artifact" + i);
            dep.setCurrentVersion("1.0." + i);
            dep.setRecommendedVersion("2.0." + i);
            dep.setMigrationStatus(DependencyMigrationStatus.NEEDS_UPGRADE);
            dependencies.add(dep);
        }

        List<SubtaskTableComponent.SubtaskItem> subtasks = SubtaskTableComponent.createSubtasks(
            new String[]{},
            dependencies,
            DependencyMigrationStatus.NEEDS_UPGRADE
        );

        // Should be limited to 10 dependency subtasks
        assertThat(subtasks).hasSize(10);
    }

    private JTable findSubtaskTable(JPanel panel) {
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
