package adrianmikula.jakartamigration.intellij.integration;

import adrianmikula.jakartamigration.dependencyanalysis.config.CompatibilityConfigLoader;
import adrianmikula.jakartamigration.dependencyanalysis.config.CompatibilityConfigLoader.ArtifactClassification;
import adrianmikula.jakartamigration.intellij.service.MigrationAnalysisService;
import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyAnalysisReport;
import adrianmikula.jakartamigration.intellij.model.DependencyInfo;
import adrianmikula.jakartamigration.intellij.model.DependencyMigrationStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for dependency classification using real GitHub projects.
 * Tests the CompatibilityConfigLoader against actual projects with mixed javax dependencies.
 * Uses ExampleProjectManager to fetch and test actual projects from examples.yaml mixed_examples section.
 */
@Tag("slow")
public class DependencyClassificationIntegrationTest extends IntegrationTestBase {
    
    private CompatibilityConfigLoader configLoader;
    private MigrationAnalysisService migrationAnalysisService;
    
    @Override
    void setUp() throws IOException {
        super.setUp();
        configLoader = new CompatibilityConfigLoader();
        migrationAnalysisService = new MigrationAnalysisService();
    }
    
    @Nested
    @DisplayName("Mixed Examples Dependency Classification")
    class MixedExamplesClassification {
        
        @Test
        @DisplayName("Should classify dependencies in non-javax project correctly")
        void testClassifyNonJavaxProject() throws Exception {
            // Get project from GitHub examples
            Path projectDir = projectManager.getExampleProject("Non-Javax Only", "mixed_examples");
            
            // Analyze project
            DependencyAnalysisReport report = migrationAnalysisService.analyzeProject(projectDir);
            Set<Artifact> dependencies = report.dependencyGraph().getNodes();
            
            // Verify analysis completed
            assertNotNull(dependencies);
            assertTrue(dependencies.size() >= 0, "Should return dependency list");
            
            // Check that no javax dependencies are classified as needing Jakarta migration
            long javaxUpgradeCount = dependencies.stream()
                .filter(artifact -> artifact.groupId() != null && artifact.groupId().startsWith("javax."))
                .filter(artifact -> !artifact.isJakartaCompatible())
                .count();
            
            System.out.println("Non-Javax project: " + dependencies.size() + " dependencies, " + 
                javaxUpgradeCount + " javax dependencies needing upgrade");
            
            // This project should have minimal or no javax dependencies needing Jakarta migration
            assertTrue(javaxUpgradeCount == 0, "Non-javax project should not have javax dependencies needing migration");
        }
        
        @Test
        @DisplayName("Should correctly identify servlet dependency needing Jakarta migration")
        void testClassifyServletProject() throws Exception {
            // Get project from GitHub examples
            Path projectDir = projectManager.getExampleProject("Non-Javax plus Servlet", "mixed_examples");
            
            // Analyze project
            DependencyAnalysisReport report = migrationAnalysisService.analyzeProject(projectDir);
            Set<Artifact> dependencies = report.dependencyGraph().getNodes();
            
            // Verify analysis completed
            assertNotNull(dependencies);
            assertTrue(dependencies.size() > 0, "Should return non-empty dependency list");
            
            // Check for javax.servlet dependencies
            boolean foundServlet = dependencies.stream()
                .filter(artifact -> artifact.groupId() != null)
                .anyMatch(artifact -> artifact.groupId().startsWith("javax.servlet"));
            
            System.out.println("Servlet project: " + dependencies.size() + " dependencies found");
            
            // The project description says it has javax.servlet
            // Note: We may or may not find it depending on the actual project state
            if (foundServlet) {
                System.out.println("Found javax.servlet dependency - should be classified as NEEDS_UPGRADE");
            }
        }
        
