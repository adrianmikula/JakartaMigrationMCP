package adrianmikula.jakartamigration.dependencyanalysis.service;

import adrianmikula.jakartamigration.dependencyanalysis.service.ImprovedMavenCentralLookupService.JakartaArtifactMatch;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Debug test to understand why Maven lookups aren't working
 */
public class MavenLookupDebugTest {
    
    public static void main(String[] args) throws Exception {
        ImprovedMavenCentralLookupService lookupService = new ImprovedMavenCentralLookupService();
        
        System.out.println("=== Testing Maven Central Lookup ===\n");
        
        // Test 1: Exact match for javax.servlet-api
        System.out.println("1. Testing exact match for javax.servlet:javax.servlet-api");
        CompletableFuture<List<JakartaArtifactMatch>> result1 = 
            lookupService.findJakartaEquivalents("javax.servlet", "javax.servlet-api");
        
        List<JakartaArtifactMatch> artifacts1 = result1.get(30, TimeUnit.SECONDS);
        System.out.println("Results: " + artifacts1.size() + " artifacts found");
        artifacts1.forEach(a -> System.out.println("  - " + a.groupId() + ":" + a.artifactId() + ":" + a.version()));
        
        // Test 2: Artifact mapping for javax.servlet-api → jakarta.servlet-api
        System.out.println("\n2. Testing artifact mapping for javax.servlet:javax.servlet-api");
        CompletableFuture<List<JakartaArtifactMatch>> result2 = 
            lookupService.findJakartaEquivalents("javax.servlet", "javax.servlet-api");
        
        List<JakartaArtifactMatch> artifacts2 = result2.get(30, TimeUnit.SECONDS);
        System.out.println("Results: " + artifacts2.size() + " artifacts found");
        artifacts2.forEach(a -> System.out.println("  - " + a.groupId() + ":" + a.artifactId() + ":" + a.version()));
        
        // Test 3: Direct search for jakarta.servlet-api
        System.out.println("\n3. Testing direct search for jakarta.servlet:jakarta.servlet-api");
        CompletableFuture<List<JakartaArtifactMatch>> result3 = 
            lookupService.findJakartaEquivalents("jakarta.servlet", "jakarta.servlet-api");
        
        List<JakartaArtifactMatch> artifacts3 = result3.get(30, TimeUnit.SECONDS);
        System.out.println("Results: " + artifacts3.size() + " artifacts found");
        artifacts3.forEach(a -> System.out.println("  - " + a.groupId() + ":" + a.artifactId() + ":" + a.version()));
        
        System.out.println("\n=== Debug Complete ===");
    }
}
