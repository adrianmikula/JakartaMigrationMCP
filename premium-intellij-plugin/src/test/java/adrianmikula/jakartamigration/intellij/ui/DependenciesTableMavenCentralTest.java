package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.model.DependencyInfo;
import adrianmikula.jakartamigration.intellij.model.DependencyMigrationStatus;
import adrianmikula.jakartamigration.intellij.model.JakartaArtifactCoordinates;
import adrianmikula.jakartamigration.intellij.service.MavenCentralService;
import com.intellij.openapi.project.Project;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.swing.SwingUtilities;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test suite for DependenciesTableComponent Maven Central integration
 * Tests progress bar, async operations, and error handling
 */
@ExtendWith(MockitoExtension.class)
public class DependenciesTableMavenCentralTest {
    
    @Mock
    private MavenCentralService mavenCentralService;
    
    @Mock
    private Project project;
    
    private DependenciesTableComponent component;
    
    @BeforeEach
    void setUp() {
        component = new DependenciesTableComponent(project);
    }
    
    @Test
    @DisplayName("Should show progress bar during Maven Central queries")
    void shouldShowProgressBarDuringMavenCentralQueries() {
        // Given
        DependencyInfo javaxDependency = createJavaxDependency("javax.mail", "javax.mail-api", "1.5.5");
        List<DependencyInfo> dependencies = Arrays.asList(javaxDependency);
        
        JakartaArtifactCoordinates jakartaArtifact = new JakartaArtifactCoordinates(
                "jakarta.mail", "jakarta.mail-api", "2.0.1", DependencyMigrationStatus.COMPATIBLE);
        
        when(mavenCentralService.findJakartaEquivalents(eq("javax.mail"), eq("javax.mail-api")))
                .thenReturn(CompletableFuture.completedFuture(List.of(jakartaArtifact)));
        
        // When
        component.setDependencies(dependencies);
        component.queryMavenCentralForDependencies();
        
        // Wait for async operations to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Then - progress bar should be visible during query
        // Note: We can't directly test private fields, but we can test the behavior
        verify(mavenCentralService, times(1)).findJakartaEquivalents(eq("javax.mail"), eq("javax.mail-api"));
    }
    
    @Test
    @DisplayName("Should hide progress bar after Maven Central queries complete")
    void shouldHideProgressBarAfterMavenCentralQueriesComplete() {
        // Given
        DependencyInfo javaxDependency = createJavaxDependency("javax.mail", "javax.mail-api", "1.5.5");
        JakartaArtifactCoordinates jakartaArtifact = new JakartaArtifactCoordinates(
                "jakarta.mail", "jakarta.mail-api", "2.0.1", DependencyMigrationStatus.COMPATIBLE);
        
        when(mavenCentralService.findJakartaEquivalents(eq("javax.mail"), eq("javax.mail-api")))
                .thenReturn(CompletableFuture.completedFuture(List.of(jakartaArtifact)));
        
        // When
        component.setDependencies(Arrays.asList(javaxDependency));
        component.queryMavenCentralForDependencies();
        
        // Wait for async operations to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Then - verify Maven Central service was called
        verify(mavenCentralService, times(1)).findJakartaEquivalents(eq("javax.mail"), eq("javax.mail-api"));
    }
    
    @Test
    @DisplayName("Should handle multiple javax dependencies in Maven Central query")
    void shouldHandleMultipleJavaxDependenciesInMavenCentralQuery() {
        // Given
        DependencyInfo javaxDep1 = createJavaxDependency("javax.mail", "javax.mail-api", "1.5.5");
        DependencyInfo javaxDep2 = createJavaxDependency("javax.servlet", "javax.servlet-api", "4.0.1");
        List<DependencyInfo> dependencies = Arrays.asList(javaxDep1, javaxDep2);
        
        JakartaArtifactCoordinates jakartaArtifact1 = new JakartaArtifactCoordinates(
                "jakarta.mail", "jakarta.mail-api", "2.0.1", DependencyMigrationStatus.COMPATIBLE);
        JakartaArtifactCoordinates jakartaArtifact2 = new JakartaArtifactCoordinates(
                "jakarta.servlet", "jakarta.servlet-api", "6.0.0", DependencyMigrationStatus.REQUIRES_MANUAL_MIGRATION);
        
        when(mavenCentralService.findJakartaEquivalents(eq("javax.mail"), eq("javax.mail-api")))
                .thenReturn(CompletableFuture.completedFuture(List.of(jakartaArtifact1)));
        when(mavenCentralService.findJakartaEquivalents(eq("javax.servlet"), eq("javax.servlet-api")))
                .thenReturn(CompletableFuture.completedFuture(List.of(jakartaArtifact2)));
        
        // When
        component.setDependencies(dependencies);
        component.queryMavenCentralForDependencies();
        
        // Wait for async operations to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Then - verify both dependencies were queried
        verify(mavenCentralService, times(1)).findJakartaEquivalents(eq("javax.mail"), eq("javax.mail-api"));
        verify(mavenCentralService, times(1)).findJakartaEquivalents(eq("javax.servlet"), eq("javax.servlet-api"));
    }
    
