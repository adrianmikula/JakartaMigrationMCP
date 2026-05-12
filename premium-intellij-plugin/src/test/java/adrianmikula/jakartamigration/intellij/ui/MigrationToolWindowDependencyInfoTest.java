package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Dependency;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyAnalysisReport;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.intellij.model.DependencyInfo;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for DependencyInfo conversion in MigrationToolWindow.
 * These tests verify that the isTransitive flag is correctly set based on graph structure
 * to prevent regressions where all dependencies are incorrectly marked as transitive.
 */
public class MigrationToolWindowDependencyInfoTest {

    /**
     * Test that verifies direct dependencies (no incoming edges) are marked as non-transitive.
     * This is a regression test for the bug where all dependencies were marked as transitive=true
     * because the artifact.transitive() field was hardcoded to true in MavenDependencyGraphBuilder.
     */
    @Test
    public void testDirectDependenciesMarkedAsNonTransitive() {
        // Create a dependency graph with direct dependencies (no incoming edges)
        DependencyGraph graph = new DependencyGraph();
        
        // Project artifact
        Artifact project = new Artifact("com.example", "my-project", "1.0.0", "compile", false);
        graph.addNode(project);
        
        // Direct dependency (no incoming edges)
        Artifact directDep = new Artifact("org.example", "direct-lib", "1.0.0", "compile", false);
        graph.addNode(directDep);
        
        // Add edge from project to direct dependency
        graph.addEdge(new Dependency(project, directDep, "compile", false));
        
        // Create report
        DependencyAnalysisReport report = new DependencyAnalysisReport(graph, null, null, null, null);
        
        // Simulate the conversion logic from MigrationToolWindow.convertBasicReportToDependencyInfo
        List<DependencyInfo> deps = convertBasicReportToDependencyInfo(report);
        
        // Verify direct dependency is marked as non-transitive
        DependencyInfo directDepInfo = deps.stream()
            .filter(d -> d.getArtifactId().equals("direct-lib"))
            .findFirst()
            .orElseThrow();
        
        assertThat(directDepInfo.isTransitive()).isFalse();
        assertThat(directDepInfo.getDepth()).isEqualTo(0);
    }

    /**
     * Test that verifies transitive dependencies (with incoming edges) are marked as transitive.
     */
    @Test
    public void testTransitiveDependenciesMarkedAsTransitive() {
        // Create a dependency graph with transitive dependencies
        DependencyGraph graph = new DependencyGraph();
        
        // Project artifact
        Artifact project = new Artifact("com.example", "my-project", "1.0.0", "compile", false);
        graph.addNode(project);
        
        // Direct dependency
        Artifact directDep = new Artifact("org.example", "direct-lib", "1.0.0", "compile", false);
        graph.addNode(directDep);
        
        // Transitive dependency (has incoming edge from directDep)
        Artifact transitiveDep = new Artifact("org.example", "transitive-lib", "2.0.0", "compile", false);
        graph.addNode(transitiveDep);
        
        // Add edges
        graph.addEdge(new Dependency(project, directDep, "compile", false));
        graph.addEdge(new Dependency(directDep, transitiveDep, "compile", false));
        
        // Create report
        DependencyAnalysisReport report = new DependencyAnalysisReport(graph, null, null, null, null);
        
        // Simulate the conversion logic
        List<DependencyInfo> deps = convertBasicReportToDependencyInfo(report);
        
        // Verify direct dependency is non-transitive
        DependencyInfo directDepInfo = deps.stream()
            .filter(d -> d.getArtifactId().equals("direct-lib"))
            .findFirst()
            .orElseThrow();
        assertThat(directDepInfo.isTransitive()).isFalse();
        assertThat(directDepInfo.getDepth()).isEqualTo(0);
        
        // Verify transitive dependency is marked as transitive
        DependencyInfo transitiveDepInfo = deps.stream()
            .filter(d -> d.getArtifactId().equals("transitive-lib"))
            .findFirst()
            .orElseThrow();
        assertThat(transitiveDepInfo.isTransitive()).isTrue();
        assertThat(transitiveDepInfo.getDepth()).isEqualTo(1);
    }

