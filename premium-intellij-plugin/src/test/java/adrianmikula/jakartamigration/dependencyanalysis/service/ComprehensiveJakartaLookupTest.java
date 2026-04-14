package adrianmikula.jakartamigration.dependencyanalysis.service;

import adrianmikula.jakartamigration.dependencyanalysis.service.ImprovedMavenCentralLookupService.JakartaArtifactMatch;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * Test-Driven Development tests for comprehensive Jakarta artifact lookup.
 * Tests all common javax to jakarta mappings to ensure robust fuzzy matching.
 *
 * NOTE: These tests require network access to Maven Central.
 * Run with: ./gradlew :premium-intellij-plugin:runIntegrationTests
 */
@DisplayName("Comprehensive Jakarta Artifact Lookup - TDD")
@org.junit.jupiter.api.Disabled("Requires network access to Maven Central - run via runIntegrationTests task")
public class ComprehensiveJakartaLookupTest {
    
    private ImprovedMavenCentralLookupService lookupService;
    
    @BeforeEach
    void setUp() {
        lookupService = new ImprovedMavenCentralLookupService();
    }
    
    @Nested
    @DisplayName("Core Servlet APIs")
    class ServletApis {
        
        @Test
        @DisplayName("Should find Jakarta equivalent for javax.servlet-api")
        void shouldFindJakartaForJavaxServletApi() throws Exception {
            CompletableFuture<List<JakartaArtifactMatch>> result = 
                lookupService.findJakartaEquivalents("javax.servlet", "javax.servlet-api");
            
            List<JakartaArtifactMatch> artifacts = result.get(30, TimeUnit.SECONDS);
            
            assertThat(artifacts).isNotEmpty();
            assertThat(artifacts.get(0).groupId()).isEqualTo("jakarta.servlet");
            assertThat(artifacts.get(0).artifactId()).isEqualTo("jakarta.servlet-api");
        }
        
        @Test
        @DisplayName("Should find Jakarta equivalent for javax.servlet.jsp-api")
        void shouldFindJakartaForJavaxJspApi() throws Exception {
            CompletableFuture<List<JakartaArtifactMatch>> result = 
                lookupService.findJakartaEquivalents("javax.servlet.jsp", "javax.servlet.jsp-api");
            
            List<JakartaArtifactMatch> artifacts = result.get(30, TimeUnit.SECONDS);
            
            assertThat(artifacts).isNotEmpty();
            assertThat(artifacts.get(0).groupId()).isEqualTo("jakarta.servlet.jsp");
            assertThat(artifacts.get(0).artifactId()).isEqualTo("jakarta.servlet.jsp-api");
        }
        
        @Test
        @DisplayName("Should find Jakarta equivalent for javax.servlet.jsp.jstl-api")
        void shouldFindJakartaForJavaxJstlApi() throws Exception {
            CompletableFuture<List<JakartaArtifactMatch>> result = 
                lookupService.findJakartaEquivalents("javax.servlet.jsp.jstl", "javax.servlet.jsp.jstl-api");
            
            List<JakartaArtifactMatch> artifacts = result.get(30, TimeUnit.SECONDS);
            
            assertThat(artifacts).isNotEmpty();
            assertThat(artifacts.get(0).groupId()).isEqualTo("jakarta.servlet.jsp.jstl");
            assertThat(artifacts.get(0).artifactId()).isEqualTo("jakarta.servlet.jsp.jstl-api");
        }
    }
    
    @Nested
    @DisplayName("Enterprise Persistence APIs")
    class PersistenceApis {
        
        @Test
        @DisplayName("Should find Jakarta equivalent for javax.persistence-api")
        void shouldFindJakartaForJavaxPersistenceApi() throws Exception {
            CompletableFuture<List<JakartaArtifactMatch>> result = 
                lookupService.findJakartaEquivalents("javax.persistence", "javax.persistence-api");
            
            List<JakartaArtifactMatch> artifacts = result.get(30, TimeUnit.SECONDS);
            
            assertThat(artifacts).isNotEmpty();
            assertThat(artifacts.get(0).groupId()).isEqualTo("jakarta.persistence");
            assertThat(artifacts.get(0).artifactId()).isEqualTo("jakarta.persistence-api");
        }
        
