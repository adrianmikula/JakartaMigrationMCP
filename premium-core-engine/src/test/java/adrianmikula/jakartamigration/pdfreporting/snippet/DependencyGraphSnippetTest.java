package adrianmikula.jakartamigration.pdfreporting.snippet;

import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Dependency;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DependencyGraphSnippet.
 * Verifies that the dependency graph visualization generates valid HTML and JSON data.
 */
class DependencyGraphSnippetTest {

    @Test
    @DisplayName("Should generate graph with valid HTML structure")
    void shouldGenerateValidHtmlStructure() throws SnippetGenerationException {
        // Arrange
        var dependencyGraph = createMockDependencyGraph();
        var snippet = new DependencyGraphSnippet(dependencyGraph);

        // Act
        String html = snippet.generate();

        // Assert
        assertTrue(html.contains("Dependency Graph Visualization"), "Should have section title");
        assertTrue(html.contains("dependency-graph-container"), "Should have graph container");
        assertTrue(html.contains("graph-legend"), "Should have legend");
        assertTrue(html.contains("Jakarta Compatible"), "Should have green legend item");
        assertTrue(html.contains("Needs Update"), "Should have yellow legend item");
        assertTrue(html.contains("No Jakarta Version"), "Should have red legend item");
        assertTrue(html.contains("Unknown Status"), "Should have gray legend item");
    }

    @Test
    @DisplayName("Should generate valid JSON for nodes")
    void shouldGenerateValidNodesJson() throws SnippetGenerationException {
        // Arrange
        var dependencyGraph = createMockDependencyGraph();
        var snippet = new DependencyGraphSnippet(dependencyGraph);

        // Act
        String html = snippet.generate();

        // Assert
        assertTrue(html.contains("var graphNodes ="), "Should define graphNodes variable");
        assertTrue(html.contains("\"id\":"), "Should have node id");
        assertTrue(html.contains("\"label\":"), "Should have node label");
        assertTrue(html.contains("\"color\":"), "Should have node color");
        assertTrue(html.contains("\"title\":"), "Should have node title");
    }

    @Test
    @DisplayName("Should generate valid JSON for edges")
    void shouldGenerateValidEdgesJson() throws SnippetGenerationException {
        // Arrange
        var dependencyGraph = createMockDependencyGraph();
        var snippet = new DependencyGraphSnippet(dependencyGraph);

        // Act
        String html = snippet.generate();

        // Assert
        assertTrue(html.contains("var graphEdges ="), "Should define graphEdges variable");
        assertTrue(html.contains("\"from\":"), "Should have edge from");
        assertTrue(html.contains("\"to\":"), "Should have edge to");
    }

    @Test
    @DisplayName("Should apply correct color coding for Jakarta compatible artifacts")
    void shouldApplyGreenColorForJakartaCompatible() throws SnippetGenerationException {
        // Arrange
        var dependencyGraph = createDependencyGraphWithJakartaCompatible();
        var snippet = new DependencyGraphSnippet(dependencyGraph);

        // Act
        String html = snippet.generate();

        // Assert
        assertTrue(html.contains("#28a745"), "Should use green for Jakarta compatible");
        assertTrue(html.contains("#1e7e34"), "Should use green border for Jakarta compatible");
    }

    @Test
    @DisplayName("Should apply correct color coding for javax dependencies")
    void shouldApplyYellowColorForJavaxDependencies() throws SnippetGenerationException {
        // Arrange
        var dependencyGraph = createDependencyGraphWithJavax();
        var snippet = new DependencyGraphSnippet(dependencyGraph);

        // Act
        String html = snippet.generate();

        // Assert
        assertTrue(html.contains("#ffc107"), "Should use yellow for javax dependencies");
        assertTrue(html.contains("#d39e00"), "Should use yellow border for javax dependencies");
    }

    @Test
    @DisplayName("Should apply correct color coding for incompatible dependencies")
    void shouldApplyRedColorForIncompatible() throws SnippetGenerationException {
        // Arrange
        var dependencyGraph = createDependencyGraphWithIncompatible();
        var snippet = new DependencyGraphSnippet(dependencyGraph);

        // Act
        String html = snippet.generate();

        // Assert
        assertTrue(html.contains("#dc3545"), "Should use red for incompatible");
        assertTrue(html.contains("#c82333"), "Should use red border for incompatible");
    }