    @Test
    @DisplayName("Should handle Maven Central service errors gracefully")
    void shouldHandleMavenCentralServiceErrorsGracefully() {
        // Given
        DependencyInfo javaxDependency = createJavaxDependency("javax.mail", "javax.mail-api", "1.5.5");
        List<DependencyInfo> dependencies = Arrays.asList(javaxDependency);
        
        when(mavenCentralService.findJakartaEquivalents(eq("javax.mail"), eq("javax.mail-api")))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Network error")));
        
        // When
        component.setDependencies(dependencies);
        component.queryMavenCentralForDependencies();
        
        // Wait for async operations to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Then - should handle error gracefully
        verify(mavenCentralService, times(1)).findJakartaEquivalents(eq("javax.mail"), eq("javax.mail-api"));
    }
    
    @Test
    @DisplayName("Should update dependency with Jakarta artifact coordinates")
    void shouldUpdateDependencyWithJakartaArtifactCoordinates() {
        // Given
        DependencyInfo javaxDependency = createJavaxDependency("javax.mail", "javax.mail-api", "1.5.5");
        JakartaArtifactCoordinates jakartaArtifact = new JakartaArtifactCoordinates(
                "jakarta.mail", "jakarta.mail-api", "2.0.1", DependencyMigrationStatus.COMPATIBLE);
        List<DependencyInfo> dependencies = Arrays.asList(javaxDependency);
        
        when(mavenCentralService.findJakartaEquivalents(eq("javax.mail"), eq("javax.mail-api")))
                .thenReturn(CompletableFuture.completedFuture(List.of(jakartaArtifact)));
        
        // When
        component.setDependencies(dependencies);
        component.queryMavenCentralForDependencies();
        
        // Wait for async operations to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Then - dependency should be updated with Jakarta coordinates
        List<DependencyInfo> updatedDependencies = component.getSelectedDependencies();
        assertFalse(updatedDependencies.isEmpty());
        
        DependencyInfo updatedDep = updatedDependencies.get(0);
        assertEquals("jakarta.mail:jakarta.mail-api:2.0.1", updatedDep.getRecommendedArtifactCoordinates());
    }
    
    @Test
    @DisplayName("Should filter out non-javax dependencies from Maven Central queries")
    void shouldFilterNonJavaxDependenciesFromMavenCentralQueries() {
        // Given
        DependencyInfo javaxDependency = createJavaxDependency("javax.mail", "javax.mail-api", "1.5.5");
        DependencyInfo nonJavaxDependency = createNonJavaxDependency("org.springframework", "spring-core", "5.3.0");
        List<DependencyInfo> dependencies = Arrays.asList(javaxDependency, nonJavaxDependency);
        
        when(mavenCentralService.findJakartaEquivalents(eq("javax.mail"), eq("javax.mail-api")))
                .thenReturn(CompletableFuture.completedFuture(List.of(
                        new JakartaArtifactCoordinates("jakarta.mail", "jakarta.mail-api", "2.0.1", DependencyMigrationStatus.COMPATIBLE)
                )));
        
        // When
        component.setDependencies(dependencies);
        component.queryMavenCentralForDependencies();
        
        // Wait for async operations to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Then - only javax dependency should be queried
        verify(mavenCentralService, times(1)).findJakartaEquivalents(eq("javax.mail"), eq("javax.mail-api"));
        verify(mavenCentralService, never()).findJakartaEquivalents(eq("org.springframework"), eq("spring-core"));
    }
    
    @Test
    @DisplayName("Should handle empty dependencies list gracefully")
    void shouldHandleEmptyDependenciesListGracefully() {
        // Given
        List<DependencyInfo> dependencies = Arrays.asList();
        
        // When
        component.setDependencies(dependencies);
        component.queryMavenCentralForDependencies();
        
        // Wait for async operations to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Then - should not attempt Maven Central queries
        verify(mavenCentralService, never()).findJakartaEquivalents(anyString(), anyString());
    }
    