        @Test
        @DisplayName("Should find Jakarta equivalent for javax.transaction-api")
        void shouldFindJakartaForJavaxTransactionApi() throws Exception {
            CompletableFuture<List<JakartaArtifactMatch>> result = 
                lookupService.findJakartaEquivalents("javax.transaction", "javax.transaction-api");
            
            List<JakartaArtifactMatch> artifacts = result.get(30, TimeUnit.SECONDS);
            
            assertThat(artifacts).isNotEmpty();
            assertThat(artifacts.get(0).groupId()).isEqualTo("jakarta.transaction");
            assertThat(artifacts.get(0).artifactId()).isEqualTo("jakarta.transaction-api");
        }
        
        @Test
        @DisplayName("Should find Jakarta equivalent for javax.ejb-api")
        void shouldFindJakartaForJavaxEjbApi() throws Exception {
            CompletableFuture<List<JakartaArtifactMatch>> result = 
                lookupService.findJakartaEquivalents("javax.ejb", "javax.ejb-api");
            
            List<JakartaArtifactMatch> artifacts = result.get(30, TimeUnit.SECONDS);
            
            assertThat(artifacts).isNotEmpty();
            assertThat(artifacts.get(0).groupId()).isEqualTo("jakarta.ejb");
            assertThat(artifacts.get(0).artifactId()).isEqualTo("jakarta.ejb-api");
        }
    }
    
    @Nested
    @DisplayName("Validation and Injection APIs")
    class ValidationAndInjectionApis {
        
        @Test
        @DisplayName("Should find Jakarta equivalent for javax.validation-api")
        void shouldFindJakartaForJavaxValidationApi() throws Exception {
            CompletableFuture<List<JakartaArtifactMatch>> result = 
                lookupService.findJakartaEquivalents("javax.validation", "javax.validation-api");
            
            List<JakartaArtifactMatch> artifacts = result.get(30, TimeUnit.SECONDS);
            
            assertThat(artifacts).isNotEmpty();
            assertThat(artifacts.get(0).groupId()).isEqualTo("jakarta.validation");
            assertThat(artifacts.get(0).artifactId()).isEqualTo("jakarta.validation-api");
        }
        
        @Test
        @DisplayName("Should find Jakarta equivalent for javax.inject")
        void shouldFindJakartaForJavaxInject() throws Exception {
            CompletableFuture<List<JakartaArtifactMatch>> result = 
                lookupService.findJakartaEquivalents("javax.inject", "javax.inject");
            
            List<JakartaArtifactMatch> artifacts = result.get(30, TimeUnit.SECONDS);
            
            assertThat(artifacts).isNotEmpty();
            assertThat(artifacts.get(0).groupId()).isEqualTo("jakarta.inject");
            assertThat(artifacts.get(0).artifactId()).isEqualTo("jakarta.inject");
        }
        
        @Test
        @DisplayName("Should find Jakarta equivalent for javax.annotation-api")
        void shouldFindJakartaForJavaxAnnotationApi() throws Exception {
            CompletableFuture<List<JakartaArtifactMatch>> result = 
                lookupService.findJakartaEquivalents("javax.annotation", "javax.annotation-api");
            
            List<JakartaArtifactMatch> artifacts = result.get(30, TimeUnit.SECONDS);
            
            assertThat(artifacts).isNotEmpty();
            assertThat(artifacts.get(0).groupId()).isEqualTo("jakarta.annotation");
            assertThat(artifacts.get(0).artifactId()).isEqualTo("jakarta.annotation-api");
        }
    }
    
    @Nested
    @DisplayName("Web Services and XML APIs")
    class WebServicesAndXmlApis {
        
        @Test
        @DisplayName("Should find Jakarta equivalent for javax.xml.bind-api")
        void shouldFindJakartaForJavaxXmlBindApi() throws Exception {
            CompletableFuture<List<JakartaArtifactMatch>> result = 
                lookupService.findJakartaEquivalents("javax.xml.bind", "javax.xml.bind-api");
            
            List<JakartaArtifactMatch> artifacts = result.get(30, TimeUnit.SECONDS);
            
            assertThat(artifacts).isNotEmpty();
            assertThat(artifacts.get(0).groupId()).isEqualTo("jakarta.xml.bind");
            assertThat(artifacts.get(0).artifactId()).isEqualTo("jakarta.xml.bind-api");
        }
        