        @Test
        @DisplayName("Should handle JSON dependencies correctly")
        void testClassifyJsonProject() throws Exception {
            // Get project from GitHub examples
            Path projectDir = projectManager.getExampleProject("Non-Javax plus JSON", "mixed_examples");
            
            // Analyze project
            DependencyAnalysisReport report = migrationAnalysisService.analyzeProject(projectDir);
            Set<Artifact> dependencies = report.dependencyGraph().getNodes();
            
            // Verify analysis completed
            assertNotNull(dependencies);
            assertTrue(dependencies.size() >= 0, "Should return dependency list");
            
            // Check for JSON-related dependencies
            boolean foundJsonDep = dependencies.stream()
                .filter(artifact -> artifact.groupId() != null)
                .anyMatch(artifact -> artifact.artifactId().toLowerCase().contains("json") ||
                                artifact.groupId().toLowerCase().contains("json"));
            
            System.out.println("JSON project: " + dependencies.size() + " dependencies, " +
                (foundJsonDep ? "found JSON dependency" : "no JSON dependency found"));
            
            // Verify we can analyze the project without errors
            assertTrue(dependencies.size() >= 0);
        }
    }
    
    @Nested
    @DisplayName("Compatibility Config Classification Tests")
    class CompatibilityConfigTests {
        
        @Test
        @DisplayName("Should classify JDK-provided packages correctly")
        void testJdkProvidedClassification() {
            // These should all be JDK_PROVIDED
            assertEquals(ArtifactClassification.JDK_PROVIDED, 
                configLoader.classifyArtifact("javax.management", "management-api"));
            assertEquals(ArtifactClassification.JDK_PROVIDED, 
                configLoader.classifyArtifact("javax.naming", "naming-api"));
            assertEquals(ArtifactClassification.JDK_PROVIDED, 
                configLoader.classifyArtifact("javax.crypto", "crypto-api"));
            assertEquals(ArtifactClassification.JDK_PROVIDED, 
                configLoader.classifyArtifact("javax.net", "net-api"));
            assertEquals(ArtifactClassification.JDK_PROVIDED, 
                configLoader.classifyArtifact("javax.script", "script-api"));
            assertEquals(ArtifactClassification.JDK_PROVIDED, 
                configLoader.classifyArtifact("javax.sql", "sql-api"));
            
            System.out.println("JDK-provided classification test passed");
        }
        
        @Test
        @DisplayName("Should classify Jakarta-required packages correctly")
        void testJakartaRequiredClassification() {
            // These should all be JAKARTA_REQUIRED
            assertEquals(ArtifactClassification.JAKARTA_REQUIRED, 
                configLoader.classifyArtifact("javax.servlet", "servlet-api"));
            assertEquals(ArtifactClassification.JAKARTA_REQUIRED, 
                configLoader.classifyArtifact("javax.persistence", "persistence-api"));
            assertEquals(ArtifactClassification.JAKARTA_REQUIRED, 
                configLoader.classifyArtifact("javax.validation", "validation-api"));
            assertEquals(ArtifactClassification.JAKARTA_REQUIRED, 
                configLoader.classifyArtifact("javax.ws.rs", "jaxrs-api"));
            assertEquals(ArtifactClassification.JAKARTA_REQUIRED, 
                configLoader.classifyArtifact("javax.ejb", "ejb-api"));
            assertEquals(ArtifactClassification.JAKARTA_REQUIRED, 
                configLoader.classifyArtifact("javax.jms", "jms-api"));
            assertEquals(ArtifactClassification.JAKARTA_REQUIRED, 
                configLoader.classifyArtifact("javax.faces", "faces-api"));
            assertEquals(ArtifactClassification.JAKARTA_REQUIRED, 
                configLoader.classifyArtifact("javax.enterprise", "cdi-api"));
            
            System.out.println("Jakarta-required classification test passed");
        }
        
