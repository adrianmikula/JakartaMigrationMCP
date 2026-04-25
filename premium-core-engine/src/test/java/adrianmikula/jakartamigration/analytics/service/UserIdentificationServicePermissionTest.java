package adrianmikula.jakartamigration.analytics.service;

import adrianmikula.jakartamigration.analytics.config.SupabaseConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for permission tracking functionality in UserIdentificationService.
 */
public class UserIdentificationServicePermissionTest {
    
    @TempDir
    Path tempDir;
    private UserIdentificationService userIdentificationService;
    private SupabaseConfig supabaseConfig;
    
    @BeforeEach
    void setUp() {
        supabaseConfig = new SupabaseConfig();
        userIdentificationService = new UserIdentificationService(tempDir, supabaseConfig);
    }
    
    @Test
    @DisplayName("Should return false for usage permission requested when not set")
    void shouldReturnFalseForUsagePermissionRequestedWhenNotSet() {
        // When
        boolean result = userIdentificationService.isUsagePermissionRequested();
        
        // Then
        assertFalse(result, "Usage permission should not be requested by default");
    }
    
    @Test
    @DisplayName("Should return true for usage permission requested after setting")
    void shouldReturnTrueForUsagePermissionRequestedAfterSetting() {
        // When
        userIdentificationService.setUsagePermissionRequested();
        boolean result = userIdentificationService.isUsagePermissionRequested();
        
        // Then
        assertTrue(result, "Usage permission should be requested after setting");
    }
    
    @Test
    @DisplayName("Should persist usage permission requested flag across instances")
    void shouldPersistUsagePermissionRequestedAcrossInstances() throws IOException {
        // Given
        userIdentificationService.setUsagePermissionRequested();
        
        // Create new instance with same storage path
        try (UserIdentificationService newService = new UserIdentificationService(tempDir, supabaseConfig)) {
            // When
            boolean result = newService.isUsagePermissionRequested();
            
            // Then
            assertTrue(result, "Usage permission requested flag should persist across instances");
        }
    }
    
    @Test
    @DisplayName("Should set usage permission requested flag in properties")
    void shouldSetUsagePermissionRequestedFlagInProperties() throws IOException {
        // When
        userIdentificationService.setUsagePermissionRequested();
        
        // Then - check properties file
        Path propertiesFile = tempDir.resolve("analytics-user-id.properties");
        assertTrue(Files.exists(propertiesFile), "Properties file should exist");
        
        Properties props = new Properties();
        props.load(Files.newInputStream(propertiesFile));
        
        assertEquals("true", props.getProperty("usage.permission.requested"), 
            "Usage permission requested flag should be set in properties");
    }
    
    @Test
    @DisplayName("Should handle multiple set usage permission requested calls")
    void shouldHandleMultipleSetUsagePermissionRequestedCalls() {
        // When
        userIdentificationService.setUsagePermissionRequested();
        userIdentificationService.setUsagePermissionRequested();
        boolean result = userIdentificationService.isUsagePermissionRequested();
        
        // Then
        assertTrue(result, "Multiple calls should not cause issues");
    }
    
    @Test
    @DisplayName("Should maintain other preferences when setting permission requested")
    void shouldMaintainOtherPreferencesWhenSettingPermissionRequested() {
        // Given
        userIdentificationService.setUsageMetricsEnabled(false);
        userIdentificationService.setErrorReportingEnabled(true);
        
        // When
        userIdentificationService.setUsagePermissionRequested();
        
        // Then
        assertFalse(userIdentificationService.isUsageMetricsOptedOut(), 
            "Usage metrics preference should be preserved");
        assertTrue(userIdentificationService.isErrorReportingEnabled(), 
            "Error reporting preference should be preserved");
        assertTrue(userIdentificationService.isUsagePermissionRequested(), 
            "Permission requested flag should be set");
    }
    
    @Test
    @DisplayName("Should create properties file if it doesn't exist")
    void shouldCreatePropertiesFileIfNotExists() {
        // Given
        Path propertiesFile = tempDir.resolve("analytics-user-id.properties");
        
        // Ensure file doesn't exist
        try {
            Files.deleteIfExists(propertiesFile);
        } catch (IOException e) {
            // Ignore
        }
        
        // When
        new UserIdentificationService(tempDir, supabaseConfig);
        
        // Then
        assertTrue(Files.exists(propertiesFile), "Properties file should be created");
    }
    
    @Test
    @DisplayName("Should handle corrupted properties file gracefully")
    void shouldHandleCorruptedPropertiesFileGracefully() throws IOException {
        // Given
        Path propertiesFile = tempDir.resolve("analytics-user-id.properties");
        
        // Write corrupted content
        Files.writeString(propertiesFile, "invalid=properties=content=\nmalformed");
        
        // When - should not throw exception
        assertDoesNotThrow(() -> {
            try (UserIdentificationService service = new UserIdentificationService(tempDir, supabaseConfig)) {
                // Should work with default values
                assertFalse(service.isUsagePermissionRequested());
                assertFalse(service.isUsageMetricsOptedOut());
                assertFalse(service.isErrorReportingOptedOut());
            }
        });
    }
    
    @Test
    @DisplayName("Should preserve permission requested flag with other operations")
    void shouldPreservePermissionRequestedFlagWithOtherOperations() {
        // Given
        userIdentificationService.setUsagePermissionRequested();
        assertTrue(userIdentificationService.isUsagePermissionRequested());
        
        // When - perform other operations
        userIdentificationService.setUsageMetricsEnabled(true);
        userIdentificationService.setErrorReportingEnabled(false);
        userIdentificationService.getAnonymousUserId();
        
        // Then - permission requested flag should still be true
        assertTrue(userIdentificationService.isUsagePermissionRequested(), 
            "Permission requested flag should be preserved");
    }
    
    @AfterEach
    void cleanup() throws IOException {
        if (userIdentificationService != null) {
            userIdentificationService.close();
        }
    }
}
