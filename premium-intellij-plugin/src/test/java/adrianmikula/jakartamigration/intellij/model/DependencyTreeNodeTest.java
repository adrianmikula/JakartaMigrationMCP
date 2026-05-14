package adrianmikula.jakartamigration.intellij.model;

import adrianmikula.jakartamigration.intellij.ui.tree.DependencyTreeNode;
import org.junit.Test;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for DependencyTreeNode.
 */
public class DependencyTreeNodeTest {

    @Test
    public void testGetDisplayText() {
        DependencyInfo dep = createDependency("org.example", "lib1", "1.0.0");
        DependencyTreeNode node = new DependencyTreeNode(dep);
        
        assertThat(node.getDisplayText()).isEqualTo("org.example:lib1");
    }

    @Test
    public void testHasChildDependencies() {
        DependencyInfo dep = createDependency("org.example", "lib1", "1.0.0");
        dep.setChildren(new ArrayList<>());
        
        DependencyTreeNode node = new DependencyTreeNode(dep);
        assertThat(node.hasChildDependencies()).isFalse();
        
        dep.getChildren().add(createDependency("org.example", "lib2", "2.0.0"));
        assertThat(node.hasChildDependencies()).isTrue();
    }

    @Test
    public void testBuildChildNodes() {
        DependencyInfo parent = createDependency("org.example", "parent", "1.0.0");
        DependencyInfo child = createDependency("org.example", "child", "2.0.0");
        
        parent.setChildren(new ArrayList<>());
        parent.getChildren().add(child);
        
        DependencyTreeNode node = new DependencyTreeNode(parent);
        node.buildChildNodes();
        
        assertThat(node.getChildCount()).isEqualTo(1);
        assertThat(((DependencyTreeNode) node.getChildAt(0)).getDependency().getArtifactId()).isEqualTo("child");
    }

    @Test
    public void testToString() {
        DependencyInfo dep = createDependency("org.example", "lib1", "1.0.0");
        DependencyTreeNode node = new DependencyTreeNode(dep);
        
        assertThat(node.toString()).isEqualTo("org.example:lib1");
    }

    @Test
    public void testFilterTransitiveRemovesTransitiveNode() {
        // Test that a transitive node is removed
        DependencyInfo transitiveDep = createDependency("org.example", "transitive", "1.0.0");
        transitiveDep.setTransitive(true);
        
        DependencyTreeNode node = new DependencyTreeNode(transitiveDep);
        DependencyTreeNode filtered = node.filterTransitive();
        
        assertThat(filtered).isNull();
    }

    @Test
    public void testFilterTransitiveKeepsDirectNode() {
        // Test that a direct node is kept
        DependencyInfo directDep = createDependency("org.example", "direct", "1.0.0");
        directDep.setTransitive(false);
        
        DependencyTreeNode node = new DependencyTreeNode(directDep);
        DependencyTreeNode filtered = node.filterTransitive();
        
        assertThat(filtered).isNotNull();
        assertThat(filtered.getDependency().getArtifactId()).isEqualTo("direct");
    }

    @Test
    public void testFilterTransitiveRemovesTransitiveChildren() {
        // Test that transitive children are removed from a direct node
        DependencyInfo parent = createDependency("org.example", "parent", "1.0.0");
        parent.setTransitive(false);
        
        DependencyInfo transitiveChild = createDependency("org.example", "child", "2.0.0");
        transitiveChild.setTransitive(true);
        
        parent.setChildren(new ArrayList<>());
        parent.getChildren().add(transitiveChild);
        
        DependencyTreeNode node = new DependencyTreeNode(parent);
        DependencyTreeNode filtered = node.filterTransitive();
        
        assertThat(filtered).isNotNull();
        assertThat(filtered.getDependency().getArtifactId()).isEqualTo("parent");
        assertThat(filtered.getChildCount()).isEqualTo(0); // Child should be removed
    }

    @Test
    public void testFilterTransitiveKeepsDirectChildren() {
        // Test that direct children are kept
        DependencyInfo parent = createDependency("org.example", "parent", "1.0.0");
        parent.setTransitive(false);
        
        DependencyInfo directChild = createDependency("org.example", "child", "2.0.0");
        directChild.setTransitive(false);
        
        parent.setChildren(new ArrayList<>());
        parent.getChildren().add(directChild);
        
        DependencyTreeNode node = new DependencyTreeNode(parent);
        DependencyTreeNode filtered = node.filterTransitive();
        
        assertThat(filtered).isNotNull();
        assertThat(filtered.getDependency().getArtifactId()).isEqualTo("parent");
        assertThat(filtered.getChildCount()).isEqualTo(1); // Child should be kept
        assertThat(((DependencyTreeNode) filtered.getChildAt(0)).getDependency().getArtifactId()).isEqualTo("child");
    }

    @Test
    public void testFilterTransitiveRecursive() {
        // Test that filtering works recursively through multiple levels
        DependencyInfo root = createDependency("org.example", "root", "1.0.0");
        root.setTransitive(false);
        
        DependencyInfo level1Transitive = createDependency("org.example", "level1", "2.0.0");
        level1Transitive.setTransitive(true);
        
        DependencyInfo level1Direct = createDependency("org.example", "level1-direct", "2.0.0");
        level1Direct.setTransitive(false);
        
        DependencyInfo level2 = createDependency("org.example", "level2", "3.0.0");
        level2.setTransitive(true);
        
        root.setChildren(new ArrayList<>());
        root.getChildren().add(level1Transitive);
        root.getChildren().add(level1Direct);
        
        level1Direct.setChildren(new ArrayList<>());
        level1Direct.getChildren().add(level2);
        
        DependencyTreeNode node = new DependencyTreeNode(root);
        DependencyTreeNode filtered = node.filterTransitive();
        
        assertThat(filtered).isNotNull();
        assertThat(filtered.getDependency().getArtifactId()).isEqualTo("root");
        assertThat(filtered.getChildCount()).isEqualTo(1); // Only level1-direct should remain
        assertThat(((DependencyTreeNode) filtered.getChildAt(0)).getDependency().getArtifactId()).isEqualTo("level1-direct");
        assertThat(filtered.getChildAt(0).getChildCount()).isEqualTo(0); // level2 should be removed
    }

    private DependencyInfo createDependency(String groupId, String artifactId, String version) {
        DependencyInfo dep = new DependencyInfo();
        dep.setGroupId(groupId);
        dep.setArtifactId(artifactId);
        dep.setCurrentVersion(version);
        dep.setMigrationStatus(DependencyMigrationStatus.COMPATIBLE);
        dep.setTransitive(false);
        dep.setOrganizational(false);
        dep.setDepth(0);
        dep.setScope("compile");
        return dep;
    }
}