    @Test
    @DisplayName("Should include Vis.js initialization code")
    void shouldIncludeVisJsInitialization() throws SnippetGenerationException {
        // Arrange
        var dependencyGraph = createMockDependencyGraph();
        var snippet = new DependencyGraphSnippet(dependencyGraph);

        // Act
        String html = snippet.generate();

        // Assert
        assertTrue(html.contains("new vis.Network"), "Should initialize Vis.js network");
        assertTrue(html.contains("vis.DataSet"), "Should use Vis.js DataSet");
        assertTrue(html.contains("forceAtlas2Based"), "Should use force-directed layout");
        assertTrue(html.contains("stabilizationIterationsDone"), "Should handle stabilization");
    }

    @Test
    @DisplayName("Should show no dependencies message when graph is empty")
    void shouldShowNoDependenciesMessageForEmptyGraph() throws SnippetGenerationException {
        // Arrange
        var emptyGraph = new DependencyGraph();
        var snippet = new DependencyGraphSnippet(emptyGraph);

        // Act
        String html = snippet.generate();

        // Assert
        assertTrue(html.contains("No Dependencies Found"), "Should show no dependencies message");
        assertTrue(html.contains("Eclipse project"), "Should mention Eclipse project possibility");
        assertFalse(html.contains("dependency-graph-container"), "Should not render graph container");
    }

    @Test
    @DisplayName("Should show no dependencies message when graph is null")
    void shouldShowNoDependenciesMessageForNullGraph() throws SnippetGenerationException {
        // Arrange
        var snippet = new DependencyGraphSnippet(null);

        // Act
        String html = snippet.generate();

        // Assert
        assertTrue(html.contains("No Dependencies Found"), "Should show no dependencies message");
    }

    @Test
    @DisplayName("Should be applicable when dependency graph exists")
    void shouldBeApplicableWithValidGraph() {
        // Arrange
        var dependencyGraph = createMockDependencyGraph();
        var snippet = new DependencyGraphSnippet(dependencyGraph);

        // Act & Assert
        assertTrue(snippet.isApplicable());
    }

    @Test
    @DisplayName("Should not be applicable when dependency graph is null")
    void shouldNotBeApplicableWithNullGraph() {
        // Arrange
        var snippet = new DependencyGraphSnippet(null);

        // Act & Assert
        assertFalse(snippet.isApplicable());
    }

    @Test
    @DisplayName("Should return correct order")
    void shouldReturnCorrectOrder() {
        // Arrange
        var dependencyGraph = createMockDependencyGraph();
        var snippet = new DependencyGraphSnippet(dependencyGraph);

        // Act & Assert
        assertEquals(35, snippet.getOrder());
    }

    @Test
    @DisplayName("Should truncate long labels")
    void shouldTruncateLongLabels() throws SnippetGenerationException {
        // Arrange
        var dependencyGraph = createDependencyGraphWithLongLabel();
        var snippet = new DependencyGraphSnippet(dependencyGraph);

        // Act
        String html = snippet.generate();

        // Assert
        assertTrue(html.contains("..."), "Should truncate long labels with ellipsis");
    }

    @Test
    @DisplayName("Should escape special characters in JSON")
    void shouldEscapeSpecialCharactersInJson() throws SnippetGenerationException {
        // Arrange
        var dependencyGraph = createDependencyGraphWithSpecialChars();
        var snippet = new DependencyGraphSnippet(dependencyGraph);

        // Act
        String html = snippet.generate();

        // Assert
        // Should not contain unescaped quotes or special characters that would break JSON
        assertFalse(html.contains("\"label\": \"test\"artifact\""), "Should escape quotes in labels");
    }

    @Test
    @DisplayName("Should include physics configuration for large graphs")
    void shouldIncludePhysicsConfiguration() throws SnippetGenerationException {
        // Arrange
        var dependencyGraph = createMockDependencyGraph();
        var snippet = new DependencyGraphSnippet(dependencyGraph);

        // Act
        String html = snippet.generate();

        // Assert
        assertTrue(html.contains("gravitationalConstant"), "Should configure gravity");
        assertTrue(html.contains("springLength"), "Should configure spring length");
        assertTrue(html.contains("springConstant"), "Should configure spring constant");
        assertTrue(html.contains("maxVelocity"), "Should configure max velocity");
    }

    @Test
    @DisplayName("Should enable interaction options")
    void shouldEnableInteractionOptions() throws SnippetGenerationException {
        // Arrange
        var dependencyGraph = createMockDependencyGraph();
        var snippet = new DependencyGraphSnippet(dependencyGraph);

        // Act
        String html = snippet.generate();

        // Assert
        assertTrue(html.contains("hover: true"), "Should enable hover");
        assertTrue(html.contains("zoomView: true"), "Should enable zoom");
        assertTrue(html.contains("dragView: true"), "Should enable drag");
    }