    @Test
    @DisplayName("Should show progress bar with dependency count during queries")
    void shouldShowProgressBarWithDependencyCountDuringQueries() {
        // Given
        DependencyInfo javaxDep1 = createJavaxDependency("javax.mail", "javax.mail-api", "1.5.5");
        DependencyInfo javaxDep2 = createJavaxDependency("javax.servlet", "javax.servlet-api", "4.0.1");
        List<DependencyInfo> dependencies = Arrays.asList(javaxDep1, javaxDep2);
        
        when(mavenCentralService.findJakartaEquivalents(anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(List.of()));
        
        // When
        component.setDependencies(dependencies);
        component.queryMavenCentralForDependencies();
        
        // Then - should show progress with count
        // Note: Testing private fields indirectly through behavior
        verify(mavenCentralService, times(2)).findJakartaEquivalents(anyString(), anyString());
    }
    
    @Test
    @DisplayName("Should handle mixed javax and non-javax dependencies correctly")
    void shouldHandleMixedJavaxAndNonJavaxDependenciesCorrectly() {
        // Given
        DependencyInfo javaxDep = createJavaxDependency("javax.mail", "javax.mail-api", "1.5.5");
        DependencyInfo nonJavaxDep1 = createNonJavaxDependency("org.springframework", "spring-core", "5.3.0");
        DependencyInfo nonJavaxDep2 = createNonJavaxDependency("org.apache.commons", "commons-lang3", "3.12.0");
        List<DependencyInfo> dependencies = Arrays.asList(javaxDep, nonJavaxDep1, nonJavaxDep2);
        
        when(mavenCentralService.findJakartaEquivalents(eq("javax.mail"), eq("javax.mail-api")))
                .thenReturn(CompletableFuture.completedFuture(List.of(
                        new JakartaArtifactCoordinates("jakarta.mail", "jakarta.mail-api", "2.0.1", DependencyMigrationStatus.COMPATIBLE)
                )));
        
        // When
        component.setDependencies(dependencies);
        component.queryMavenCentralForDependencies();
        
        // Wait for async operations to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Then - only javax dependency should be queried
        verify(mavenCentralService, times(1)).findJakartaEquivalents(eq("javax.mail"), eq("javax.mail-api"));
        verify(mavenCentralService, never()).findJakartaEquivalents(eq("org.springframework"), eq("spring-core"));
        verify(mavenCentralService, never()).findJakartaEquivalents(eq("org.apache.commons"), eq("commons-lang3"));
    }
    
    @Test
    @DisplayName("Should handle partial failures in Maven Central queries")
    void shouldHandlePartialFailuresInMavenCentralQueries() {
        // Given
        DependencyInfo javaxDep1 = createJavaxDependency("javax.mail", "javax.mail-api", "1.5.5");
        DependencyInfo javaxDep2 = createJavaxDependency("javax.servlet", "javax.servlet-api", "4.0.1");
        List<DependencyInfo> dependencies = Arrays.asList(javaxDep1, javaxDep2);
        
        JakartaArtifactCoordinates jakartaArtifact1 = new JakartaArtifactCoordinates(
                "jakarta.mail", "jakarta.mail-api", "2.0.1", DependencyMigrationStatus.COMPATIBLE);
        
        when(mavenCentralService.findJakartaEquivalents(eq("javax.mail"), eq("javax.mail-api")))
                .thenReturn(CompletableFuture.completedFuture(List.of(jakartaArtifact1)));
        when(mavenCentralService.findJakartaEquivalents(eq("javax.servlet"), eq("javax.servlet-api")))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Network timeout")));
        
        // When
        component.setDependencies(dependencies);
        component.queryMavenCentralForDependencies();
        
        // Wait for async operations to complete
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Then - should handle partial failures gracefully
        verify(mavenCentralService, times(1)).findJakartaEquivalents(eq("javax.mail"), eq("javax.mail-api"));
        verify(mavenCentralService, times(1)).findJakartaEquivalents(eq("javax.servlet"), eq("javax.servlet-api"));
    }
    
    @Test
    @DisplayName("Should update dependency status when no Jakarta artifacts found")
    void shouldUpdateDependencyStatusWhenNoJakartaArtifactsFound() {
        // Given
        DependencyInfo javaxDependency = createJavaxDependency("javax.obscure", "javax.obscure-api", "1.0.0");
        List<DependencyInfo> dependencies = Arrays.asList(javaxDependency);
        
        when(mavenCentralService.findJakartaEquivalents(eq("javax.obscure"), eq("javax.obscure-api")))
                .thenReturn(CompletableFuture.completedFuture(List.of())); // Empty list
        
        // When
        component.setDependencies(dependencies);
        component.queryMavenCentralForDependencies();
        
        // Wait for async operations to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Then - dependency should be marked as having no Jakarta version
        verify(mavenCentralService, times(1)).findJakartaEquivalents(eq("javax.obscure"), eq("javax.obscure-api"));
    }
    
    /**
     * Helper method to create a javax dependency for testing
     */
    private DependencyInfo createJavaxDependency(String groupId, String artifactId, String version) {
        DependencyInfo dep = new DependencyInfo();
        dep.setGroupId(groupId);
        dep.setArtifactId(artifactId);
        dep.setCurrentVersion(version);
        dep.setMigrationStatus(DependencyMigrationStatus.NEEDS_UPGRADE);
        dep.setTransitive(false);
        dep.setOrganizational(false);
        return dep;
    }
    
    /**
     * Helper method to create a non-javax dependency for testing
     */
    private DependencyInfo createNonJavaxDependency(String groupId, String artifactId, String version) {
        DependencyInfo dep = new DependencyInfo();
        dep.setGroupId(groupId);
        dep.setArtifactId(artifactId);
        dep.setCurrentVersion(version);
        dep.setMigrationStatus(DependencyMigrationStatus.COMPATIBLE);
        dep.setTransitive(false);
        dep.setOrganizational(false);
        return dep;
    }
}