        @Test
        @DisplayName("Should find Jakarta equivalent for javax.xml.ws-api")
        void shouldFindJakartaForJavaxXmlWsApi() throws Exception {
            CompletableFuture<List<JakartaArtifactMatch>> result = 
                lookupService.findJakartaEquivalents("javax.xml.ws", "javax.xml.ws-api");
            
            List<JakartaArtifactMatch> artifacts = result.get(30, TimeUnit.SECONDS);
            
            assertThat(artifacts).isNotEmpty();
            assertThat(artifacts.get(0).groupId()).isEqualTo("jakarta.xml.ws");
            assertThat(artifacts.get(0).artifactId()).isEqualTo("jakarta.xml.ws-api");
        }
        
        @Test
        @DisplayName("Should find Jakarta equivalent for javax.ws.rs-api")
        void shouldFindJakartaForJavaxWsRsApi() throws Exception {
            CompletableFuture<List<JakartaArtifactMatch>> result = 
                lookupService.findJakartaEquivalents("javax.ws.rs", "javax.ws.rs-api");
            
            List<JakartaArtifactMatch> artifacts = result.get(30, TimeUnit.SECONDS);
            
            assertThat(artifacts).isNotEmpty();
            assertThat(artifacts.get(0).groupId()).isEqualTo("jakarta.ws.rs");
            assertThat(artifacts.get(0).artifactId()).isEqualTo("jakarta.ws.rs-api");
        }
    }
    
    @Nested
    @DisplayName("Enterprise Messaging and APIs")
    class EnterpriseMessagingApis {
        
        @Test
        @DisplayName("Should find Jakarta equivalent for javax.jms-api")
        void shouldFindJakartaForJavaxJmsApi() throws Exception {
            CompletableFuture<List<JakartaArtifactMatch>> result = 
                lookupService.findJakartaEquivalents("javax.jms", "javax.jms-api");
            
            List<JakartaArtifactMatch> artifacts = result.get(30, TimeUnit.SECONDS);
            
            assertThat(artifacts).isNotEmpty();
            assertThat(artifacts.get(0).groupId()).isEqualTo("jakarta.jms");
            assertThat(artifacts.get(0).artifactId()).isEqualTo("jakarta.jms-api");
        }
        
        @Test
        @DisplayName("Should find Jakarta equivalent for javax.json-api")
        void shouldFindJakartaForJavaxJsonApi() throws Exception {
            CompletableFuture<List<JakartaArtifactMatch>> result = 
                lookupService.findJakartaEquivalents("javax.json", "javax.json-api");
            
            List<JakartaArtifactMatch> artifacts = result.get(30, TimeUnit.SECONDS);
            
            assertThat(artifacts).isNotEmpty();
            assertThat(artifacts.get(0).groupId()).isEqualTo("jakarta.json");
            assertThat(artifacts.get(0).artifactId()).isEqualTo("jakarta.json-api");
        }
        
        @Test
        @DisplayName("Should find Jakarta equivalent for javax.websocket-api")
        void shouldFindJakartaForJavaxWebsocketApi() throws Exception {
            CompletableFuture<List<JakartaArtifactMatch>> result = 
                lookupService.findJakartaEquivalents("javax.websocket", "javax.websocket-api");
            
            List<JakartaArtifactMatch> artifacts = result.get(30, TimeUnit.SECONDS);
            
            assertThat(artifacts).isNotEmpty();
            assertThat(artifacts.get(0).groupId()).isEqualTo("jakarta.websocket");
            assertThat(artifacts.get(0).artifactId()).isEqualTo("jakarta.websocket-api");
        }
    }
    
    @Nested
    @DisplayName("Faces and UI APIs")
    class FacesAndUiApis {
        
        @Test
        @DisplayName("Should find Jakarta equivalent for javax.faces-api")
        void shouldFindJakartaForJavaxFacesApi() throws Exception {
            CompletableFuture<List<JakartaArtifactMatch>> result = 
                lookupService.findJakartaEquivalents("javax.faces", "javax.faces-api");
            
            List<JakartaArtifactMatch> artifacts = result.get(30, TimeUnit.SECONDS);
            
            assertThat(artifacts).isNotEmpty();
            assertThat(artifacts.get(0).groupId()).isEqualTo("jakarta.faces");
            assertThat(artifacts.get(0).artifactId()).isEqualTo("jakarta.faces-api");
        }
    }
    
