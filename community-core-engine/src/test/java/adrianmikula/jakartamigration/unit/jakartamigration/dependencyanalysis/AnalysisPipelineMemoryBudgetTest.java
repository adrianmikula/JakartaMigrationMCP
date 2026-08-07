package unit.jakartamigration.dependencyanalysis;

import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Dependency;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.dependencyanalysis.service.ImprovedMavenCentralLookupService;
import adrianmikula.jakartamigration.dependencyanalysis.service.JakartaMappingService;
import adrianmikula.jakartamigration.dependencyanalysis.service.NamespaceClassifier;
import adrianmikula.jakartamigration.dependencyanalysis.service.impl.DependencyAnalysisModuleImpl;
import adrianmikula.jakartamigration.analysis.persistence.CentralMigrationAnalysisStore;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyGraphBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

/**
 * Memory budget tests for the dependency analysis pipeline.
 * Verifies that analyzing a realistic-sized project does not allocate
 * excessive memory from repeated collection copying or unbounded growth.
 *
 * <p>These tests build graphs with 500-1000 nodes (typical real-world size)
 * and assert that the analysis pipeline completes within a memory budget.
 * If defensive copies are reintroduced, the budget will be exceeded.</p>
 */
@Tag("slow")
@ExtendWith(MockitoExtension.class)
@DisplayName("Analysis pipeline memory budget tests")
class AnalysisPipelineMemoryBudgetTest {

    private MemoryMXBean memoryBean;

    @Mock
    private NamespaceClassifier namespaceClassifier;
    @Mock
    private JakartaMappingService jakartaMappingService;
    @Mock
    private ImprovedMavenCentralLookupService mavenCentralLookupService;
    @Mock
    private CentralMigrationAnalysisStore analysisStore;

    private DependencyAnalysisModuleImpl module;

    @BeforeEach
    void setUp() {
        memoryBean = ManagementFactory.getMemoryMXBean();

        lenient().when(namespaceClassifier.classify(org.mockito.ArgumentMatchers.any(Artifact.class)))
                .thenReturn(adrianmikula.jakartamigration.dependencyanalysis.domain.Namespace.UNKNOWN);
        lenient().when(jakartaMappingService.isJakartaCompatible(anyString(), anyString(), anyString()))
                .thenReturn(false);
        lenient().when(jakartaMappingService.hasMapping(anyString(), anyString()))
                .thenReturn(false);
        lenient().when(mavenCentralLookupService.findJakartaEquivalents(anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(List.of()));
        lenient().when(analysisStore.getUpgradeRecommendation(anyString(), anyString()))
                .thenReturn(null);
    }

    @Test
    @DisplayName("Analyzing a 500-node graph should use less than 50MB of heap")
    void analyze500NodeGraphShouldStayWithinBudget() {
        DependencyGraph graph = buildLargeGraph(500, 800);
        StubGraphBuilder builder = new StubGraphBuilder(graph);
        DependencyAnalysisModuleImpl module = createModule(builder);

        forceGC();
        long heapBefore = usedHeap();

        module.analyzeProject(Path.of("/tmp/test-project"));

        long heapAfter = usedHeap();
        long heapDelta = heapAfter - heapBefore;

        assertThat(heapDelta)
                .as("Heap increase for 500-node graph should be < 50MB, was %d bytes", heapDelta)
                .isLessThan(50L * 1024 * 1024);
    }

    @Test
    @DisplayName("Analyzing a 1000-node graph should use less than 100MB of heap")
    void analyze1000NodeGraphShouldStayWithinBudget() {
        DependencyGraph graph = buildLargeGraph(1000, 2000);
        StubGraphBuilder builder = new StubGraphBuilder(graph);
        DependencyAnalysisModuleImpl module = createModule(builder);

        forceGC();
        long heapBefore = usedHeap();

        module.analyzeProject(Path.of("/tmp/test-project"));

        long heapAfter = usedHeap();
        long heapDelta = heapAfter - heapBefore;

        assertThat(heapDelta)
                .as("Heap increase for 1000-node graph should be < 100MB, was %d bytes", heapDelta)
                .isLessThan(100L * 1024 * 1024);
    }

    @Test
    @DisplayName("Running analyzeProject twice should not grow heap proportionally")
    void repeatedAnalysisShouldNotLeak() {
        DependencyGraph graph = buildLargeGraph(500, 800);
        StubGraphBuilder builder = new StubGraphBuilder(graph);
        DependencyAnalysisModuleImpl module = createModule(builder);

        forceGC();
        long heapBefore = usedHeap();

        // Run 3 times
        for (int i = 0; i < 3; i++) {
            module.analyzeProject(Path.of("/tmp/test-project"));
        }

        forceGC();
        long heapAfter = usedHeap();
        long heapDelta = heapAfter - heapBefore;

        // Should not grow linearly with iterations
        assertThat(heapDelta)
                .as("3 analyses should not use more than 150MB total, was %d bytes", heapDelta)
                .isLessThan(150L * 1024 * 1024);
    }

    // --- Helpers ---

    private DependencyAnalysisModuleImpl createModule(DependencyGraphBuilder builder) {
        return new DependencyAnalysisModuleImpl(
                builder,
                namespaceClassifier,
                jakartaMappingService,
                mavenCentralLookupService,
                analysisStore);
    }

    private DependencyGraph buildLargeGraph(int nodeCount, int edgeCount) {
        DependencyGraph graph = new DependencyGraph();
        List<Artifact> artifacts = new ArrayList<>();

        for (int i = 0; i < nodeCount; i++) {
            String ns = i % 3 == 0 ? "javax" : "com.example";
            Artifact a = new Artifact(ns + ".lib" + (i / 3), "lib" + i, "1." + i, "compile", false);
            artifacts.add(a);
            graph.addNode(a);
        }

        for (int i = 0; i < edgeCount; i++) {
            Artifact from = artifacts.get(i % nodeCount);
            Artifact to = artifacts.get((i + 1) % nodeCount);
            if (!from.equals(to)) {
                graph.addEdge(new Dependency(from, to, "compile", false));
            }
        }

        return graph;
    }

    private void forceGC() {
        System.gc();
        try { Thread.sleep(50); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }

    private long usedHeap() {
        MemoryUsage heap = memoryBean.getHeapMemoryUsage();
        return heap.getUsed();
    }

    /**
     * Stub that returns a pre-built graph. Avoids filesystem scanning so the test
     * measures only the analysis pipeline, not file discovery.
     */
    private static class StubGraphBuilder implements DependencyGraphBuilder {
        private final DependencyGraph graph;

        StubGraphBuilder(DependencyGraph graph) {
            this.graph = graph;
        }

        @Override
        public DependencyGraph buildFromProject(Path projectPath) {
            return graph;
        }

        @Override
        public DependencyGraph buildFromMaven(Path pomXmlPath) {
            return graph;
        }

        @Override
        public DependencyGraph buildFromGradle(Path buildFilePath) {
            return graph;
        }
    }
}