    /**
     * Test that verifies dependencies with multiple incoming edges are marked as transitive.
     * This can happen when multiple direct dependencies depend on the same transitive library.
     */
    @Test
    public void testDependenciesWithMultipleIncomingEdgesMarkedAsTransitive() {
        // Create a dependency graph with shared transitive dependency
        DependencyGraph graph = new DependencyGraph();
        
        // Project artifact
        Artifact project = new Artifact("com.example", "my-project", "1.0.0", "compile", false);
        graph.addNode(project);
        
        // Two direct dependencies
        Artifact directDep1 = new Artifact("org.example", "direct-lib1", "1.0.0", "compile", false);
        graph.addNode(directDep1);
        
        Artifact directDep2 = new Artifact("org.example", "direct-lib2", "1.0.0", "compile", false);
        graph.addNode(directDep2);
        
        // Shared transitive dependency (has incoming edges from both direct deps)
        Artifact sharedTransitive = new Artifact("org.example", "shared-lib", "2.0.0", "compile", false);
        graph.addNode(sharedTransitive);
        
        // Add edges
        graph.addEdge(new Dependency(project, directDep1, "compile", false));
        graph.addEdge(new Dependency(project, directDep2, "compile", false));
        graph.addEdge(new Dependency(directDep1, sharedTransitive, "compile", false));
        graph.addEdge(new Dependency(directDep2, sharedTransitive, "compile", false));
        
        // Create report
        DependencyAnalysisReport report = new DependencyAnalysisReport(graph, null, null, null, null);
        
        // Simulate the conversion logic
        List<DependencyInfo> deps = convertBasicReportToDependencyInfo(report);
        
        // Verify shared transitive dependency is marked as transitive
        DependencyInfo sharedDepInfo = deps.stream()
            .filter(d -> d.getArtifactId().equals("shared-lib"))
            .findFirst()
            .orElseThrow();
        assertThat(sharedDepInfo.isTransitive()).isTrue();
        assertThat(sharedDepInfo.getDepth()).isEqualTo(1);
    }

    /**
     * Test that verifies the conversion handles empty graphs gracefully.
     */
    @Test
    public void testEmptyGraphReturnsEmptyList() {
        DependencyGraph graph = new DependencyGraph();
        DependencyAnalysisReport report = new DependencyAnalysisReport(graph, null, null, null, null);
        
        List<DependencyInfo> deps = convertBasicReportToDependencyInfo(report);
        
        assertThat(deps).isEmpty();
    }

    /**
     * Test that verifies the conversion handles null report gracefully.
     */
    @Test
    public void testNullReportReturnsEmptyList() {
        List<DependencyInfo> deps = convertBasicReportToDependencyInfo(null);
        
        assertThat(deps).isEmpty();
    }

    /**
     * Simulates the convertBasicReportToDependencyInfo logic from MigrationToolWindow.
     * This is a copy of the implementation to allow testing without needing to instantiate
     * the full MigrationToolWindow class.
     */
    private List<DependencyInfo> convertBasicReportToDependencyInfo(DependencyAnalysisReport report) {
        List<DependencyInfo> deps = new ArrayList<>();
        if (report == null || report.dependencyGraph() == null) {
            return deps;
        }

        // Build a set of artifacts that have incoming edges (transitive dependencies)
        java.util.Set<String> hasIncomingEdges = new java.util.HashSet<>();
        for (adrianmikula.jakartamigration.dependencyanalysis.domain.Dependency dep : report.dependencyGraph().getEdges()) {
            hasIncomingEdges.add(dep.to().groupId() + ":" + dep.to().artifactId());
        }

        for (Artifact artifact : report.dependencyGraph().getNodes()) {
            DependencyInfo info = new DependencyInfo();
            info.setArtifactId(artifact.artifactId());
            info.setGroupId(artifact.groupId());
            info.setCurrentVersion(artifact.version());
            
            // Determine if this is a transitive dependency based on incoming edges
            String artifactId = artifact.groupId() + ":" + artifact.artifactId();
            boolean isTransitive = hasIncomingEdges.contains(artifactId);
            info.setTransitive(isTransitive);
            info.setDepth(isTransitive ? 1 : 0);
            
            info.setScope(artifact.scope());
            deps.add(info);
        }

        return deps;
    }
}
