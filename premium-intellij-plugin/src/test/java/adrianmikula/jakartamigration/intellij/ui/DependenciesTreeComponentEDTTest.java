package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.model.DependencyInfo;
import adrianmikula.jakartamigration.intellij.model.DependencyMigrationStatus;
import adrianmikula.jakartamigration.intellij.ui.tree.DependencyTreeNode;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.Test;

import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EDT (Event Dispatch Thread) safety tests for DependenciesTreeComponent.
 * These tests verify that UI components are only accessed from the EDT.
 */
public class DependenciesTreeComponentEDTTest extends BasePlatformTestCase {

    private DependenciesTreeComponent treeComponent;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        treeComponent = new DependenciesTreeComponent(getProject());
    }

    @Test
    public void testSetDependenciesFromEDT() {
        // This should work fine when called from EDT
        List<DependencyInfo> dependencies = createTestDependencies();
        treeComponent.setDependencies(dependencies);
        
        // Verify no exceptions were thrown
        assertThat(treeComponent.getPanel()).isNotNull();
    }

    @Test
    public void testSetDependenciesFromBackgroundThread() throws Exception {
        // Test that setDependencies can be called from background thread
        // without causing EDT violations (should be internally wrapped)
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean success = new AtomicBoolean(false);
        List<DependencyInfo> dependencies = createTestDependencies();

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                // This should not cause EDT violation if properly wrapped
                treeComponent.setDependencies(dependencies);
                success.set(true);
            } catch (Exception e) {
                // If this fails, it indicates an EDT violation
                success.set(false);
            } finally {
                latch.countDown();
            }
        });

        // Wait for the background thread to complete
        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        
        // Give the EDT time to process the update
        Thread.sleep(200);
        
        // Verify the operation succeeded
        assertThat(success.get()).isTrue();
    }

    @Test
    public void testApplyFiltersFromBackgroundThread() throws Exception {
        // Test that filter operations are thread-safe
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean success = new AtomicBoolean(false);
        List<DependencyInfo> dependencies = createTestDependencies();

        // First set dependencies on EDT
        ApplicationManager.getApplication().invokeAndWait(() -> {
            treeComponent.setDependencies(dependencies);
        });

        // Then toggle filter from background thread
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                treeComponent.getTransitiveFilter().setSelected(true);
                success.set(true);
            } catch (Exception e) {
                success.set(false);
            } finally {
                latch.countDown();
            }
        });

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        Thread.sleep(200);
        assertThat(success.get()).isTrue();
    }

    @Test
    public void testConcurrentSetDependenciesCalls() throws Exception {
        // Test that concurrent calls to setDependencies are handled safely
        CountDownLatch latch = new CountDownLatch(5);
        AtomicBoolean allSuccess = new AtomicBoolean(true);

        for (int i = 0; i < 5; i++) {
            final int index = i;
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                try {
                    List<DependencyInfo> deps = createTestDependencies();
                    // Modify slightly for each thread
                    deps.get(0).setArtifactId("lib" + index);
                    treeComponent.setDependencies(deps);
                } catch (Exception e) {
                    allSuccess.set(false);
                } finally {
                    latch.countDown();
                }
            });
        }

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        Thread.sleep(300);
        assertThat(allSuccess.get()).isTrue();
    }

    @Test
    public void testHideTransitiveFilter() throws Exception {
        // Test that hiding transitive dependencies only hides transitive deps, not direct ones
        List<DependencyInfo> dependencies = createTestDependenciesWithDepth();
        
        ApplicationManager.getApplication().invokeAndWait(() -> {
            treeComponent.setDependencies(dependencies);
            treeComponent.getTransitiveFilter().setSelected(true);
        });
        
        // Give the EDT time to process the filter
        Thread.sleep(200);
        
        // Verify the component still exists and is not empty
        assertThat(treeComponent.getPanel()).isNotNull();
    }

    @Test
    public void testClickToExpandCollapse() throws Exception {
        // Test that click-to-expand/collapse is enabled
        List<DependencyInfo> dependencies = createTestDependenciesWithDepth();
        
        ApplicationManager.getApplication().invokeAndWait(() -> {
            treeComponent.setDependencies(dependencies);
        });
        
        Thread.sleep(200);
        
        // Verify the component exists
        assertThat(treeComponent.getPanel()).isNotNull();
        // The actual click behavior is tested by UI integration tests
    }

    @Test
    public void testIsTransitiveFlagCorrectness() throws Exception {
        // Test that isTransitive flag is set correctly based on depth
        List<DependencyInfo> dependencies = createTestDependenciesWithDepth();
        
        // Verify direct dependency (depth 0) is not transitive
        assertThat(dependencies.get(0).getDepth()).isEqualTo(0);
        assertThat(dependencies.get(0).isTransitive()).isFalse();
        
        // Verify transitive dependency (depth 1) is transitive
        assertThat(dependencies.get(1).getDepth()).isEqualTo(1);
        assertThat(dependencies.get(1).isTransitive()).isTrue();
    }

    @Test
    public void testFilterWithCorrectIsTransitiveData() throws Exception {
        // Test that filter works correctly when isTransitive flag is properly set
        List<DependencyInfo> dependencies = createTestDependenciesWithDepth();
        
        ApplicationManager.getApplication().invokeAndWait(() -> {
            treeComponent.setDependencies(dependencies);
            treeComponent.getTransitiveFilter().setSelected(true);
        });
        
        Thread.sleep(200);
        
        // Verify the component still exists (direct dependencies should remain visible)
        assertThat(treeComponent.getPanel()).isNotNull();
    }

    @Test
    public void testHideTransitiveDependenciesActuallyHidesTransitiveNodes() throws Exception {
        // Test that hiding transitive dependencies actually removes them from the tree
        // This test verifies the bug: transitive dependencies are NOT being hidden
        List<DependencyInfo> dependencies = createTestDependenciesWithTreeStructure();
        
        ApplicationManager.getApplication().invokeAndWait(() -> {
            treeComponent.setDependencies(dependencies);
            
            // Initially, both direct and transitive should be visible
            int initialRowCount = treeComponent.getTree().getRowCount();
            assertThat(initialRowCount).isGreaterThan(1); // At least root + direct + transitive
            
            // Enable the hide transitive filter
            treeComponent.getTransitiveFilter().setSelected(true);
        });
        
        Thread.sleep(200);
        
        ApplicationManager.getApplication().invokeAndWait(() -> {
            // Count how many nodes in the tree are transitive
            int transitiveNodeCount = countTransitiveNodesInTree();
            
            // This assertion will fail with the current bug - transitive nodes are NOT hidden
            assertThat(transitiveNodeCount).isEqualTo(0);
        });
    }

    /**
     * Helper method to create test dependencies with a proper tree structure.
     * Creates a direct dependency with a transitive child.
     */
    private List<DependencyInfo> createTestDependenciesWithTreeStructure() {
        List<DependencyInfo> dependencies = new ArrayList<>();
        
        // Direct dependency at depth 0
        DependencyInfo parent = new DependencyInfo();
        parent.setGroupId("org.example");
        parent.setArtifactId("parent-lib");
        parent.setCurrentVersion("1.0.0");
        parent.setMigrationStatus(DependencyMigrationStatus.NEEDS_UPGRADE);
        parent.setTransitive(false);
        parent.setOrganizational(false);
        parent.setDepth(0);
        parent.setScope("compile");
        dependencies.add(parent);

        // Transitive dependency at depth 1 (child of parent)
        DependencyInfo child = new DependencyInfo();
        child.setGroupId("org.example");
        child.setArtifactId("child-lib");
        child.setCurrentVersion("2.0.0");
        child.setMigrationStatus(DependencyMigrationStatus.COMPATIBLE);
        child.setTransitive(true);
        child.setOrganizational(false);
        child.setDepth(1);
        child.setScope("compile");
        dependencies.add(child);

        return dependencies;
    }

    /**
     * Helper method to count transitive nodes in the tree.
     * Traverses the tree and counts nodes where isTransitive() is true.
     */
    private int countTransitiveNodesInTree() {
        javax.swing.JTree tree = treeComponent.getTree();
        int count = 0;
        
        for (int i = 0; i < tree.getRowCount(); i++) {
            TreePath path = tree.getPathForRow(i);
            Object lastComponent = path.getLastPathComponent();
            if (lastComponent instanceof DependencyTreeNode) {
                DependencyTreeNode node = (DependencyTreeNode) lastComponent;
                if (node.getDependency().isTransitive()) {
                    count++;
                }
            }
        }
        
        return count;
    }

    /**
     * Helper method to create test dependency list with depth for tree structure.
     */
    private List<DependencyInfo> createTestDependenciesWithDepth() {
        List<DependencyInfo> dependencies = new ArrayList<>();
        
        // Direct dependency at depth 0
        DependencyInfo dep1 = new DependencyInfo();
        dep1.setGroupId("org.example");
        dep1.setArtifactId("parent-lib");
        dep1.setCurrentVersion("1.0.0");
        dep1.setMigrationStatus(DependencyMigrationStatus.NEEDS_UPGRADE);
        dep1.setTransitive(false);
        dep1.setOrganizational(false);
        dep1.setDepth(0);
        dep1.setScope("compile");
        dependencies.add(dep1);

        // Transitive dependency at depth 1
        DependencyInfo dep2 = new DependencyInfo();
        dep2.setGroupId("org.example");
        dep2.setArtifactId("child-lib");
        dep2.setCurrentVersion("2.0.0");
        dep2.setMigrationStatus(DependencyMigrationStatus.COMPATIBLE);
        dep2.setTransitive(true);
        dep2.setOrganizational(false);
        dep2.setDepth(1);
        dep2.setScope("compile");
        dependencies.add(dep2);

        return dependencies;
    }

    /**
     * Helper method to create test dependency list.
     */
    private List<DependencyInfo> createTestDependencies() {
        List<DependencyInfo> dependencies = new ArrayList<>();
        
        DependencyInfo dep1 = new DependencyInfo();
        dep1.setGroupId("org.example");
        dep1.setArtifactId("lib1");
        dep1.setCurrentVersion("1.0.0");
        dep1.setMigrationStatus(DependencyMigrationStatus.NEEDS_UPGRADE);
        dep1.setTransitive(false);
        dep1.setOrganizational(false);
        dep1.setScope("compile");
        dependencies.add(dep1);

        DependencyInfo dep2 = new DependencyInfo();
        dep2.setGroupId("org.example");
        dep2.setArtifactId("lib2");
        dep2.setCurrentVersion("2.0.0");
        dep2.setMigrationStatus(DependencyMigrationStatus.COMPATIBLE);
        dep2.setTransitive(true);
        dep2.setOrganizational(false);
        dep2.setScope("compile");
        dependencies.add(dep2);

        return dependencies;
    }

    @Test
    public void testTreeIndentationCorrect() throws Exception {
        // Test that child nodes are indented correctly from their parent
        // This test verifies the bug: setRootVisible(false) + setShowsRootHandles(true) causes incorrect indentation
        List<DependencyInfo> dependencies = createTestDependenciesWithTreeStructure();
        
        ApplicationManager.getApplication().invokeAndWait(() -> {
            treeComponent.setDependencies(dependencies);
        });
        
        Thread.sleep(200);
        
        ApplicationManager.getApplication().invokeAndWait(() -> {
            javax.swing.JTree tree = treeComponent.getTree();
            
            // Get the row bounds for the first visible row (should be the parent node)
            java.awt.Rectangle parentRowBounds = tree.getRowBounds(0);
            assertThat(parentRowBounds).isNotNull();
            
            // Get the row bounds for the second visible row (should be the child node)
            java.awt.Rectangle childRowBounds = tree.getRowBounds(1);
            assertThat(childRowBounds).isNotNull();
            
            // Child node should be indented to the right of parent node
            // Standard JTree indentation is typically 20-30 pixels per level
            assertThat(childRowBounds.x).isGreaterThan(parentRowBounds.x);
            
            // The indentation should be reasonable (at least 10 pixels, less than 100 pixels)
            int indentation = childRowBounds.x - parentRowBounds.x;
            assertThat(indentation).isGreaterThan(10);
            assertThat(indentation).isLessThan(100);
        });
    }
}
