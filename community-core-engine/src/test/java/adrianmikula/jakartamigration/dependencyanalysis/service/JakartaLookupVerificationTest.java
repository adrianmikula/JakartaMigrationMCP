package adrianmikula.jakartamigration.dependencyanalysis.service;

import adrianmikula.jakartamigration.dependencyanalysis.service.ImprovedMavenCentralLookupService.JakartaArtifactMatch;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Assumptions;

import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * Simple verification test for our TDD Jakarta lookup improvements.
 * These tests make real network calls to Maven Central and are tagged as "slow".
 */
@DisplayName("Jakarta Lookup Improvements Verification")
@Tag("slow")
public class JakartaLookupVerificationTest {
    
    private ImprovedMavenCentralLookupService lookupService;
    
    @BeforeEach
    void setUp() {
        lookupService = new ImprovedMavenCentralLookupService();
        // Skip tests if Maven Central is not reachable (offline or network issues)
        Assumptions.assumeTrue(isMavenCentralReachable(), 
            "Skipping test: Maven Central is not reachable (offline or network issues)");
    }
    
    private boolean isMavenCentralReachable() {
        try {
            InetAddress.getByName("repo1.maven.org").isReachable(3000);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Test
    @DisplayName("Should handle input validation gracefully")
    void shouldHandleInputValidation() throws Exception {
        // Test empty coordinates
        CompletableFuture<List<JakartaArtifactMatch>> result1 = 
            lookupService.findJakartaEquivalents("", "invalid-artifact");
        
        List<JakartaArtifactMatch> artifacts1 = result1.get(10, TimeUnit.SECONDS);
        assertThat(artifacts1).isEmpty();
        
        // Test null coordinates
        CompletableFuture<List<JakartaArtifactMatch>> result2 = 
            lookupService.findJakartaEquivalents(null, null);
        
        List<JakartaArtifactMatch> artifacts2 = result2.get(10, TimeUnit.SECONDS);
        assertThat(artifacts2).isEmpty();
        
        System.out.println("✅ Input validation works correctly");
    }
    
    @Test
    @DisplayName("Should handle case insensitive variations")
    void shouldHandleCaseInsensitive() throws Exception {
        // Test case insensitive matching
        CompletableFuture<List<JakartaArtifactMatch>> result = 
            lookupService.findJakartaEquivalents("JAVAX.SERVLET", "JAVAX.SERVLET-API");
        
        List<JakartaArtifactMatch> artifacts = result.get(30, TimeUnit.SECONDS);
        
        // Should find results due to case insensitive matching
        assertThat(artifacts).isNotEmpty();
        
        System.out.println("✅ Case insensitive matching works");
        System.out.println("   Found " + artifacts.size() + " results for JAVAX.SERVLET-API");
    }
    
    @Test
    @DisplayName("Should handle naming variations")
    void shouldHandleNamingVariations() throws Exception {
        // Test both with and without -api suffix
        CompletableFuture<List<JakartaArtifactMatch>> result1 = 
            lookupService.findJakartaEquivalents("javax.servlet", "javax.servlet");
        CompletableFuture<List<JakartaArtifactMatch>> result2 = 
            lookupService.findJakartaEquivalents("javax.servlet", "javax.servlet-api");
        
        List<JakartaArtifactMatch> artifacts1 = result1.get(30, TimeUnit.SECONDS);
        List<JakartaArtifactMatch> artifacts2 = result2.get(30, TimeUnit.SECONDS);
        
        // Both should find results
        assertThat(artifacts1).isNotEmpty();
        assertThat(artifacts2).isNotEmpty();
        
        System.out.println("✅ Naming variations work correctly");
        System.out.println("   javax.servlet: " + artifacts1.size() + " results");
        System.out.println("   javax.servlet-api: " + artifacts2.size() + " results");
    }
    
    @Test
    @DisplayName("Should find Jakarta equivalents for common javax artifacts")
    void shouldFindJakartaEquivalents() throws Exception {
        // Test a few common mappings
        String[] testCases = {
            "javax.persistence:javax.persistence-api",
            "javax.validation:javax.validation-api",
            "javax.annotation:javax.annotation-api"
        };
        
        for (String testCase : testCases) {
            String[] parts = testCase.split(":");
            String groupId = parts[0];
            String artifactId = parts[1];
            
            CompletableFuture<List<JakartaArtifactMatch>> result = 
                lookupService.findJakartaEquivalents(groupId, artifactId);
            
            List<JakartaArtifactMatch> artifacts = result.get(30, TimeUnit.SECONDS);
            
            assertThat(artifacts).isNotEmpty();
            
            // Check if we found Jakarta equivalents
            boolean foundJakarta = artifacts.stream()
                .anyMatch(match -> match.groupId() != null && match.groupId().startsWith("jakarta."));
                
            assertThat(foundJakarta).isTrue();
            
            System.out.println("✅ " + testCase + " → " + artifacts.size() + " Jakarta results found");
        }
    }

    @Test
    @DisplayName("Should find Jakarta equivalents for 5 popular third-party javax libraries")
    void shouldFindJakartaForRealWorldArtifacts() throws Exception {
        // 3rd-party javax libraries not covered by the static whitelist/blacklist.
        // Verification is purely through live Maven Central lookups.
        String[][] withJakartaCases = {
            {"org.apache.cxf", "cxf-rt-frontend-jaxws", "org.apache.cxf", "cxf-rt-frontend-jaxws"},
            {"org.apache.cxf", "cxf-rt-frontend-jaxrs", "org.apache.cxf", "cxf-rt-frontend-jaxrs"},
            {"org.hibernate", "hibernate-core", "org.hibernate", "hibernate-core"},
            {"org.apache.wicket", "wicket", "org.apache.wicket", "wicket"},
            {"org.apache.myfaces.core", "myfaces-api", "org.apache.myfaces.core", "myfaces-api"}
        };

        for (String[] c : withJakartaCases) {
            String inputGroup = c[0];
            String inputArtifact = c[1];
            String expectedGroup = c[2];
            String expectedArtifact = c[3];

            CompletableFuture<List<JakartaArtifactMatch>> result =
                lookupService.findJakartaEquivalents(inputGroup, inputArtifact);
            List<JakartaArtifactMatch> matches = result.get(30, TimeUnit.SECONDS);

            assertThat(matches)
                .withFailMessage("Expected at least one match for %s:%s", inputGroup, inputArtifact)
                .isNotEmpty();

            boolean foundExpected = matches.stream()
                .anyMatch(m -> expectedGroup.equals(m.groupId())
                    && expectedArtifact.equals(m.artifactId()));

            assertThat(foundExpected)
                .withFailMessage("Expected to find %s:%s for %s:%s",
                    expectedGroup, expectedArtifact, inputGroup, inputArtifact)
                .isTrue();
        }
    }

    @Test
    @DisplayName("Should not find Jakarta equivalents for 5 legacy third-party javax libraries")
    void shouldNotFindJakartaEquivalentsForLegacyThirdPartyLibraries() throws Exception {
        // Popular 3rd-party javax libraries that never received a Jakarta EE migration.
        String[][] withoutJakartaCases = {
            {"com.google.code.findbugs", "jsr305"},
            {"org.codehaus.jackson", "jackson-jaxrs"},
            {"org.codehaus.jackson", "jackson-xc"},
            {"org.apache.axis", "axis"},
            {"org.apache.axis", "axis-jaxrpc"}
        };

        for (String[] c : withoutJakartaCases) {
            String groupId = c[0];
            String artifactId = c[1];

            CompletableFuture<List<JakartaArtifactMatch>> result =
                lookupService.findJakartaEquivalents(groupId, artifactId);
            List<JakartaArtifactMatch> matches = result.get(30, TimeUnit.SECONDS);

            boolean noJakartaMatch = matches.stream().noneMatch(m -> {
                String g = m.groupId() == null ? "" : m.groupId();
                String a = m.artifactId() == null ? "" : m.artifactId();
                return g.startsWith("jakarta.") || a.startsWith("jakarta.");
            });

            assertThat(noJakartaMatch)
                .withFailMessage("Did not expect any jakarta.* match for %s:%s", groupId, artifactId)
                .isTrue();
        }
    }
}
