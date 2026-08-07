package adrianmikula.jakartamigration.dependencyanalysis.domain;

import java.util.HashSet;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a dependency graph with nodes (artifacts) and edges
 * (dependencies).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
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
        this.nodes = nodes != null ? nodes : new HashSet<>();
        this.edges = edges != null ? edges : new HashSet<>();
    }

    @JsonProperty("nodes")
    public Set<Artifact> getNodes() {
        return nodes;
    }

    @JsonProperty("edges")
    public Set<Dependency> getEdges() {
        return edges;
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

    /**
     * Merges all nodes and edges from another graph into this one.
     * Deduplicates by artifact identity (groupId:artifactId:version:scope).
     */
    public void merge(DependencyGraph other) {
        if (other == null) return;
        for (Artifact node : other.nodes) {
            nodes.add(node);
        }
        for (Dependency edge : other.edges) {
            edges.add(edge);
        }
    }

    public int nodeCount() {
        return nodes.size();
    }

    public int edgeCount() {
        return edges.size();
    }

    public String getJakartaCompatibilityColor(Artifact artifact) {
        if (artifact.isJakartaCompatible()) {
            return "green";
        } else if (hasJakartaVersion(artifact)) {
            return "yellow";
        } else {
            return "red";
        }
    }

    private boolean hasJakartaVersion(Artifact artifact) {
        // This would be implemented to check if the artifact has a jakarta version available
        return artifact.groupId().startsWith("javax.");
    }
}
