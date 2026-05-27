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

    @Nested
    @DisplayName("Spring Framework Ecosystem")
    class SpringFrameworkEcosystem {
        
        @Test
        @DisplayName("Should find Spring Boot 3.x starter for web")
        void shouldFindSpringBootWebStarter() throws Exception {
            CompletableFuture<List<JakartaArtifactMatch>> result = 
                lookupService.findJakartaEquivalents("org.springframework.boot", "spring-boot-starter-web");
            
            List<JakartaArtifactMatch> artifacts = result.get(30, TimeUnit.SECONDS);
            
            assertThat(artifacts).isNotEmpty();
            // Spring Boot 3.x+ uses jakarta.* packages
            assertThat(artifacts.get(0).groupId()).isEqualTo("org.springframework.boot");
            assertThat(artifacts.get(0).artifactId()).isEqualTo("spring-boot-starter-web");
            // Version should be 3.x or higher (Jakarta EE 9+)
            String version = artifacts.get(0).version();
            int majorVersion = Integer.parseInt(version.split("\\.")[0]);
            assertThat(majorVersion).isGreaterThanOrEqualTo(3);
        }
        
        @Test
        @DisplayName("Should find Spring Boot 3.x starter for data JPA")
        void shouldFindSpringBootDataJpaStarter() throws Exception {
            CompletableFuture<List<JakartaArtifactMatch>> result = 
                lookupService.findJakartaEquivalents("org.springframework.boot", "spring-boot-starter-data-jpa");
            
            List<JakartaArtifactMatch> artifacts = result.get(30, TimeUnit.SECONDS);
            
            assertThat(artifacts).isNotEmpty();
            assertThat(artifacts.get(0).groupId()).isEqualTo("org.springframework.boot");
            assertThat(artifacts.get(0).artifactId()).isEqualTo("spring-boot-starter-data-jpa");
        }
        
        @Test
        @DisplayName("Should find Spring Boot 3.x starter for validation")
        void shouldFindSpringBootValidationStarter() throws Exception {
            CompletableFuture<List<JakartaArtifactMatch>> result = 
                lookupService.findJakartaEquivalents("org.springframework.boot", "spring-boot-starter-validation");
            
            List<JakartaArtifactMatch> artifacts = result.get(30, TimeUnit.SECONDS);
            
            assertThat(artifacts).isNotEmpty();
            assertThat(artifacts.get(0).artifactId()).isEqualTo("spring-boot-starter-validation");
        }
        
        @Test
        @DisplayName("Should find Spring Framework 6.x web")
        void shouldFindSpringFramework6Web() throws Exception {
            CompletableFuture<List<JakartaArtifactMatch>> result = 
                lookupService.findJakartaEquivalents("org.springframework", "spring-web");
            
            List<JakartaArtifactMatch> artifacts = result.get(30, TimeUnit.SECONDS);
            
            assertThat(artifacts).isNotEmpty();
            assertThat(artifacts.get(0).groupId()).isEqualTo("org.springframework");
            assertThat(artifacts.get(0).artifactId()).isEqualTo("spring-web");
            // Spring Framework 6.x+ uses jakarta.* packages
            String version = artifacts.get(0).version();
            int majorVersion = Integer.parseInt(version.split("\\.")[0]);
            assertThat(majorVersion).isGreaterThanOrEqualTo(6);
        }
        
        @Test
        @DisplayName("Should find Spring Security 6.x")
        void shouldFindSpringSecurity6() throws Exception {
            CompletableFuture<List<JakartaArtifactMatch>> result = 
                lookupService.findJakartaEquivalents("org.springframework.security", "spring-security-config");
            
            List<JakartaArtifactMatch> artifacts = result.get(30, TimeUnit.SECONDS);
            
            assertThat(artifacts).isNotEmpty();
            assertThat(artifacts.get(0).groupId()).isEqualTo("org.springframework.security");
            assertThat(artifacts.get(0).artifactId()).isEqualTo("spring-security-config");
        }
        
        @Test
        @DisplayName("Should find Spring Data JPA 3.x")
        void shouldFindSpringDataJpa3() throws Exception {
            CompletableFuture<List<JakartaArtifactMatch>> result = 
                lookupService.findJakartaEquivalents("org.springframework.data", "spring-data-jpa");
            
            List<JakartaArtifactMatch> artifacts = result.get(30, TimeUnit.SECONDS);
            
            assertThat(artifacts).isNotEmpty();
            assertThat(artifacts.get(0).groupId()).isEqualTo("org.springframework.data");
            assertThat(artifacts.get(0).artifactId()).isEqualTo("spring-data-jpa");
            // Spring Data 3.x+ uses jakarta.* packages
            String version = artifacts.get(0).version();
            int majorVersion = Integer.parseInt(version.split("\\.")[0]);
            assertThat(majorVersion).isGreaterThanOrEqualTo(3);
        }
    }

    @Nested
    @DisplayName("JAX-RS Implementation Frameworks")
    class JaxRsImplementations {
        
        @Test
        @DisplayName("Should find Jersey 3.x Jakarta artifacts")
        void shouldFindJersey3() throws Exception {
            CompletableFuture<List<JakartaArtifactMatch>> result = 
                lookupService.findJakartaEquivalents("org.glassfish.jersey.core", "jersey-server");
            
            List<JakartaArtifactMatch> artifacts = result.get(30, TimeUnit.SECONDS);
            
            assertThat(artifacts).isNotEmpty();
            assertThat(artifacts.get(0).groupId()).isEqualTo("org.glassfish.jersey.core");
            assertThat(artifacts.get(0).artifactId()).isEqualTo("jersey-server");
            // Jersey 3.x+ uses jakarta.* packages
            String version = artifacts.get(0).version();
            int majorVersion = Integer.parseInt(version.split("\\.")[0]);
            assertThat(majorVersion).isGreaterThanOrEqualTo(3);
        }
        
        @Test
        @DisplayName("Should migrate Jersey 1.x com.sun.jersey to org.glassfish.jersey")
        void shouldMigrateJersey1ToJersey3() throws Exception {
            CompletableFuture<List<JakartaArtifactMatch>> result = 
                lookupService.findJakartaEquivalents("com.sun.jersey", "jersey-server");
            
            List<JakartaArtifactMatch> artifacts = result.get(30, TimeUnit.SECONDS);
            
            assertThat(artifacts).isNotEmpty();
            // Should find org.glassfish.jersey artifacts
            boolean foundGlassfishGroup = artifacts.stream()
                .anyMatch(a -> a.groupId().startsWith("org.glassfish.jersey"));
            assertThat(foundGlassfishGroup).isTrue();
        }
        
        @Test
        @DisplayName("Should find RESTEasy 6.x Jakarta artifacts")
        void shouldFindResteasy6() throws Exception {
            CompletableFuture<List<JakartaArtifactMatch>> result = 
                lookupService.findJakartaEquivalents("org.jboss.resteasy", "resteasy-core");
            
            List<JakartaArtifactMatch> artifacts = result.get(30, TimeUnit.SECONDS);
            
            assertThat(artifacts).isNotEmpty();
            assertThat(artifacts.get(0).groupId()).isEqualTo("org.jboss.resteasy");
            // RESTEasy 6.x+ uses jakarta.* packages
            String version = artifacts.get(0).version();
            int majorVersion = Integer.parseInt(version.split("\\.")[0]);
            assertThat(majorVersion).isGreaterThanOrEqualTo(6);
        }
        
        @Test
        @DisplayName("Should migrate resteasy-jaxrs to resteasy-core")
        void shouldMigrateResteasyJaxrsToCore() throws Exception {
            CompletableFuture<List<JakartaArtifactMatch>> result = 
                lookupService.findJakartaEquivalents("org.jboss.resteasy", "resteasy-jaxrs");
            
            List<JakartaArtifactMatch> artifacts = result.get(30, TimeUnit.SECONDS);
            
            assertThat(artifacts).isNotEmpty();
            // Should find resteasy-core artifact
            boolean foundResteasyCore = artifacts.stream()
                .anyMatch(a -> a.artifactId().equals("resteasy-core"));
            assertThat(foundResteasyCore).isTrue();
        }
        
        @Test
        @DisplayName("Should find Apache CXF 4.x Jakarta artifacts")
        void shouldFindCxf4() throws Exception {
            CompletableFuture<List<JakartaArtifactMatch>> result = 
                lookupService.findJakartaEquivalents("org.apache.cxf", "cxf-rt-frontend-jaxrs");
            
            List<JakartaArtifactMatch> artifacts = result.get(30, TimeUnit.SECONDS);
            
            assertThat(artifacts).isNotEmpty();
            assertThat(artifacts.get(0).groupId()).isEqualTo("org.apache.cxf");
            assertThat(artifacts.get(0).artifactId()).isEqualTo("cxf-rt-frontend-jaxrs");
            // CXF 4.x+ uses jakarta.* packages
            String version = artifacts.get(0).version();
            int majorVersion = Integer.parseInt(version.split("\\.")[0]);
            assertThat(majorVersion).isGreaterThanOrEqualTo(4);
        }
    }

    @Nested
    @DisplayName("Enterprise Frameworks")
    class EnterpriseFrameworks {
        
        @Test
        @DisplayName("Should find Apache Wicket 9.x/10.x Jakarta artifacts")
        void shouldFindWicket9Or10() throws Exception {
            CompletableFuture<List<JakartaArtifactMatch>> result = 
                lookupService.findJakartaEquivalents("org.apache.wicket", "wicket-core");
            
            List<JakartaArtifactMatch> artifacts = result.get(30, TimeUnit.SECONDS);
            
            assertThat(artifacts).isNotEmpty();
            assertThat(artifacts.get(0).groupId()).isEqualTo("org.apache.wicket");
            assertThat(artifacts.get(0).artifactId()).isEqualTo("wicket-core");
            // Wicket 9.x+ uses jakarta.* packages
            String version = artifacts.get(0).version();
            int majorVersion = Integer.parseInt(version.split("\\.")[0]);
            assertThat(majorVersion).isGreaterThanOrEqualTo(9);
        }
        
        @Test
        @DisplayName("Should find MyBatis-Spring-Boot 3.x Jakarta artifacts")
        void shouldFindMyBatisSpringBoot3() throws Exception {
            CompletableFuture<List<JakartaArtifactMatch>> result = 
                lookupService.findJakartaEquivalents("org.mybatis.spring.boot", "mybatis-spring-boot-starter");
            
            List<JakartaArtifactMatch> artifacts = result.get(30, TimeUnit.SECONDS);
            
            assertThat(artifacts).isNotEmpty();
            assertThat(artifacts.get(0).groupId()).isEqualTo("org.mybatis.spring.boot");
            assertThat(artifacts.get(0).artifactId()).isEqualTo("mybatis-spring-boot-starter");
        }
        
        @Test
        @DisplayName("Should find Hibernate 6.x (Jakarta JPA)")
        void shouldFindHibernate6() throws Exception {
            CompletableFuture<List<JakartaArtifactMatch>> result = 
                lookupService.findJakartaEquivalents("org.hibernate", "hibernate-core");
            
            List<JakartaArtifactMatch> artifacts = result.get(30, TimeUnit.SECONDS);
            
            assertThat(artifacts).isNotEmpty();
            assertThat(artifacts.get(0).groupId()).isEqualTo("org.hibernate");
            assertThat(artifacts.get(0).artifactId()).isEqualTo("hibernate-core");
            // Hibernate 6.x+ uses jakarta.* packages
            String version = artifacts.get(0).version();
            int majorVersion = Integer.parseInt(version.split("\\.")[0]);
            assertThat(majorVersion).isGreaterThanOrEqualTo(6);
        }
        
        @Test
        @DisplayName("Should migrate hibernate-entitymanager to hibernate-core")
        void shouldMigrateHibernateEntityManager() throws Exception {
            CompletableFuture<List<JakartaArtifactMatch>> result = 
                lookupService.findJakartaEquivalents("org.hibernate", "hibernate-entitymanager");
            
            List<JakartaArtifactMatch> artifacts = result.get(30, TimeUnit.SECONDS);
            
            assertThat(artifacts).isNotEmpty();
            // hibernate-entitymanager was merged into hibernate-core in 5.2+
            boolean foundHibernateCore = artifacts.stream()
                .anyMatch(a -> a.artifactId().equals("hibernate-core"));
            assertThat(foundHibernateCore).isTrue();
        }
        
        @Test
        @DisplayName("Should find Hibernate Validator 7.x (Jakarta)")
        void shouldFindHibernateValidator7() throws Exception {
            CompletableFuture<List<JakartaArtifactMatch>> result = 
                lookupService.findJakartaEquivalents("org.hibernate.validator", "hibernate-validator");
            
            List<JakartaArtifactMatch> artifacts = result.get(30, TimeUnit.SECONDS);
            
            assertThat(artifacts).isNotEmpty();
            assertThat(artifacts.get(0).groupId()).isEqualTo("org.hibernate.validator");
            assertThat(artifacts.get(0).artifactId()).isEqualTo("hibernate-validator");
            // Hibernate Validator 7.x+ uses jakarta.* packages
            String version = artifacts.get(0).version();
            int majorVersion = Integer.parseInt(version.split("\\.")[0]);
            assertThat(majorVersion).isGreaterThanOrEqualTo(7);
        }
    }

    @Nested
    @DisplayName("Complex Migration Scenarios")
    class ComplexMigrationScenarios {
        
        @Test
        @DisplayName("Should handle Spring Boot with Jersey integration")
        void shouldHandleSpringBootWithJersey() throws Exception {
            CompletableFuture<List<JakartaArtifactMatch>> result = 
                lookupService.findJakartaEquivalents("org.springframework.boot", "spring-boot-starter-jersey");
            
            List<JakartaArtifactMatch> artifacts = result.get(30, TimeUnit.SECONDS);
            
            assertThat(artifacts).isNotEmpty();
            assertThat(artifacts.get(0).groupId()).isEqualTo("org.springframework.boot");
            assertThat(artifacts.get(0).artifactId()).isEqualTo("spring-boot-starter-jersey");
            // Boot 3.x includes Jersey 3.x (Jakarta EE 9+)
            String version = artifacts.get(0).version();
            int majorVersion = Integer.parseInt(version.split("\\.")[0]);
            assertThat(majorVersion).isGreaterThanOrEqualTo(3);
        }
        
        @Test
        @DisplayName("Should handle Jersey servlet container migration")
        void shouldHandleJerseyServletContainerMigration() throws Exception {
            // Legacy jersey-servlet should map to jersey-container-servlet
            CompletableFuture<List<JakartaArtifactMatch>> result = 
                lookupService.findJakartaEquivalents("com.sun.jersey", "jersey-servlet");
            
            List<JakartaArtifactMatch> artifacts = result.get(30, TimeUnit.SECONDS);
            
            assertThat(artifacts).isNotEmpty();
            // Should find container-servlet variant
            boolean foundContainerServlet = artifacts.stream()
                .anyMatch(a -> a.artifactId().contains("container"));
            assertThat(foundContainerServlet).isTrue();
        }
    }
    
    @Nested
    @DisplayName("Critical Real-World Framework Artifacts")
    class CriticalFrameworkArtifacts {
        
        @Test
        @DisplayName("Should find Jakarta equivalent for RESTEasy client")
        void shouldFindJakartaForRestEasyClient() throws Exception {
            CompletableFuture<List<JakartaArtifactMatch>> result = 
                lookupService.findJakartaEquivalents("org.jboss.resteasy", "resteasy-client");
            
            List<JakartaArtifactMatch> artifacts = result.get(30, TimeUnit.SECONDS);
            
            assertThat(artifacts).isNotEmpty();
            // RESTEasy 6+ supports Jakarta EE
            assertThat(artifacts.get(0).groupId()).isEqualTo("org.jboss.resteasy");
            assertThat(artifacts.get(0).artifactId()).isEqualTo("resteasy-client");
        }
        
        @Test
        @DisplayName("Should find Jakarta equivalent for Jersey client")
        void shouldFindJakartaForJerseyClient() throws Exception {
            CompletableFuture<List<JakartaArtifactMatch>> result = 
                lookupService.findJakartaEquivalents("org.glassfish.jersey.core", "jersey-client");
            
            List<JakartaArtifactMatch> artifacts = result.get(30, TimeUnit.SECONDS);
            
            assertThat(artifacts).isNotEmpty();
            // Jersey 3+ is Jakarta EE compatible
            assertThat(artifacts.get(0).groupId()).isEqualTo("org.glassfish.jersey.core");
            assertThat(artifacts.get(0).artifactId()).isEqualTo("jersey-client");
        }
        
        @Test
        @DisplayName("Should find Jakarta equivalent for GlassFish JSON implementation")
        void shouldFindJakartaForGlassFishJson() throws Exception {
            CompletableFuture<List<JakartaArtifactMatch>> result = 
                lookupService.findJakartaEquivalents("org.glassfish", "javax.json");
            
            List<JakartaArtifactMatch> artifacts = result.get(30, TimeUnit.SECONDS);
            
            assertThat(artifacts).isNotEmpty();
            // Should map to jakarta.json implementation
            assertThat(artifacts.get(0).groupId()).isEqualTo("org.eclipse.ee4j");
            assertThat(artifacts.get(0).artifactId()).isEqualTo("jakarta.json");
        }
        
        @Test
        @DisplayName("Should find Jakarta equivalent for JAXB runtime")
        void shouldFindJakartaForJaxbRuntime() throws Exception {
            CompletableFuture<List<JakartaArtifactMatch>> result = 
                lookupService.findJakartaEquivalents("org.glassfish.jaxb", "jaxb-runtime");
            
            List<JakartaArtifactMatch> artifacts = result.get(30, TimeUnit.SECONDS);
            
            assertThat(artifacts).isNotEmpty();
            // Eclipse JAXB supports Jakarta EE
            assertThat(artifacts.get(0).groupId()).isEqualTo("org.glassfish.jaxb");
            assertThat(artifacts.get(0).artifactId()).isEqualTo("jaxb-runtime");
        }
        
        @Test
        @DisplayName("Should find Jakarta equivalent for Arquillian protocol servlet")
        void shouldFindJakartaForArquillianServlet() throws Exception {
            CompletableFuture<List<JakartaArtifactMatch>> result = 
                lookupService.findJakartaEquivalents("org.jboss.arquillian.protocol", "arquillian-protocol-servlet");
            
            List<JakartaArtifactMatch> artifacts = result.get(30, TimeUnit.SECONDS);
            
            assertThat(artifacts).isNotEmpty();
            // Arquillian 1.7+ supports Jakarta EE
            assertThat(artifacts.get(0).groupId()).isEqualTo("org.jboss.arquillian.protocol");
            assertThat(artifacts.get(0).artifactId()).isEqualTo("arquillian-protocol-servlet");
        }
        
        @Test
        @DisplayName("Should find Jakarta equivalent for TomEE embedded")
        void shouldFindJakartaForTomEeEmbedded() throws Exception {
            CompletableFuture<List<JakartaArtifactMatch>> result = 
                lookupService.findJakartaEquivalents("org.apache.tomee", "tomee-embedded");
            
            List<JakartaArtifactMatch> artifacts = result.get(30, TimeUnit.SECONDS);
            
            assertThat(artifacts).isNotEmpty();
            // TomEE 9+ is Jakarta EE compatible
            assertThat(artifacts.get(0).groupId()).isEqualTo("org.apache.tomee");
            assertThat(artifacts.get(0).artifactId()).isEqualTo("tomee-embedded");
        }
        
        @Test
        @DisplayName("Should find Jakarta equivalent for WildFly Arquillian container")
        void shouldFindJakartaForWildflyArquillian() throws Exception {
            CompletableFuture<List<JakartaArtifactMatch>> result = 
                lookupService.findJakartaEquivalents("org.wildfly.arquillian", "wildfly-arquillian-container-remote");
            
            List<JakartaArtifactMatch> artifacts = result.get(30, TimeUnit.SECONDS);
            
            assertThat(artifacts).isNotEmpty();
            // WildFly 27+ supports Jakarta EE
            assertThat(artifacts.get(0).groupId()).isEqualTo("org.wildfly.arquillian");
            assertThat(artifacts.get(0).artifactId()).isEqualTo("wildfly-arquillian-container-remote");
        }
        
        @Test
        @DisplayName("Should find Jakarta equivalent for ShrinkWrap resolver")
        void shouldFindJakartaForShrinkWrapResolver() throws Exception {
            CompletableFuture<List<JakartaArtifactMatch>> result = 
                lookupService.findJakartaEquivalents("org.jboss.shrinkwrap.resolver", "shrinkwrap-resolver-impl-maven");
            
            List<JakartaArtifactMatch> artifacts = result.get(30, TimeUnit.SECONDS);
            
            assertThat(artifacts).isNotEmpty();
            // ShrinkWrap resolver supports Jakarta EE in newer versions
            assertThat(artifacts.get(0).groupId()).isEqualTo("org.jboss.shrinkwrap.resolver");
            assertThat(artifacts.get(0).artifactId()).isEqualTo("shrinkwrap-resolver-impl-maven");
        }
    }
}
