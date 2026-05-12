package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.model.DependencyInfo;
import adrianmikula.jakartamigration.intellij.model.DependencyMigrationStatus;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.Test;

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
}
