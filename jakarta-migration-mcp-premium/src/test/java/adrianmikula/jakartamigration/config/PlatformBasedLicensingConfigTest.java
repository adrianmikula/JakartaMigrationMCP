package adrianmikula.jakartamigration.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for platform-based licensing auto-configuration.
 * 
 * Verifies that:
 * - PlatformDetectionService correctly detects Apify vs local deployment
 * - The service works with actual environment variables
 */
@DisplayName("Platform-Based Licensing Configuration Tests")
class PlatformBasedLicensingConfigTest {

    @Nested
    @DisplayName("PlatformDetectionService Tests")
    class PlatformDetectionServiceTests {
        
        @Test
        @DisplayName("Should detect platform based on ACTOR_ID environment variable")
        void shouldDetectPlatformBasedOnActorId() {
            // Given
            PlatformDetectionService service = new PlatformDetectionService();
            
            // When - Check actual environment
            String actorId = System.getenv("ACTOR_ID");
            boolean isApify = service.isApifyPlatform();
            boolean isLocal = service.isLocalDeployment();
            String platformName = service.getPlatformName();
            
            // Then - Verify consistency
            if (actorId != null && !actorId.isBlank()) {
                // If ACTOR_ID is set, should detect Apify
                assertThat(isApify).isTrue();
                assertThat(isLocal).isFalse();
                assertThat(platformName).isEqualTo("apify");
            } else {
                // If ACTOR_ID is not set, should detect local
                assertThat(isApify).isFalse();
                assertThat(isLocal).isTrue();
                assertThat(platformName).isEqualTo("local");
            }
        }
        
        @Test
        @DisplayName("Platform name should be consistent with detection")
        void platformNameShouldBeConsistent() {
            // Given
            PlatformDetectionService service = new PlatformDetectionService();
            
            // When
            boolean isApify = service.isApifyPlatform();
            String platformName = service.getPlatformName();
            
            // Then
            if (isApify) {
                assertThat(platformName).isEqualTo("apify");
            } else {
                assertThat(platformName).isEqualTo("local");
            }
        }
        
        @Test
        @DisplayName("isLocalDeployment should be opposite of isApifyPlatform")
        void isLocalDeploymentShouldBeOpposite() {
            // Given
            PlatformDetectionService service = new PlatformDetectionService();
            
            // When
            boolean isApify = service.isApifyPlatform();
            boolean isLocal = service.isLocalDeployment();
            
            // Then
            assertThat(isLocal).isEqualTo(!isApify);
        }
    }
    
}
