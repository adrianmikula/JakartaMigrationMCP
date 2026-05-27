package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Dependency;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;

import java.util.HashSet;
import java.util.Set;

/**
 * Factory class for creating mock DependencyGraph objects for testing.
 */
public class DependencyGraphTestFactory {

    /**
     * Create a simple graph with 1 node and no edges: dep1
     * (The canvas creates its own root node)
     */
    public static DependencyGraph createSimpleGraph() {
        Set<Artifact> nodes = new HashSet<>();
        Set<Dependency> edges = new HashSet<>();

        Artifact dep1 = new Artifact("org.example", "dep1", "1.0.0", "compile", false);

        nodes.add(dep1);

        return new DependencyGraph(nodes, edges);
    }

    /**
     * Create a chain graph: dep1 -> dep2
     * (The canvas creates its own root node)
     */
    public static DependencyGraph createChainGraph() {
        Set<Artifact> nodes = new HashSet<>();
        Set<Dependency> edges = new HashSet<>();

        Artifact dep1 = new Artifact("org.example", "dep1", "1.0.0", "compile", false);
        Artifact dep2 = new Artifact("org.example", "dep2", "2.0.0", "compile", true);

        nodes.add(dep1);
        nodes.add(dep2);

        edges.add(new Dependency(dep1, dep2, "compile", false));

        return new DependencyGraph(nodes, edges);
    }

    /**
     * Create a diamond graph: dep1 & dep2, both -> dep3
     * (The canvas creates its own root node)
     */
    public static DependencyGraph createDiamondGraph() {
        Set<Artifact> nodes = new HashSet<>();
        Set<Dependency> edges = new HashSet<>();

        Artifact dep1 = new Artifact("org.example", "dep1", "1.0.0", "compile", false);
        Artifact dep2 = new Artifact("org.example", "dep2", "1.0.0", "compile", false);
        Artifact dep3 = new Artifact("org.example", "dep3", "1.0.0", "compile", true);

        nodes.add(dep1);
        nodes.add(dep2);
        nodes.add(dep3);

        edges.add(new Dependency(dep1, dep3, "compile", false));
        edges.add(new Dependency(dep2, dep3, "compile", false));

        return new DependencyGraph(nodes, edges);
    }

    /**
     * Create a graph with multiple direct dependencies (no root node)
     * (The canvas creates its own root node)
     */
    public static DependencyGraph createMultipleDirectDepsGraph() {
        Set<Artifact> nodes = new HashSet<>();
        Set<Dependency> edges = new HashSet<>();

        Artifact dep1 = new Artifact("org.example", "dep1", "1.0.0", "compile", false);
        Artifact dep2 = new Artifact("org.example", "dep2", "2.0.0", "compile", false);
        Artifact dep3 = new Artifact("org.example", "dep3", "3.0.0", "compile", true);

        nodes.add(dep1);
        nodes.add(dep2);
        nodes.add(dep3);

        return new DependencyGraph(nodes, edges);
    }

    /**
     * Create an empty graph
     */
    public static DependencyGraph createEmptyGraph() {
        return new DependencyGraph();
    }

    /**
     * Create a graph with organisational dependencies (no root node)
     * (The canvas creates its own root node)
     */
    public static DependencyGraph createGraphWithOrgDeps() {
        Set<Artifact> nodes = new HashSet<>();
        Set<Dependency> edges = new HashSet<>();

        Artifact orgDep = new Artifact("com.myorg", "internal-lib", "1.0.0", "compile", false);
        Artifact externalDep = new Artifact("org.example", "external-lib", "1.0.0", "compile", false);

        nodes.add(orgDep);
        nodes.add(externalDep);

        return new DependencyGraph(nodes, edges);
    }
}
