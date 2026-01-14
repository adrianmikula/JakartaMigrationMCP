package adrianmikula.jakartamigration.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PlatformBasedLicensingPostProcessor.
 * 
 * Verifies that:
 * - Platform detection works correctly
 * - Environment properties are modified correctly based on platform detection
 * - Property sources are added with correct priority
 * - Environment variable overrides are respected
 */
@DisplayName("PlatformBasedLicensingPostProcessor Unit Tests")
class PlatformBasedLicensingPostProcessorTest {

    private PlatformBasedLicensingPostProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new PlatformBasedLicensingPostProcessor();
    }

    @AfterEach
    void tearDown() {
        // Clean up any system properties that might have been set
        System.clearProperty("jakarta.migration.apify.enabled");
        System.clearProperty("jakarta.migration.stripe.enabled");
        System.clearProperty("ACTOR_ID");
        System.clearProperty("APIFY_VALIDATION_ENABLED");
        System.clearProperty("STRIPE_VALIDATION_ENABLED");
    }

    @Nested
    @DisplayName("Platform Detection Tests")
    class PlatformDetectionTests {

        @Test
        @DisplayName("Should detect Apify platform when ACTOR_ID is set")
        void shouldDetectApifyPlatform() {
            // Given
            String originalActorId = System.getenv("ACTOR_ID");
            try {
                // Set ACTOR_ID to simulate Apify platform
                System.setProperty("ACTOR_ID", "test-actor-id");
                
                // When
                boolean isApify = (boolean) ReflectionTestUtils.invokeMethod(processor, "isApifyPlatform");
                
                // Then
                assertThat(isApify).isTrue();
            } finally {
                // Restore original value
                if (originalActorId != null) {
                    System.setProperty("ACTOR_ID", originalActorId);
                } else {
                    System.clearProperty("ACTOR_ID");
                }
            }
        }

        @Test
        @DisplayName("Should detect local platform when ACTOR_ID is not set")
        void shouldDetectLocalPlatform() {
            // Given
            String originalActorId = System.getenv("ACTOR_ID");
            try {
                // Clear ACTOR_ID to simulate local deployment
                System.clearProperty("ACTOR_ID");
                
                // When
                boolean isApify = (boolean) ReflectionTestUtils.invokeMethod(processor, "isApifyPlatform");
                
                // Then
                assertThat(isApify).isFalse();
            } finally {
                // Restore original value
                if (originalActorId != null) {
                    System.setProperty("ACTOR_ID", originalActorId);
                }
            }
        }
    }

    @Nested
    @DisplayName("Environment Property Modification Tests")
    class EnvironmentPropertyModificationTests {

        @Test
        @DisplayName("Should set Apify enabled and Stripe disabled on Apify platform")
        void shouldSetApifyEnabledOnApifyPlatform() {
            // Given
            System.setProperty("ACTOR_ID", "test-actor-id");
            System.clearProperty("APIFY_VALIDATION_ENABLED");
            System.clearProperty("STRIPE_VALIDATION_ENABLED");
            
            MockEnvironment mockEnv = new MockEnvironment();
            mockEnv.setProperty("jakarta.migration.apify.enabled", "false"); // Default
            mockEnv.setProperty("jakarta.migration.stripe.enabled", "true"); // Default
            
            ConfigurableApplicationContext mockContext = mock(ConfigurableApplicationContext.class);
            when(mockContext.getEnvironment()).thenReturn(mockEnv);
            ConfigurableListableBeanFactory beanFactory = (ConfigurableListableBeanFactory) mockContext;
            
            // When
            processor.postProcessBeanFactory(beanFactory);
            
            // Then - Property source should be added
            assertThat(mockEnv.getPropertySources().contains("platformBasedLicensingOverrides")).isTrue();
            // The processor should have set apify.enabled=true
            assertThat(mockEnv.getProperty("jakarta.migration.apify.enabled", Boolean.class)).isTrue();
            // The processor should have set stripe.enabled=false
            assertThat(mockEnv.getProperty("jakarta.migration.stripe.enabled", Boolean.class)).isFalse();
        }

        @Test
        @DisplayName("Should set Stripe enabled and Apify disabled on local platform")
        void shouldSetStripeEnabledOnLocalPlatform() {
            // Given
            System.clearProperty("ACTOR_ID");
            System.clearProperty("APIFY_VALIDATION_ENABLED");
            System.clearProperty("STRIPE_VALIDATION_ENABLED");
            
            MockEnvironment mockEnv = new MockEnvironment();
            mockEnv.setProperty("jakarta.migration.apify.enabled", "false");
            mockEnv.setProperty("jakarta.migration.stripe.enabled", "false");
            
            ConfigurableApplicationContext mockContext = mock(ConfigurableApplicationContext.class);
            when(mockContext.getEnvironment()).thenReturn(mockEnv);
            ConfigurableListableBeanFactory beanFactory = (ConfigurableListableBeanFactory) mockContext;
            
            // When
            processor.postProcessBeanFactory(beanFactory);
            
            // Then - Property source should be added
            assertThat(mockEnv.getPropertySources().contains("platformBasedLicensingOverrides")).isTrue();
            // The processor should have set stripe.enabled=true
            assertThat(mockEnv.getProperty("jakarta.migration.stripe.enabled", Boolean.class)).isTrue();
            // The processor should have set apify.enabled=false
            assertThat(mockEnv.getProperty("jakarta.migration.apify.enabled", Boolean.class)).isFalse();
        }

        @Test
        @DisplayName("Should respect APIFY_VALIDATION_ENABLED environment variable override")
        void shouldRespectApifyValidationEnabledOverride() {
            // Given
            System.setProperty("ACTOR_ID", "test-actor-id");
            System.setProperty("APIFY_VALIDATION_ENABLED", "false"); // Explicitly disabled
            
            MockEnvironment mockEnv = new MockEnvironment();
            ConfigurableApplicationContext mockContext = mock(ConfigurableApplicationContext.class);
            when(mockContext.getEnvironment()).thenReturn(mockEnv);
            ConfigurableListableBeanFactory beanFactory = (ConfigurableListableBeanFactory) mockContext;
            
            // When
            processor.postProcessBeanFactory(beanFactory);
            
            // Then - Should not override explicit setting (no property source added)
            // The processor should detect the explicit override and not modify properties
            assertThat(mockEnv.getPropertySources().contains("platformBasedLicensingOverrides")).isFalse();
        }

        @Test
        @DisplayName("Should respect STRIPE_VALIDATION_ENABLED environment variable override")
        void shouldRespectStripeValidationEnabledOverride() {
            // Given
            System.clearProperty("ACTOR_ID");
            System.setProperty("STRIPE_VALIDATION_ENABLED", "false"); // Explicitly disabled
            
            MockEnvironment mockEnv = new MockEnvironment();
            ConfigurableApplicationContext mockContext = mock(ConfigurableApplicationContext.class);
            when(mockContext.getEnvironment()).thenReturn(mockEnv);
            ConfigurableListableBeanFactory beanFactory = (ConfigurableListableBeanFactory) mockContext;
            
            // When
            processor.postProcessBeanFactory(beanFactory);
            
            // Then - Should not override explicit setting
            assertThat(mockEnv.getPropertySources().contains("platformBasedLicensingOverrides")).isFalse();
        }
    }

    @Nested
    @DisplayName("System Properties Fallback Tests")
    class SystemPropertiesFallbackTests {

        @Test
        @DisplayName("Should use System properties fallback when environment is not accessible")
        void shouldUseSystemPropertiesFallback() {
            // Given
            System.clearProperty("ACTOR_ID");
            System.clearProperty("APIFY_VALIDATION_ENABLED");
            System.clearProperty("STRIPE_VALIDATION_ENABLED");
            
            ConfigurableListableBeanFactory beanFactory = mock(ConfigurableListableBeanFactory.class);
            when(beanFactory.containsBean("environment")).thenReturn(false);
            
            // When
            processor.postProcessBeanFactory(beanFactory);
            
            // Then - System properties should be set as fallback
            String stripeEnabled = System.getProperty("jakarta.migration.stripe.enabled");
            assertThat(stripeEnabled).isEqualTo("true"); // Should be enabled for local deployment
        }
    }

    @Nested
    @DisplayName("Property Source Priority Tests")
    class PropertySourcePriorityTests {

        @Test
        @DisplayName("Should add property source with highest priority")
        void shouldAddPropertySourceWithHighestPriority() {
            // Given
            System.clearProperty("ACTOR_ID");
            
            StandardEnvironment env = new StandardEnvironment();
            ConfigurableApplicationContext mockContext = mock(ConfigurableApplicationContext.class);
            when(mockContext.getEnvironment()).thenReturn(env);
            ConfigurableListableBeanFactory beanFactory = (ConfigurableListableBeanFactory) mockContext;
            
            // When
            processor.postProcessBeanFactory(beanFactory);
            
            // Then - Property source should be added and be first (highest priority)
            boolean hasPropertySource = env.getPropertySources().stream()
                .anyMatch(ps -> ps.getName().equals("platformBasedLicensingOverrides"));
            assertThat(hasPropertySource).isTrue();
            
            // Verify it's first in the list (highest priority)
            String firstSourceName = env.getPropertySources().iterator().next().getName();
            assertThat(firstSourceName).isEqualTo("platformBasedLicensingOverrides");
        }
    }
}

