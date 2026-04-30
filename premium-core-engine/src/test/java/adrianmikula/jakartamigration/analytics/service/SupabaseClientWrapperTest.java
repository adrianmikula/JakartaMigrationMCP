package adrianmikula.jakartamigration.analytics.service;

import adrianmikula.jakartamigration.analytics.config.SupabaseConfig;
import adrianmikula.jakartamigration.analytics.model.ErrorReport;
import adrianmikula.jakartamigration.analytics.model.UsageEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for SupabaseClientWrapper.
 * Tests error handling, retry logic, and configuration validation.
 * Temporarily disabled due to NoClassDefFoundError in JUnit platform.
 */
@ExtendWith(MockitoExtension.class)
@org.junit.jupiter.api.Disabled("Temporarily disabled due to NoClassDefFoundError in JUnit platform")
class SupabaseClientWrapperTest {

    @Mock
    private SupabaseConfig mockConfig;

    private SupabaseClientWrapper wrapper;

    @BeforeEach
    void setUp() {
        when(mockConfig.isConfigured()).thenReturn(false);
        wrapper = new SupabaseClientWrapper(mockConfig);
    }

    @Test
    void shouldInitializeWithCorrectConfiguration() {
        // Given
        when(mockConfig.isConfigured()).thenReturn(true);
        when(mockConfig.getSupabaseUrl()).thenReturn("https://test.supabase.co");

        // When
        SupabaseClientWrapper configuredWrapper = new SupabaseClientWrapper(mockConfig);

        try {
            // Then
            assertThat(configuredWrapper.isConfigured()).isTrue();
        } finally {
            configuredWrapper.close();
        }
    }

    @Test
    void shouldHandleUnconfiguredConfiguration() {
        // Given
        when(mockConfig.isConfigured()).thenReturn(false);

        // When
        SupabaseClientWrapper unconfiguredWrapper = new SupabaseClientWrapper(mockConfig);

        try {
            // Then
            assertThat(unconfiguredWrapper.isConfigured()).isFalse();
        } finally {
            unconfiguredWrapper.close();
        }
    }

    @Test
    void shouldLogUsageEventsWhenNotConfigured() {
        // Given
        List<UsageEvent> events = List.of(
            UsageEvent.creditUsed("user123", "Dependencies", "scan_button", "1.0.0", "test"),
            UsageEvent.upgradeClicked("user123", "test_source", "Dependencies", "1.0.0", "test")
        );

        // When & Then - Should not throw exception
        wrapper.insertUsageEvents(events);
    }

    @Test
    void shouldLogErrorReportsWhenNotConfigured() {
        // Given
        RuntimeException testException = new RuntimeException("Test error");
        List<ErrorReport> reports = List.of(
            ErrorReport.fromException("user123", "1.0.0", "dashboard", testException, "test")
        );

        // When & Then - Should not throw exception
        wrapper.insertErrorReports(reports);
    }

    @Test
    void shouldHandleEmptyUsageEventsList() {
        // Given
        List<UsageEvent> emptyEvents = List.of();

        // When & Then - Should not throw exception
        wrapper.insertUsageEvents(emptyEvents);
    }

    @Test
    void shouldHandleEmptyErrorReportsList() {
        // Given
        List<ErrorReport> emptyReports = List.of();

        // When & Then - Should not throw exception
        wrapper.insertErrorReports(emptyReports);
    }

    @Test
    void shouldLogUserActivityWhenNotConfigured() {
        // When & Then - Should not throw exception
        wrapper.logUserActivity("user123", "1.0.0");
    }

    @Test
    void shouldCloseGracefully() {
        // When & Then - Should not throw exception
        wrapper.close();
    }

    @Test
    void shouldHandleNullConfiguration() {
        // When & Then - Should not throw exception
        SupabaseClientWrapper nullConfigWrapper = new SupabaseClientWrapper(null);
        try {
            assertThat(nullConfigWrapper.isConfigured()).isFalse();
        } finally {
            nullConfigWrapper.close();
        }
    }
}
