package adrianmikula.jakartamigration.preferences;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for UserPreferencesService to verify centralized preferences storage
 * and workspace isolation functionality.
 */
class UserPreferencesServiceTest {

    @TempDir
    Path tempDir;
    
    private UserPreferencesService preferencesService;

    @BeforeEach
    void setUp() {
        // Use a temporary directory for testing
        System.setProperty("user.home", tempDir.toString());
        preferencesService = new UserPreferencesService();
    }

    @AfterEach
    void tearDown() {
        if (preferencesService != null) {
            preferencesService.close();
        }
    }

    @Test
    @DisplayName("Should create preferences file in user home directory")
    void shouldCreatePreferencesFileInUserHome() {
        // When
        Path preferencesPath = tempDir.resolve(".jakartamigration").resolve("plugin-user-preferences.properties");
        
        // Then
        assertThat(Files.exists(preferencesPath)).isTrue();
    }

    @Test
    @DisplayName("Should store and retrieve anonymous user ID")
    void shouldStoreAndRetrieveAnonymousUserId() {
        // Given
        String userId = "test-user-123";
        
        // When
        preferencesService.setAnonymousUserId(userId);
        
        // Then
        assertThat(preferencesService.getAnonymousUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("Should store and retrieve usage metrics opt-out preference")
    void shouldStoreAndRetrieveUsageMetricsOptOut() {
        // Given
        boolean optedOut = true;
        
        // When
        preferencesService.setUsageMetricsOptedOut(optedOut);
        
        // Then
        assertThat(preferencesService.isUsageMetricsOptedOut()).isTrue();
    }

    @Test
    @DisplayName("Should store and retrieve error reporting opt-out preference")
    void shouldStoreAndRetrieveErrorReportingOptOut() {
        // Given
        boolean optedOut = false;
        
        // When
        preferencesService.setErrorReportingOptedOut(optedOut);
        
        // Then
        assertThat(preferencesService.isErrorReportingOptedOut()).isFalse();
    }

    @Test
    @DisplayName("Should store and retrieve first seen timestamp")
    void shouldStoreAndRetrieveFirstSeenTimestamp() {
        // Given
        long timestamp = System.currentTimeMillis() - 86400000; // Yesterday
        
        // When
        preferencesService.setFirstSeenTimestamp(timestamp);
        
        // Then
        assertThat(preferencesService.getFirstSeenTimestamp()).isEqualTo(timestamp);
    }

    @Test
    @DisplayName("Should store and retrieve last seen timestamp")
    void shouldStoreAndRetrieveLastSeenTimestamp() {
        // Given
        long timestamp = System.currentTimeMillis();
        
        // When
        preferencesService.setLastSeenTimestamp(timestamp);
        
        // Then
        assertThat(preferencesService.getLastSeenTimestamp()).isEqualTo(timestamp);
    }

    @Test
    @DisplayName("Should persist preferences across service instances")
    void shouldPersistPreferencesAcrossServiceInstances() {
        // Given
        String userId = "persistent-user-456";
        preferencesService.setAnonymousUserId(userId);
        
        // When - create new service instance
        UserPreferencesService newService = new UserPreferencesService();
        
        // Then - should retrieve same user ID
        assertThat(newService.getAnonymousUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("Should handle custom preferences")
    void shouldHandleCustomPreferences() {
        // Given
        String customKey = "custom.test.setting";
        String customValue = "test-value-789";
        
        // When
        preferencesService.setPreference(customKey, customValue);
        
        // Then
        assertThat(preferencesService.getPreference(customKey)).isEqualTo(customValue);
    }

    @Test
    @DisplayName("Should return default value for missing preference")
    void shouldReturnDefaultForMissingPreference() {
        // Given
        String nonExistentKey = "non.existent.key";
        String defaultValue = "default-value";
        
        // When
        String value = preferencesService.getPreference(nonExistentKey, defaultValue);
        
        // Then
        assertThat(value).isEqualTo(defaultValue);
    }

    @Test
    @DisplayName("Should remove preferences")
    void shouldRemovePreferences() {
        // Given
        String key = "test.key.to.remove";
        String value = "should-be-removed";
        preferencesService.setPreference(key, value);
        
        // Verify it exists
        assertThat(preferencesService.getPreference(key)).isEqualTo(value);
        
        // When
        preferencesService.removePreference(key);
        
        // Then
        assertThat(preferencesService.getPreference(key)).isNull();
    }

    @Test
    @DisplayName("Should refresh preferences from disk")
    void shouldRefreshPreferencesFromDisk() {
        // Given
        String userId = "refresh-test-user";
        preferencesService.setAnonymousUserId(userId);
        
        // Simulate external change by creating new service instance
        UserPreferencesService externalService = new UserPreferencesService();
        externalService.setAnonymousUserId("externally-modified-user");
        
        // When
        preferencesService.refreshPreferences();
        
        // Then - should have the externally modified value
        assertThat(preferencesService.getAnonymousUserId()).isEqualTo("externally-modified-user");
    }
}
