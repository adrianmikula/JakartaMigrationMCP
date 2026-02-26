package adrianmikula.jakartamigration.dependencyanalysis.domain;

import java.util.HashSet;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a dependency graph with nodes (artifacts) and edges
 * (dependencies).
 */
public class DependencyGraph {
    private final Set<Artifact> nodes;
    private final Set<Dependency> edges;

    public DependencyGraph() {
        this.nodes = new HashSet<>();
        this.edges = new HashSet<>();
    }

    @JsonCreator
    public DependencyGraph(
            @JsonProperty("nodes") Set<Artifact> nodes,
            @JsonProperty("edges") Set<Dependency> edges) {
        this.nodes = nodes != null ? new HashSet<>(nodes) : new HashSet<>();
        this.edges = edges != null ? new HashSet<>(edges) : new HashSet<>();
    }

    @JsonProperty("nodes")
    public Set<Artifact> getNodes() {
        return new HashSet<>(nodes);
    }

    @JsonProperty("edges")
    public Set<Dependency> getEdges() {
        return new HashSet<>(edges);
    }

    public void addNode(Artifact artifact) {
        nodes.add(artifact);
    }

    public void addEdge(Dependency dependency) {
        nodes.add(dependency.from());
        nodes.add(dependency.to());
        edges.add(dependency);
    }

    public boolean containsNode(Artifact artifact) {
        return nodes.contains(artifact);
    }

    public boolean containsEdge(Dependency dependency) {
        return edges.contains(dependency);
    }

    public int nodeCount() {
        return nodes.size();
    }

    public int edgeCount() {
        return edges.size();
    }
}
