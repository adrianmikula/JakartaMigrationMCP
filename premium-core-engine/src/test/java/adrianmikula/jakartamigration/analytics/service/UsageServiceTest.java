package adrianmikula.jakartamigration.analytics.service;

import adrianmikula.jakartamigration.analytics.config.SupabaseConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UsageService.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UsageServiceTest {

    @Mock
    private UserIdentificationService userIdentificationService;

    @Mock
    private SupabaseConfig supabaseConfig;

    private UsageService usageService;

    @BeforeEach
    void setUp() {
        when(userIdentificationService.isAnalyticsEnabled()).thenReturn(true);
        when(userIdentificationService.getAnonymousUserId()).thenReturn("test-user-id");
        when(supabaseConfig.getAnalyticsBatchSize()).thenReturn(5);
        when(supabaseConfig.getAnalyticsFlushIntervalSeconds()).thenReturn(3600); // Long interval to prevent auto-flush
        
        usageService = new UsageService(userIdentificationService, supabaseConfig);
    }

    @Test
    void shouldTrackCreditUsage() {
        // When
        usageService.trackCreditUsage("basic_scan", "test");

        // Then
        assertThat(usageService.getQueueSize()).isEqualTo(1);
        
        // Verify no immediate processing (batch processing)
        verify(userIdentificationService, atLeastOnce()).getAnonymousUserId();
    }

    @Test
    void shouldTrackUpgradeClick() {
        // When
        usageService.trackUpgradeClick("truncation_notice", "test");

        // Then
        assertThat(usageService.getQueueSize()).isEqualTo(1);
        
        // Verify no immediate processing (batch processing)
        verify(userIdentificationService, atLeastOnce()).getAnonymousUserId();
    }

    @Test
    void shouldNotTrackWhenAnalyticsDisabled() {
        // Given
        when(userIdentificationService.isAnalyticsEnabled()).thenReturn(false);
        UsageService disabledService = new UsageService(userIdentificationService);

        // When
        disabledService.trackCreditUsage("basic_scan", "test");
        disabledService.trackUpgradeClick("truncation_notice", "test");

        // Then
        assertThat(disabledService.getQueueSize()).isEqualTo(0);
        
        // Clean up
        disabledService.close();
    }

    @Test
    void shouldProcessBatchWhenFlushCalled() {
        // Given
        usageService.trackCreditUsage("basic_scan", "test");
        usageService.trackUpgradeClick("truncation_notice", "test");

        // When
        usageService.flush();

        // Then
        assertThat(usageService.getQueueSize()).isEqualTo(0);
    }

    @Test
    void shouldHandleMultipleEventsInQueue() {
        // When
        usageService.trackCreditUsage("basic_scan", "test");
        usageService.trackCreditUsage("advanced_scan", "test");
        usageService.trackUpgradeClick("truncation_notice", "test");
        usageService.trackCreditUsage("pdf_report", "test");

        // Then
        assertThat(usageService.getQueueSize()).isEqualTo(4);
    }

    @Test
    void shouldCloseCleanly() throws InterruptedException {
        // Given
        usageService.trackCreditUsage("basic_scan", "test");

        // When
        usageService.close();

        // Then
        assertThat(usageService.getQueueSize()).isEqualTo(0);
    }
}
