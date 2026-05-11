package adrianmikula.jakartamigration.intellij.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for DependencyTreeBuilder.
 * Tests tree structure generation from flat dependency list.
 */
public class DependencyTreeBuilderTest {

    @Test
    public void testBuildTreeWithDirectDependenciesOnly() {
        List<DependencyInfo> dependencies = new ArrayList<>();
        dependencies.add(createDependency("org.example", "lib1", "1.0", 0, false));
        dependencies.add(createDependency("org.example", "lib2", "1.0", 0, false));

        DependencyTreeBuilder builder = new DependencyTreeBuilder();
        List<DependencyInfo> tree = builder.buildTree(dependencies, false);

        assertThat(tree).hasSize(2);
        assertThat(tree.get(0).getChildren()).isEmpty();
        assertThat(tree.get(1).getChildren()).isEmpty();
    }

    @Test
    public void testBuildTreeWithTransitiveDependencies() {
        List<DependencyInfo> dependencies = new ArrayList<>();
        // Direct dependency at depth 0
        dependencies.add(createDependency("org.example", "parent", "1.0", 0, false));
        // Transitive dependency at depth 1
        dependencies.add(createDependency("org.example", "child1", "1.0", 1, true));
        // Another transitive at depth 1
        dependencies.add(createDependency("org.example", "child2", "1.0", 1, true));

        DependencyTreeBuilder builder = new DependencyTreeBuilder();
        List<DependencyInfo> tree = builder.buildTree(dependencies, false);

        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).getArtifactId()).isEqualTo("parent");
        assertThat(tree.get(0).getChildren()).hasSize(2);
    }

    @Test
    public void testBuildTreeWithMultipleDepths() {
        List<DependencyInfo> dependencies = new ArrayList<>();
        // Direct dependency at depth 0
        dependencies.add(createDependency("org.example", "root", "1.0", 0, false));
        // First level transitive at depth 1
        dependencies.add(createDependency("org.example", "level1", "1.0", 1, true));
        // Second level transitive at depth 2
        dependencies.add(createDependency("org.example", "level2", "1.0", 2, true));

        DependencyTreeBuilder builder = new DependencyTreeBuilder();
        List<DependencyInfo> tree = builder.buildTree(dependencies, false);

        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).getArtifactId()).isEqualTo("root");
        assertThat(tree.get(0).getChildren()).hasSize(1);
        
        DependencyInfo level1 = tree.get(0).getChildren().get(0);
        assertThat(level1.getArtifactId()).isEqualTo("level1");
        assertThat(level1.getChildren()).hasSize(1);
        
        DependencyInfo level2 = level1.getChildren().get(0);
        assertThat(level2.getArtifactId()).isEqualTo("level2");
        assertThat(level2.getChildren()).isEmpty();
    }

    @Test
    public void testBuildTreeWithDuplicateTransitiveDependencies() {
        List<DependencyInfo> dependencies = new ArrayList<>();
        // First branch: parent1 with child
        dependencies.add(createDependency("org.example", "parent1", "1.0", 0, false));
        dependencies.add(createDependency("org.example", "shared", "1.0", 1, true));
        // Second branch: parent2 with same child
        dependencies.add(createDependency("org.example", "parent2", "1.0", 0, false));
        dependencies.add(createDependency("org.example", "shared", "1.0", 1, true));

        DependencyTreeBuilder builder = new DependencyTreeBuilder();
        List<DependencyInfo> tree = builder.buildTree(dependencies, false);

        // Without merge duplicates, both should appear
        assertThat(tree).hasSize(2);
        assertThat(tree.get(0).getChildren()).hasSize(1);
        assertThat(tree.get(1).getChildren()).hasSize(1);
    }

    @Test
    public void testBuildTreeWithMergeDuplicatesEnabled() {
        List<DependencyInfo> dependencies = new ArrayList<>();
        // First branch: parent1 with child
        dependencies.add(createDependency("org.example", "parent1", "1.0", 0, false));
        dependencies.add(createDependency("org.example", "shared", "1.0", 1, true));
        // Second branch: parent2 with same child
        dependencies.add(createDependency("org.example", "parent2", "1.0", 0, false));
        dependencies.add(createDependency("org.example", "shared", "1.0", 1, true));

        DependencyTreeBuilder builder = new DependencyTreeBuilder();
        List<DependencyInfo> tree = builder.buildTree(dependencies, true);

        // With merge duplicates, should only appear under first parent
        assertThat(tree).hasSize(2);
        assertThat(tree.get(0).getChildren()).hasSize(1);
        assertThat(tree.get(1).getChildren()).isEmpty();
    }

    @Test
    public void testBuildTreeWithEmptyList() {
        List<DependencyInfo> dependencies = new ArrayList<>();

        DependencyTreeBuilder builder = new DependencyTreeBuilder();
        List<DependencyInfo> tree = builder.buildTree(dependencies, false);

        assertThat(tree).isEmpty();
    }

    @Test
    public void testBuildTreePreservesDependencyInfo() {
        List<DependencyInfo> dependencies = new ArrayList<>();
        DependencyInfo dep = createDependency("org.example", "lib1", "1.0", 0, false);
        dep.setMigrationStatus(DependencyMigrationStatus.NEEDS_UPGRADE);
        dep.setScope("compile");
        dependencies.add(dep);

        DependencyTreeBuilder builder = new DependencyTreeBuilder();
        List<DependencyInfo> tree = builder.buildTree(dependencies, false);

        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).getGroupId()).isEqualTo("org.example");
        assertThat(tree.get(0).getArtifactId()).isEqualTo("lib1");
        assertThat(tree.get(0).getCurrentVersion()).isEqualTo("1.0");
        assertThat(tree.get(0).getMigrationStatus()).isEqualTo(DependencyMigrationStatus.NEEDS_UPGRADE);
        assertThat(tree.get(0).getScope()).isEqualTo("compile");
    }

    @Test
    public void testBuildTreeWithMultipleBranches() {
        List<DependencyInfo> dependencies = new ArrayList<>();
        // Root 1 with children
        dependencies.add(createDependency("org.example", "root1", "1.0", 0, false));
        dependencies.add(createDependency("org.example", "root1-child1", "1.0", 1, true));
        dependencies.add(createDependency("org.example", "root1-child2", "1.0", 1, true));
        // Root 2 with children
        dependencies.add(createDependency("org.example", "root2", "1.0", 0, false));
        dependencies.add(createDependency("org.example", "root2-child1", "1.0", 1, true));

        DependencyTreeBuilder builder = new DependencyTreeBuilder();
        List<DependencyInfo> tree = builder.buildTree(dependencies, false);

        assertThat(tree).hasSize(2);
        assertThat(tree.get(0).getArtifactId()).isEqualTo("root1");
        assertThat(tree.get(0).getChildren()).hasSize(2);
        assertThat(tree.get(1).getArtifactId()).isEqualTo("root2");
        assertThat(tree.get(1).getChildren()).hasSize(1);
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
        return dep;
    }
}
