package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.model.DependencyInfo;
import adrianmikula.jakartamigration.intellij.model.DependencyMigrationStatus;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for DependencyGraphComponent legacy API (updateGraph with List<DependencyInfo>).
 * Tests the flat-list-to-graph conversion logic.
 */
public class DependencyGraphComponentLegacyApiTest extends BasePlatformTestCase {

    private DependencyGraphComponent graphComponent;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        graphComponent = new DependencyGraphComponent(getProject());
    }

    @Test
    public void testSingleDependencyCreatesRootPlusOneNodePlusOneEdge() throws Exception {
        // Arrange
        List<DependencyInfo> deps = DependencyInfoTestFactory.createSingleDependencyList();

        // Act
        graphComponent.updateGraph(deps);

        // Assert - Access GraphCanvas to verify node/edge counts
        GraphCanvas canvas = extractGraphCanvas(graphComponent);
        List<GraphNode> nodes = GraphCanvasTestHelper.getNodes(canvas);
        List<GraphEdge> edges = GraphCanvasTestHelper.getEdges(canvas);

        // Should have root + 1 dependency = 2 nodes
        assertThat(nodes).hasSize(2);
        // Should have 1 edge from root to dependency
        assertThat(edges).hasSize(1);
        
        // Verify root node exists
        assertThat(nodes).anyMatch(n -> "root:root".equals(n.getId()));
        // Verify dependency node exists
        assertThat(nodes).anyMatch(n -> "org.example:dep1".equals(n.getId()));
    }

    @Test
    public void testMultipleDependenciesCreatesCorrectNodesAndEdges() throws Exception {
        // Arrange
        List<DependencyInfo> deps = DependencyInfoTestFactory.createMultipleDependenciesList();

        // Act
        graphComponent.updateGraph(deps);

        // Assert
        GraphCanvas canvas = extractGraphCanvas(graphComponent);
        List<GraphNode> nodes = GraphCanvasTestHelper.getNodes(canvas);
        List<GraphEdge> edges = GraphCanvasTestHelper.getEdges(canvas);

        // Should have root + 3 dependencies = 4 nodes
        assertThat(nodes).hasSize(4);
        // Should have 3 edges from root to each dependency
        assertThat(edges).hasSize(3);
        
        // Verify all dependency nodes exist
        assertThat(nodes).anyMatch(n -> "org.example:dep1".equals(n.getId()));
        assertThat(nodes).anyMatch(n -> "org.example:dep2".equals(n.getId()));
        assertThat(nodes).anyMatch(n -> "org.example:dep3".equals(n.getId()));
    }

    @Test
    public void testEmptyListCreatesOnlyRootNode() throws Exception {
        // Arrange
        List<DependencyInfo> deps = DependencyInfoTestFactory.createEmptyList();

        // Act
        graphComponent.updateGraph(deps);

        // Assert
        GraphCanvas canvas = extractGraphCanvas(graphComponent);
        List<GraphNode> nodes = GraphCanvasTestHelper.getNodes(canvas);
        List<GraphEdge> edges = GraphCanvasTestHelper.getEdges(canvas);

        // Should have only root node
        assertThat(nodes).hasSize(1);
        assertThat(nodes).anyMatch(n -> "root:root".equals(n.getId()));
        // Should have no edges
        assertThat(edges).isEmpty();
    }

    @Test
    public void testNullListCreatesOnlyRootNode() throws Exception {
        // Act
        graphComponent.updateGraph(null);

        // Assert
        GraphCanvas canvas = extractGraphCanvas(graphComponent);
        List<GraphNode> nodes = GraphCanvasTestHelper.getNodes(canvas);
        List<GraphEdge> edges = GraphCanvasTestHelper.getEdges(canvas);

        // Should have only root node
        assertThat(nodes).hasSize(1);
        assertThat(nodes).anyMatch(n -> "root:root".equals(n.getId()));
        // Should have no edges
        assertThat(edges).isEmpty();
    }

    @Test
    public void testTransitiveFlagIsPreserved() throws Exception {
        // Arrange
        List<DependencyInfo> deps = DependencyInfoTestFactory.createMultipleDependenciesList();

        // Act
        graphComponent.updateGraph(deps);

        // Assert
        GraphCanvas canvas = extractGraphCanvas(graphComponent);
        List<GraphNode> nodes = GraphCanvasTestHelper.getNodes(canvas);

        // Find dep3 which should be transitive
        GraphNode dep3 = nodes.stream()
            .filter(n -> "org.example:dep3".equals(n.getId()))
            .findFirst()
            .orElse(null);

        assertThat(dep3).isNotNull();
        assertThat(dep3.isTransitive()).isTrue();
    }

    @Test
    public void testVersionIsCorrectlySet() throws Exception {
        // Arrange
        List<DependencyInfo> deps = DependencyInfoTestFactory.createSingleDependencyList();

        // Act
        graphComponent.updateGraph(deps);

        // Assert
        GraphCanvas canvas = extractGraphCanvas(graphComponent);
        List<GraphNode> nodes = GraphCanvasTestHelper.getNodes(canvas);

        // Find the dependency node
        GraphNode dep1 = nodes.stream()
            .filter(n -> "org.example:dep1".equals(n.getId()))
            .findFirst()
            .orElse(null);

        assertThat(dep1).isNotNull();
        // The label should contain the artifact ID
        assertThat(dep1.getLabel()).isEqualTo("dep1");
    }

    @Test
    public void testDependencyWithNullVersionHandledGracefully() throws Exception {
        // Arrange
        List<DependencyInfo> deps = List.of(DependencyInfoTestFactory.createDependencyWithNullVersion());

        // Act
        graphComponent.updateGraph(deps);

        // Assert - Should not throw exception
        GraphCanvas canvas = extractGraphCanvas(graphComponent);
        List<GraphNode> nodes = GraphCanvasTestHelper.getNodes(canvas);

        // Should have root + 1 node even with null version
        assertThat(nodes).hasSize(2);
    }

    @Test
    public void testDependencyWithNullGroupIdIsSkipped() throws Exception {
        // Arrange
        List<DependencyInfo> deps = List.of(DependencyInfoTestFactory.createDependencyWithNullGroupId());

        // Act
        graphComponent.updateGraph(deps);

        // Assert
        GraphCanvas canvas = extractGraphCanvas(graphComponent);
        List<GraphNode> nodes = GraphCanvasTestHelper.getNodes(canvas);

        // Should have only root node (dependency with null groupId should be skipped)
        assertThat(nodes).hasSize(1);
        assertThat(nodes).anyMatch(n -> "root:root".equals(n.getId()));
    }

    @Test
    public void testDependencyWithNullArtifactIdIsSkipped() throws Exception {
        // Arrange
        List<DependencyInfo> deps = List.of(DependencyInfoTestFactory.createDependencyWithNullArtifactId());

        // Act
        graphComponent.updateGraph(deps);

        // Assert
        GraphCanvas canvas = extractGraphCanvas(graphComponent);
        List<GraphNode> nodes = GraphCanvasTestHelper.getNodes(canvas);

        // Should have only root node (dependency with null artifactId should be skipped)
        assertThat(nodes).hasSize(1);
        assertThat(nodes).anyMatch(n -> "root:root".equals(n.getId()));
    }

    // Helper method to extract GraphCanvas from DependencyGraphComponent
    private GraphCanvas extractGraphCanvas(DependencyGraphComponent component) throws Exception {
        java.lang.reflect.Field canvasField = DependencyGraphComponent.class.getDeclaredField("graphCanvas");
        canvasField.setAccessible(true);
        return (GraphCanvas) canvasField.get(component);
    }
}
