package unit.jakartamigration.dependencyanalysis.service;

import adrianmikula.jakartamigration.analysis.persistence.CentralMigrationAnalysisStore;
import adrianmikula.jakartamigration.dependencyanalysis.domain.*;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyAnalysisModule;
import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyGraphBuilder;
import adrianmikula.jakartamigration.dependencyanalysis.service.ImprovedMavenCentralLookupService;
import adrianmikula.jakartamigration.dependencyanalysis.service.JakartaMappingService;
import adrianmikula.jakartamigration.dependencyanalysis.service.NamespaceClassifier;
import adrianmikula.jakartamigration.dependencyanalysis.service.impl.DependencyAnalysisModuleImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("slow")  // Integration test with heavy dependency analysis
@DisplayName("Binary Compatibility Integration Tests")
class BinaryCompatibilityIntegrationTest {

    @Mock
    private DependencyGraphBuilder dependencyGraphBuilder;

    @Mock
    private NamespaceClassifier namespaceClassifier;

    @Mock
    private JakartaMappingService jakartaMappingService;

    @Mock
    private ImprovedMavenCentralLookupService mavenCentralLookupService;

    private DependencyAnalysisModule module;
    private CentralMigrationAnalysisStore analysisStore;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        analysisStore = new CentralMigrationAnalysisStore(tempDir.resolve("test.db"));
        module = new DependencyAnalysisModuleImpl(
                dependencyGraphBuilder,
                namespaceClassifier,
                jakartaMappingService,
                mavenCentralLookupService,
                analysisStore);
    }

    @Test
    @DisplayName("Should not add binary incompatible blocker when no breaking changes detected")
    void shouldNotAddBinaryIncompatibleBlockerWhenNoBreakingChangesDetected() {
        // Given
        DependencyGraph graph = new DependencyGraph();
        Artifact javaxArtifact = new Artifact("javax.servlet", "javax.servlet-api", "4.0.1", "compile", false);
        graph.addNode(javaxArtifact);

        lenient().when(dependencyGraphBuilder.buildFromProject(any())).thenReturn(graph);
        // FIX: Stub classify() to return Namespace.JAVAX so the binary compatibility
        // check runs
        lenient().when(namespaceClassifier.classify(any(Artifact.class))).thenReturn(Namespace.JAVAX);
        lenient().when(jakartaMappingService.hasMapping(anyString(), anyString())).thenReturn(true);
        lenient().when(jakartaMappingService.isJakartaCompatible(anyString(), anyString(), anyString()))
                .thenReturn(false);

        // When
        List<Blocker> blockers = module.detectBlockers(graph);

        // Then
        assertThat(blockers).isEmpty();
        verify(namespaceClassifier, atLeastOnce()).classify(any(Artifact.class));
    }

    @Test
    @DisplayName("Should add NO_JAKARTA_EQUIVALENT blocker when no mapping exists")
    void shouldAddNoJakartaEquivalentBlockerWhenNoMappingExists() {
        // Given
        DependencyGraph graph = new DependencyGraph();
        Artifact javaxArtifact = new Artifact("com.unknown", "unknown-lib", "1.0.0", "compile", false);
        graph.addNode(javaxArtifact);

        lenient().when(dependencyGraphBuilder.buildFromProject(any())).thenReturn(graph);
        lenient().when(namespaceClassifier.classify(any(Artifact.class))).thenReturn(Namespace.JAVAX);
        lenient().when(jakartaMappingService.hasMapping(anyString(), anyString())).thenReturn(false);
        lenient().when(jakartaMappingService.isJakartaCompatible(anyString(), anyString(), anyString()))
                .thenReturn(false);
        lenient().when(mavenCentralLookupService.findJakartaEquivalents(anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(List.of()));

        // When
        List<Blocker> blockers = module.detectBlockers(graph);

        // Then
        assertThat(blockers).hasSize(1);
        assertThat(blockers.get(0).type()).isEqualTo(BlockerType.NO_JAKARTA_EQUIVALENT);
        assertThat(blockers.get(0).artifact()).isEqualTo(javaxArtifact);
        verify(namespaceClassifier, atLeastOnce()).classify(any(Artifact.class));
    }
}
