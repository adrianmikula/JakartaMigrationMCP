package adrianmikula.jakartamigration.intellij.ui.tree;

import adrianmikula.jakartamigration.intellij.model.DependencyInfo;
import adrianmikula.jakartamigration.intellij.model.DependencyMigrationStatus;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for DependencyTreeNode.
 */
public class DependencyTreeNodeTest {

    @Test
    public void testNodeCreation() {
        DependencyInfo dep = createDependency("org.example", "lib1", "1.0", 0, false);
        dep.setMigrationStatus(DependencyMigrationStatus.COMPATIBLE);

        DependencyTreeNode node = new DependencyTreeNode(dep);

        assertThat(node).isNotNull();
        assertThat(node.getDependency()).isEqualTo(dep);
        assertThat(node.getChildCount()).isEqualTo(0);
    }

    @Test
    public void testNodeWithChildren() {
        DependencyInfo parent = createDependency("org.example", "parent", "1.0", 0, false);
        DependencyInfo child1 = createDependency("org.example", "child1", "1.0", 1, true);
        DependencyInfo child2 = createDependency("org.example", "child2", "1.0", 1, true);

        parent.setChildren(List.of(child1, child2));

        DependencyTreeNode node = new DependencyTreeNode(parent);
        node.buildChildNodes();

        assertThat(node.getChildCount()).isEqualTo(2);
    }

    @Test
    public void testMergeDuplicatesLogic() {
        List<DependencyInfo> dependencies = new ArrayList<>();
        // First branch: parent1 with child
        DependencyInfo parent1 = createDependency("org.example", "parent1", "1.0", 0, false);
        DependencyInfo shared1 = createDependency("org.example", "shared", "1.0", 1, true);
        parent1.setChildren(List.of(shared1));
        
        // Second branch: parent2 with same child
        DependencyInfo parent2 = createDependency("org.example", "parent2", "1.0", 0, false);
        DependencyInfo shared2 = createDependency("org.example", "shared", "1.0", 1, true);
        parent2.setChildren(List.of(shared2));

        dependencies.add(parent1);
        dependencies.add(parent2);

        // Build tree with merge duplicates enabled
        adrianmikula.jakartamigration.intellij.model.DependencyTreeBuilder builder = 
            new adrianmikula.jakartamigration.intellij.model.DependencyTreeBuilder();
        
        List<DependencyInfo> flatList = new ArrayList<>();
        flatList.add(parent1);
        flatList.add(shared1);
        flatList.add(parent2);
        flatList.add(shared2);

        List<DependencyInfo> tree = builder.buildTree(flatList, true);

        // With merge duplicates, shared should only appear under first parent
        assertThat(tree).hasSize(2);
        assertThat(tree.get(0).getChildren()).hasSize(1);
        assertThat(tree.get(1).getChildren()).isEmpty();
    }

    @Test
    public void testNodeDisplayText() {
        DependencyInfo dep = createDependency("org.example", "lib1", "1.0", 0, false);
        DependencyTreeNode node = new DependencyTreeNode(dep);

        // Node should display dependency coordinates
        assertThat(node.getDisplayText()).isEqualTo("org.example:lib1");
    }

    @Test
    public void testNodeWithMigrationStatus() {
        DependencyInfo dep = createDependency("org.example", "lib1", "1.0", 0, false);
        dep.setMigrationStatus(DependencyMigrationStatus.NEEDS_UPGRADE);
        
        DependencyTreeNode node = new DependencyTreeNode(dep);

        assertThat(node.getDependency().getMigrationStatus()).isEqualTo(DependencyMigrationStatus.NEEDS_UPGRADE);
    }

    /**
     * Helper method to create a test DependencyInfo.
     */
    private DependencyInfo createDependency(String groupId, String artifactId, String version, 
                                           int depth, boolean isTransitive) {
        DependencyInfo dep = new DependencyInfo();
        dep.setGroupId(groupId);
        dep.setArtifactId(artifactId);
        dep.setCurrentVersion(version);
        dep.setDepth(depth);
        dep.setTransitive(isTransitive);
        dep.setScope("compile");
        dep.setChildren(new ArrayList<>());
        return dep;
    }
}
