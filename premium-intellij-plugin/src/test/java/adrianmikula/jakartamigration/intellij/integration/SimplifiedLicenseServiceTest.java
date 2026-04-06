package adrianmikula.jakartamigration.intellij.integration;

import adrianmikula.jakartamigration.config.SimplifiedLicenseService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for simplified license service
 */
public class SimplifiedLicenseServiceTest {
    
    @TempDir
    Path tempDir;
    
    @Test
    @DisplayName("Should default to community tier")
    void testDefaultTier() {
        SimplifiedLicenseService service = new SimplifiedLicenseService();
        
        assertEquals(SimplifiedLicenseService.LicenseTier.COMMUNITY, service.getLicenseTier());
        assertFalse(service.hasPremiumFeatures());
        assertEquals("Community (Free)", service.getSubscriptionStatus());
    }
    
    @Test
    @DisplayName("Should handle premium tier from system property")
    void testPremiumTier() {
        // Set system property
        System.setProperty("jakarta.migration.license.tier", "PREMIUM");
        
        SimplifiedLicenseService service = new SimplifiedLicenseService();
        
        assertEquals(SimplifiedLicenseService.LicenseTier.PREMIUM, service.getLicenseTier());
        assertTrue(service.hasPremiumFeatures());
        assertEquals("Premium Subscription", service.getSubscriptionStatus());
    }
    
    @Test
    @DisplayName("Should handle invalid tier gracefully")
    void testInvalidTier() {
        // Set invalid system property
        System.setProperty("jakarta.migration.license.tier", "INVALID");
        
        SimplifiedLicenseService service = new SimplifiedLicenseService();
        
        assertEquals(SimplifiedLicenseService.LicenseTier.COMMUNITY, service.getLicenseTier()); // Should default to community
        assertFalse(service.hasPremiumFeatures());
    }
    
    @Test
    @DisplayName("Should return simple subscription status")
    void testSubscriptionStatus() {
        SimplifiedLicenseService service = new SimplifiedLicenseService();
        
        String status = service.getSubscriptionStatus();
        assertNotNull(status);
        assertTrue(status.contains("Premium") || status.contains("Community"));
    }
    
    @Test
    @DisplayName("Should have no trial by default")
    void testNoTrial() {
        SimplifiedLicenseService service = new SimplifiedLicenseService();
        
        assertFalse(service.isTrialActive());
        assertEquals(0, service.getRemainingTrialDays());
    }
}