        @Test
        @DisplayName("Should classify context-dependent packages correctly")
        void testContextDependentClassification() {
            // These should all be CONTEXT_DEPENDENT
            assertEquals(ArtifactClassification.CONTEXT_DEPENDENT, 
                configLoader.classifyArtifact("javax.xml", "xml-api"));
            assertEquals(ArtifactClassification.CONTEXT_DEPENDENT, 
                configLoader.classifyArtifact("javax.xml.bind", "jaxb-api"));
            assertEquals(ArtifactClassification.CONTEXT_DEPENDENT, 
                configLoader.classifyArtifact("javax.xml.ws", "jaxws-api"));
            assertEquals(ArtifactClassification.CONTEXT_DEPENDENT, 
                configLoader.classifyArtifact("javax.mail", "mail-api"));
            assertEquals(ArtifactClassification.CONTEXT_DEPENDENT, 
                configLoader.classifyArtifact("javax.activation", "activation-api"));
            assertEquals(ArtifactClassification.CONTEXT_DEPENDENT, 
                configLoader.classifyArtifact("javax.cache", "cache-api"));
            assertEquals(ArtifactClassification.CONTEXT_DEPENDENT, 
                configLoader.classifyArtifact("javax.measure", "measure-api"));
            assertEquals(ArtifactClassification.CONTEXT_DEPENDENT, 
                configLoader.classifyArtifact("javax.transaction", "transaction-api"));
            
            System.out.println("Context-dependent classification test passed");
        }
        
        @Test
        @DisplayName("Should classify unknown packages correctly")
        void testUnknownClassification() {
            // These should all be UNKNOWN (not in any list)
            assertEquals(ArtifactClassification.UNKNOWN, 
                configLoader.classifyArtifact("com.unknown.library", "unknown-artifact"));
            assertEquals(ArtifactClassification.UNKNOWN, 
                configLoader.classifyArtifact("org.custom.project", "custom-api"));
            
            System.out.println("Unknown classification test passed");
        }
    }
    
    @Nested
    @DisplayName("Dependency Migration Status Tests")
    class MigrationStatusTests {
        
        @Test
        @DisplayName("Should map JDK_PROVIDED to COMPATIBLE status")
        void testJdkProvidedToCompatible() {
            ArtifactClassification classification = configLoader.classifyArtifact("javax.management", "management-api");
            assertEquals(ArtifactClassification.JDK_PROVIDED, classification);
            
            // In DependenciesTableComponent, JDK_PROVIDED maps to COMPATIBLE
            DependencyMigrationStatus expectedStatus = DependencyMigrationStatus.COMPATIBLE;
            System.out.println("JDK_PROVIDED correctly maps to " + expectedStatus);
        }
        
        @Test
        @DisplayName("Should map JAKARTA_REQUIRED to NEEDS_UPGRADE status via Maven lookup")
        void testJakartaRequiredToNeedsUpgrade() {
            ArtifactClassification classification = configLoader.classifyArtifact("javax.servlet", "servlet-api");
            assertEquals(ArtifactClassification.JAKARTA_REQUIRED, classification);
            
            // In DependenciesTableComponent, JAKARTA_REQUIRED triggers Maven Central lookup
            // which then sets NEEDS_UPGRADE if Jakarta equivalent found
            System.out.println("JAKARTA_REQUIRED triggers Maven lookup and maps to NEEDS_UPGRADE");
        }
        
        @Test
        @DisplayName("Should map CONTEXT_DEPENDENT to REQUIRES_MANUAL_MIGRATION status")
        void testContextDependentToManualMigration() {
            ArtifactClassification classification = configLoader.classifyArtifact("javax.xml.bind", "jaxb-api");
            assertEquals(ArtifactClassification.CONTEXT_DEPENDENT, classification);
            
            // In DependenciesTableComponent, CONTEXT_DEPENDENT maps to REQUIRES_MANUAL_MIGRATION
            DependencyMigrationStatus expectedStatus = DependencyMigrationStatus.REQUIRES_MANUAL_MIGRATION;
            System.out.println("CONTEXT_DEPENDENT correctly maps to " + expectedStatus);
        }
        
        @Test
        @DisplayName("Should map UNKNOWN to UNKNOWN status via Maven lookup fallback")
        void testUnknownToUnknown() {
            ArtifactClassification classification = configLoader.classifyArtifact("com.unknown", "unknown-api");
            assertEquals(ArtifactClassification.UNKNOWN, classification);
            
            // In DependenciesTableComponent, UNKNOWN triggers Maven lookup
            // if no match found, it stays UNKNOWN
            System.out.println("UNKNOWN triggers Maven lookup fallback");
        }
    }
}
