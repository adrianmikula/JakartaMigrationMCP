package unit.jakartamigration.dependencyanalysis.service;

import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.dependencyanalysis.domain.BinaryCompatibilityReport;
import adrianmikula.jakartamigration.dependencyanalysis.domain.BreakingChange;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Blocker;
import adrianmikula.jakartamigration.dependencyanalysis.domain.BlockerType;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.dependencyanalysis.service.JakartaMappingService;
import adrianmikula.jakartamigration.dependencyanalysis.service.JapicmpCompatibilityChecker;
import adrianmikula.jakartamigration.dependencyanalysis.service.JarResolver;
import adrianmikula.jakartamigration.dependencyanalysis.service.impl.DependencyAnalysisModuleImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Integration tests for binary compatibility checking in detectBlockers.
 * 
 * Tests the integration between DependencyAnalysisModuleImpl, JapicmpCompatibilityChecker,
 * and JarResolver to ensure binary incompatibilities are detected correctly.
 */
@DisplayName("Binary Compatibility Integration Tests")
@ExtendWith(MockitoExtension.class)
class BinaryCompatibilityIntegrationTest {
    
    @Mock
    private adrianmikula.jakartamigration.dependencyanalysis.service.DependencyGraphBuilder dependencyGraphBuilder;
    
    @Mock
    private adrianmikula.jakartamigration.dependencyanalysis.service.NamespaceClassifier namespaceClassifier;
    
    @Mock
    private JakartaMappingService jakartaMappingService;
    
    @Mock
    private JapicmpCompatibilityChecker japicmpChecker;
    
    @Mock
    private JarResolver jarResolver;
    
    private DependencyAnalysisModuleImpl module;
    
    @BeforeEach
    void setUp() {
        module = new DependencyAnalysisModuleImpl(
            dependencyGraphBuilder,
            namespaceClassifier,
            jakartaMappingService,
            japicmpChecker,
            jarResolver
        );
    }
    
    @Test
    @DisplayName("Should not add blocker when JARs are not available")
    void shouldNotAddBlockerWhenJarsNotAvailable() {
        // Given
        Artifact javaxArtifact = new Artifact(
            "javax.servlet",
            "javax.servlet-api",
            "4.0.1",
            "compile",
            false
        );
        
        DependencyGraph graph = new DependencyGraph(Set.of(javaxArtifact), Set.of());
        
        when(jakartaMappingService.findMapping(any(Artifact.class)))
            .thenReturn(Optional.of(new JakartaMappingService.JakartaEquivalent(
                "jakarta.servlet",
                "jakarta.servlet-api",
                "6.0.0",
                JakartaMappingService.CompatibilityLevel.DROP_IN_REPLACEMENT
            )));
        
        when(jarResolver.jarExists(any(Artifact.class))).thenReturn(false);
        
        // When
        List<Blocker> blockers = module.detectBlockers(graph);
        
        // Then
        // Should not add BINARY_INCOMPATIBLE blocker if JARs aren't available
        assertThat(blockers).noneMatch(b -> b.type() == BlockerType.BINARY_INCOMPATIBLE);
    }
    
    @Test
    @DisplayName("Should not add blocker when no Jakarta mapping exists")
    void shouldNotAddBlockerWhenNoMapping() {
        // Given
        Artifact javaxArtifact = new Artifact(
            "javax.servlet",
            "javax.servlet-api",
            "4.0.1",
            "compile",
            false
        );
        
        DependencyGraph graph = new DependencyGraph(Set.of(javaxArtifact), Set.of());
        
        when(jakartaMappingService.findMapping(any(Artifact.class)))
            .thenReturn(Optional.empty());
        
        // When
        List<Blocker> blockers = module.detectBlockers(graph);
        
        // Then
        // Should not add BINARY_INCOMPATIBLE blocker if no mapping exists
        assertThat(blockers).noneMatch(b -> b.type() == BlockerType.BINARY_INCOMPATIBLE);
    }
    