    @Test
    @DisplayName("Should handle large graphs with 100+ dependencies")
    void shouldHandleLargeGraphs() throws SnippetGenerationException {
        // Arrange
        var dependencyGraph = createLargeDependencyGraph(100);
        var snippet = new DependencyGraphSnippet(dependencyGraph);

        // Act
        String html = snippet.generate();

        // Assert
        assertTrue(html.contains("Dependency Graph Visualization"), "Should generate graph section");
        assertTrue(html.contains("dependency-graph-container"), "Should have graph container");
        // Verify it generates all nodes
        assertTrue(html.contains("\"id\":"), "Should have node ids");
        // Count occurrences of "id:" to verify we have ~100 nodes
        long idCount = html.lines().filter(line -> line.contains("\"id\":")).count();
        assertTrue(idCount >= 100, "Should have at least 100 nodes, found: " + idCount);
    }

    // Helper methods to create mock data

    private DependencyGraph createMockDependencyGraph() {
        Set<Artifact> nodes = new HashSet<>();
        Set<Dependency> edges = new HashSet<>();

        Artifact artifact1 = new Artifact("org.example", "example-lib", "1.0.0", "compile", false);
        Artifact artifact2 = new Artifact("jakarta.example", "jakarta-lib", "2.0.0", "compile", false);
        Artifact artifact3 = new Artifact("org.test", "test-lib", "2.0.0", "compile", false);

        nodes.add(artifact1);
        nodes.add(artifact2);
        nodes.add(artifact3);

        edges.add(new Dependency(artifact1, artifact2, "compile", false));
        edges.add(new Dependency(artifact1, artifact3, "compile", false));

        return new DependencyGraph(nodes, edges);
    }

    private DependencyGraph createDependencyGraphWithJakartaCompatible() {
        Set<Artifact> nodes = new HashSet<>();
        Set<Dependency> edges = new HashSet<>();

        Artifact jakartaArtifact = new Artifact("jakarta.platform", "jakarta-core", "10.0.0", "compile", false);
        nodes.add(jakartaArtifact);

        return new DependencyGraph(nodes, edges);
    }

    private DependencyGraph createDependencyGraphWithJavax() {
        Set<Artifact> nodes = new HashSet<>();
        Set<Dependency> edges = new HashSet<>();

        Artifact javaxArtifact = new Artifact("javax.servlet", "servlet-api", "2.5", "compile", false);
        nodes.add(javaxArtifact);

        return new DependencyGraph(nodes, edges);
    }

    private DependencyGraph createDependencyGraphWithIncompatible() {
        Set<Artifact> nodes = new HashSet<>();
        Set<Dependency> edges = new HashSet<>();

        Artifact oldArtifact = new Artifact("org.legacy", "old-lib", "0.5.0", "compile", false);
        nodes.add(oldArtifact);

        return new DependencyGraph(nodes, edges);
    }

    private DependencyGraph createDependencyGraphWithLongLabel() {
        Set<Artifact> nodes = new HashSet<>();
        Set<Dependency> edges = new HashSet<>();

        Artifact longLabelArtifact = new Artifact("org.very.long.package.name", 
            "very-long-artifact-name-that-exceeds-thirty-characters", "1.0.0", "compile", false);
        nodes.add(longLabelArtifact);

        return new DependencyGraph(nodes, edges);
    }

    private DependencyGraph createDependencyGraphWithSpecialChars() {
        Set<Artifact> nodes = new HashSet<>();
        Set<Dependency> edges = new HashSet<>();

        Artifact specialArtifact = new Artifact("org.test", "test\"artifact", "1.0.0", "compile", false);
        nodes.add(specialArtifact);

        return new DependencyGraph(nodes, edges);
    }

    private DependencyGraph createLargeDependencyGraph(int nodeCount) {
        Set<Artifact> nodes = new HashSet<>();
        Set<Dependency> edges = new HashSet<>();

        // Create root node
        Artifact root = new Artifact("root", "project", "1.0.0", "compile", false);
        nodes.add(root);

        // Create many dependency nodes
        for (int i = 0; i < nodeCount; i++) {
            String groupId = i % 3 == 0 ? "javax.example" : (i % 3 == 1 ? "jakarta.example" : "org.example");
            Artifact artifact = new Artifact(groupId, "lib-" + i, "1.0." + i, "compile", i > 10);
            nodes.add(artifact);
            
            // Connect to root for first 20, chain the rest
            if (i < 20) {
                edges.add(new Dependency(root, artifact, "compile", false));
            } else if (i > 0) {
                Artifact prevArtifact = new Artifact(groupId, "lib-" + (i - 1), "1.0." + (i - 1), "compile", (i - 1) > 10);
                edges.add(new Dependency(prevArtifact, artifact, "compile", false));
            }
        }

        return new DependencyGraph(nodes, edges);
    }
}
