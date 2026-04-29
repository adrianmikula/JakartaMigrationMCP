package adrianmikula.jakartamigration.analytics.service;

import adrianmikula.jakartamigration.analytics.config.SupabaseConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserIdentificationService.
 */
@Tag("slow")
@ExtendWith(MockitoExtension.class)
class UserIdentificationServiceTest {

    @TempDir
    Path tempDir;

    @Mock
    private SupabaseConfig supabaseConfig;

    private UserIdentificationService userIdentificationService;

    @BeforeEach
    void setUp() {
        when(supabaseConfig.isAnalyticsEnabled()).thenReturn(true);
        when(supabaseConfig.isErrorReportingEnabled()).thenReturn(true);
        when(supabaseConfig.isConfigured()).thenReturn(true);
        
        userIdentificationService = new UserIdentificationService(tempDir, supabaseConfig);
    }

    @Test
    void shouldGenerateNewUserIdOnFirstUse() {
        // When
        String userId = userIdentificationService.getAnonymousUserId();

        // Then
        assertThat(userId).isNotNull();
        assertThat(userId).hasSize(36); // UUID length
    }

    @Test
    void shouldReuseExistingUserId() {
        // Given
        UserIdentificationService firstService = new UserIdentificationService(tempDir, supabaseConfig);
        String firstUserId = firstService.getAnonymousUserId();
        firstService.close();

        // When
        UserIdentificationService secondService = new UserIdentificationService(tempDir, supabaseConfig);
        String secondUserId = secondService.getAnonymousUserId();
        secondService.close();

        // Then
        assertThat(secondUserId).isEqualTo(firstUserId);
    }

    @Test
    void shouldUpdateLastSeenTimestamp() throws InterruptedException {
        // Given
        long initialLastSeen = userIdentificationService.getLastSeenTimestamp();
        Thread.sleep(10); // Small delay

        // When
        userIdentificationService.close(); // This should update last seen
        UserIdentificationService newService = new UserIdentificationService(tempDir, supabaseConfig);
        long updatedLastSeen = newService.getLastSeenTimestamp();
        newService.close();

        // Then
        assertThat(updatedLastSeen).isGreaterThan(initialLastSeen);
    }

    @Test
    void shouldReturnFirstSeenTimestamp() {
        // When
        long firstSeen = userIdentificationService.getFirstSeenTimestamp();

        // Then
        assertThat(firstSeen).isGreaterThan(0);
    }

    @Test
    void shouldCheckAnalyticsEnabled() {
        // Given
        when(supabaseConfig.isAnalyticsEnabled()).thenReturn(false);
        UserIdentificationService disabledService = new UserIdentificationService(tempDir, supabaseConfig);

        // When
        boolean analyticsEnabled = disabledService.isAnalyticsEnabled();

        // Then
        assertThat(analyticsEnabled).isFalse();
        
        // Clean up
        disabledService.close();
    }

    @Test
    void shouldCheckErrorReportingEnabled() {
        // Given
        when(supabaseConfig.isErrorReportingEnabled()).thenReturn(false);
        UserIdentificationService disabledService = new UserIdentificationService(tempDir, supabaseConfig);

        // When
        boolean errorReportingEnabled = disabledService.isErrorReportingEnabled();

        // Then
        assertThat(errorReportingEnabled).isFalse();
        
        // Clean up
        disabledService.close();
    }

    @Test
    void shouldHandleConfigurationNotConfigured() {
        // Given
        when(supabaseConfig.isConfigured()).thenReturn(false);
        UserIdentificationService unconfiguredService = new UserIdentificationService(tempDir, supabaseConfig);

        // When
        boolean analyticsEnabled = unconfiguredService.isAnalyticsEnabled();
        boolean errorReportingEnabled = unconfiguredService.isErrorReportingEnabled();

        // Then
        assertThat(analyticsEnabled).isFalse();
        assertThat(errorReportingEnabled).isFalse();
        
        // Clean up
        unconfiguredService.close();
    }

    @Test
    void shouldCloseCleanly() {
        // When
        userIdentificationService.close();

        // Then
        // No exception should be thrown
        assertThat(true).isTrue();
    }
}