    @Test
    @DisplayName("Should add BINARY_INCOMPATIBLE blocker when breaking changes detected")
    void shouldAddBinaryIncompatibleBlockerWhenBreakingChangesDetected() {
        // Given
        Artifact javaxArtifact = new Artifact(
            "javax.servlet",
            "javax.servlet-api",
            "4.0.1",
            "compile",
            false
        );
        
        Artifact jakartaArtifact = new Artifact(
            "jakarta.servlet",
            "jakarta.servlet-api",
            "6.0.0",
            "compile",
            false
        );
        
        DependencyGraph graph = new DependencyGraph(Set.of(javaxArtifact), Set.of());
        
        when(jakartaMappingService.findMapping(any(Artifact.class)))
            .thenReturn(Optional.of(new JakartaMappingService.JakartaEquivalent(
                "jakarta.servlet",
                "jakarta.servlet-api",
                "6.0.0",
                JakartaMappingService.CompatibilityLevel.DROP_IN_REPLACEMENT
            )));
        
        when(jarResolver.jarExists(any(Artifact.class))).thenReturn(true);
        
        // Mock breaking changes
        BreakingChange breakingChange = new BreakingChange(
            BreakingChange.BreakingChangeType.METHOD_REMOVED,
            "javax.servlet.Servlet",
            "getServletInfo",
            "Method was removed: javax.servlet.Servlet.getServletInfo()"
        );
        
        BinaryCompatibilityReport incompatibleReport = BinaryCompatibilityReport.incompatible(
            javaxArtifact,
            jakartaArtifact,
            List.of(breakingChange)
        );
        
        when(japicmpChecker.compareVersions(javaxArtifact, jakartaArtifact))
            .thenReturn(incompatibleReport);
        
        // When
        List<Blocker> blockers = module.detectBlockers(graph);
        
        // Then
        assertThat(blockers).anyMatch(b -> 
            b.type() == BlockerType.BINARY_INCOMPATIBLE &&
            b.artifact().equals(javaxArtifact) &&
            b.reason().contains("Binary incompatibility detected")
        );
    }
    
    @Test
    @DisplayName("Should not add blocker when versions are compatible")
    void shouldNotAddBlockerWhenCompatible() {
        // Given
        Artifact javaxArtifact = new Artifact(
            "javax.servlet",
            "javax.servlet-api",
            "4.0.1",
            "compile",
            false
        );
        
        DependencyGraph graph = new DependencyGraph(Set.of(javaxArtifact), Set.of());
        
        when(jakartaMappingService.findMapping(any(Artifact.class)))
            .thenReturn(Optional.of(new JakartaMappingService.JakartaEquivalent(
                "jakarta.servlet",
                "jakarta.servlet-api",
                "6.0.0",
                JakartaMappingService.CompatibilityLevel.DROP_IN_REPLACEMENT
            )));
        
        when(jarResolver.jarExists(any(Artifact.class))).thenReturn(true);
        
        // Mock compatible report
        BinaryCompatibilityReport compatibleReport = BinaryCompatibilityReport.compatible(
            javaxArtifact,
            new Artifact("jakarta.servlet", "jakarta.servlet-api", "6.0.0", "compile", false)
        );
        
        when(japicmpChecker.compareVersions(any(Artifact.class), any(Artifact.class)))
            .thenReturn(compatibleReport);
        
        // When
        List<Blocker> blockers = module.detectBlockers(graph);
        
        // Then
        assertThat(blockers).noneMatch(b -> b.type() == BlockerType.BINARY_INCOMPATIBLE);
    }
    
    @Test
    @DisplayName("Should handle japicmp errors gracefully")
    void shouldHandleJapicmpErrorsGracefully() {
        // Given
        Artifact javaxArtifact = new Artifact(
            "javax.servlet",
            "javax.servlet-api",
            "4.0.1",
            "compile",
            false
        );
        
        DependencyGraph graph = new DependencyGraph(Set.of(javaxArtifact), Set.of());
        
        when(jakartaMappingService.findMapping(any(Artifact.class)))
            .thenReturn(Optional.of(new JakartaMappingService.JakartaEquivalent(
                "jakarta.servlet",
                "jakarta.servlet-api",
                "6.0.0",
                JakartaMappingService.CompatibilityLevel.DROP_IN_REPLACEMENT
            )));
        
        when(jarResolver.jarExists(any(Artifact.class))).thenReturn(true);
        
        // Mock japicmp throwing exception
        when(japicmpChecker.compareVersions(any(Artifact.class), any(Artifact.class)))
            .thenThrow(new RuntimeException("japicmp error"));
        
        // When
        List<Blocker> blockers = module.detectBlockers(graph);
        
        // Then
        // Should not add blocker on error (don't want false positives)
        assertThat(blockers).noneMatch(b -> b.type() == BlockerType.BINARY_INCOMPATIBLE);
    }
}