    @Nested
    @DisplayName("Missing Jakarta Equivalents - Negative Tests")
    class MissingJakartaEquivalents {
        
        @Test
        @DisplayName("Should return empty list for javax.activation (no Jakarta equivalent)")
        void shouldReturnEmptyForJavaxActivation() throws Exception {
            CompletableFuture<List<JakartaArtifactMatch>> result = 
                lookupService.findJakartaEquivalents("javax.activation", "javax.activation-api");
            
            List<JakartaArtifactMatch> artifacts = result.get(30, TimeUnit.SECONDS);
            
            // Should return empty or minimal results since activation doesn't have a direct Jakarta equivalent
            assertThat(artifacts).hasSizeLessThanOrEqualTo(1);
        }
        
        @Test
        @DisplayName("Should return empty list for javax.resource (no Jakarta equivalent)")
        void shouldReturnEmptyForJavaxResource() throws Exception {
            CompletableFuture<List<JakartaArtifactMatch>> result = 
                lookupService.findJakartaEquivalents("javax.resource", "javax.resource-api");
            
            List<JakartaArtifactMatch> artifacts = result.get(30, TimeUnit.SECONDS);
            
            // Should return empty since JCA doesn't have a direct Jakarta equivalent
            assertThat(artifacts).isEmpty();
        }
        
        @Test
        @DisplayName("Should return empty list for javax.script (no Jakarta equivalent)")
        void shouldReturnEmptyForJavaxScript() throws Exception {
            CompletableFuture<List<JakartaArtifactMatch>> result = 
                lookupService.findJakartaEquivalents("javax.script", "javax.script-api");
            
            List<JakartaArtifactMatch> artifacts = result.get(30, TimeUnit.SECONDS);
            
            // Should return empty since scripting API doesn't have a Jakarta equivalent
            assertThat(artifacts).isEmpty();
        }
    }
    
    @Nested
    @DisplayName("Fuzzy Matching Edge Cases")
    class FuzzyMatchingEdgeCases {
        
        @Test
        @DisplayName("Should handle naming variations - javax.servlet vs javax.servlet-api")
        void shouldHandleNamingVariations() throws Exception {
            // Test both variations
            CompletableFuture<List<JakartaArtifactMatch>> result1 = 
                lookupService.findJakartaEquivalents("javax.servlet", "javax.servlet");
            CompletableFuture<List<JakartaArtifactMatch>> result2 = 
                lookupService.findJakartaEquivalents("javax.servlet", "javax.servlet-api");
            
            List<JakartaArtifactMatch> artifacts1 = result1.get(30, TimeUnit.SECONDS);
            List<JakartaArtifactMatch> artifacts2 = result2.get(30, TimeUnit.SECONDS);
            
            // Both should find the same Jakarta equivalent
            assertThat(artifacts1).isNotEmpty();
            assertThat(artifacts2).isNotEmpty();
            assertThat(artifacts1.get(0).artifactId()).isEqualTo(artifacts2.get(0).artifactId());
        }
        
        @Test
        @DisplayName("Should handle case insensitive matching")
        void shouldHandleCaseInsensitive() throws Exception {
            CompletableFuture<List<JakartaArtifactMatch>> result = 
                lookupService.findJakartaEquivalents("JAVAX.SERVLET", "JAVAX.SERVLET-API");
            
            List<JakartaArtifactMatch> artifacts = result.get(30, TimeUnit.SECONDS);
            
            assertThat(artifacts).isNotEmpty();
        }
        
        @Test
        @DisplayName("Should handle malformed coordinates gracefully")
        void shouldHandleMalformedCoordinates() throws Exception {
            CompletableFuture<List<JakartaArtifactMatch>> result = 
                lookupService.findJakartaEquivalents("", "invalid-artifact");
            
            List<JakartaArtifactMatch> artifacts = result.get(30, TimeUnit.SECONDS);
            
            // Should not crash, return empty list
            assertThat(artifacts).isEmpty();
        }
    }
}
