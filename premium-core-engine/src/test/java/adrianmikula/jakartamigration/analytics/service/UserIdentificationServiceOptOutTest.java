package adrianmikula.jakartamigration.analytics.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for UserIdentificationService opt-out functionality.
 */
@Tag("slow")
@ExtendWith(MockitoExtension.class)
class UserIdentificationServiceOptOutTest {

    @TempDir
    Path tempDir;

    private UserIdentificationService userIdentificationService;

    @BeforeEach
    void setUp() {
        userIdentificationService = new UserIdentificationService(tempDir, null);
    }

    @Test
    void shouldDefaultUsageMetricsToEnabled() {
        // When
        boolean usageMetricsOptedOut = userIdentificationService.isUsageMetricsOptedOut();

        // Then
        assertThat(usageMetricsOptedOut).isFalse();
    }

    @Test
    void shouldDefaultErrorReportingToEnabled() {
        // When
        boolean errorReportingOptedOut = userIdentificationService.isErrorReportingOptedOut();

        // Then
        assertThat(errorReportingOptedOut).isFalse();
    }

    @Test
    void shouldAllowOptingOutOfUsageMetrics() {
        // When
        userIdentificationService.setUsageMetricsEnabled(false);

        // Then
        assertThat(userIdentificationService.isUsageMetricsOptedOut()).isTrue();
    }

    @Test
    void shouldAllowOptingBackIntoUsageMetrics() {
        // Given
        userIdentificationService.setUsageMetricsEnabled(false);

        // When
        userIdentificationService.setUsageMetricsEnabled(true);

        // Then
        assertThat(userIdentificationService.isUsageMetricsOptedOut()).isFalse();
    }

    @Test
    void shouldAllowOptingOutOfErrorReporting() {
        // When
        userIdentificationService.setErrorReportingEnabled(false);

        // Then
        assertThat(userIdentificationService.isErrorReportingOptedOut()).isTrue();
    }

    @Test
    void shouldAllowOptingBackIntoErrorReporting() {
        // Given
        userIdentificationService.setErrorReportingEnabled(false);

        // When
        userIdentificationService.setErrorReportingEnabled(true);

        // Then
        assertThat(userIdentificationService.isErrorReportingOptedOut()).isFalse();
    }

    @Test
    void shouldPersistOptOutSettingsAcrossServiceInstances() {
        // Given
        userIdentificationService.setUsageMetricsEnabled(false);
        userIdentificationService.setErrorReportingEnabled(false);
        userIdentificationService.close();

        // When
        UserIdentificationService newService = new UserIdentificationService(tempDir, null);

        // Then
        assertThat(newService.isUsageMetricsOptedOut()).isTrue();
        assertThat(newService.isErrorReportingOptedOut()).isTrue();
        
        // Clean up
        newService.close();
    }
}
