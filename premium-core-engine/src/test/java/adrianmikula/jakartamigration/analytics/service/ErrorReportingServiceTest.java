package adrianmikula.jakartamigration.analytics.service;

import adrianmikula.jakartamigration.analytics.config.SupabaseConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ErrorReportingService.
 */
@ExtendWith(MockitoExtension.class)
class ErrorReportingServiceTest {

    @Mock
    private UserIdentificationService userIdentificationService;

    @Mock
    private SupabaseConfig supabaseConfig;

    private ErrorReportingService errorReportingService;

    @BeforeEach
    void setUp() {
        when(userIdentificationService.isErrorReportingEnabled()).thenReturn(true);
        when(userIdentificationService.getAnonymousUserId()).thenReturn("test-user-id");
        
        errorReportingService = new ErrorReportingService(userIdentificationService);
    }

    @Test
    void shouldReportError() {
        // Given
        RuntimeException testException = new RuntimeException("Test error message");

        // When
        errorReportingService.reportError(testException);

        // Then
        assertThat(errorReportingService.getQueueSize()).isEqualTo(1);
        verify(userIdentificationService, atLeastOnce()).getAnonymousUserId();
    }

    @Test
    void shouldReportErrorWithContext() {
        // Given
        IllegalArgumentException testException = new IllegalArgumentException("Invalid argument");
        String context = "user_action";

        // When
        errorReportingService.reportError(testException, context);

        // Then
        assertThat(errorReportingService.getQueueSize()).isEqualTo(1);
        verify(userIdentificationService, atLeastOnce()).getAnonymousUserId();
    }

    @Test
    void shouldNotReportWhenErrorReportingDisabled() {
        // Given
        when(userIdentificationService.isErrorReportingEnabled()).thenReturn(false);
        ErrorReportingService disabledService = new ErrorReportingService(userIdentificationService);
        RuntimeException testException = new RuntimeException("Test error");

        // When
        disabledService.reportError(testException);

        // Then
        assertThat(disabledService.getQueueSize()).isEqualTo(0);
        
        // Clean up
        disabledService.close();
    }

    @Test
    void shouldSetCurrentTab() {
        // When
        errorReportingService.setCurrentTab("Dependencies");

        // Then
        // No direct assertion possible as currentTab is private
        // This test mainly ensures no exception is thrown
        assertThat(errorReportingService).isNotNull();
    }

    @Test
    void shouldProcessBatchWhenFlushCalled() {
        // Given
        RuntimeException testException = new RuntimeException("Test error");
        errorReportingService.reportError(testException);

        // When
        errorReportingService.flush();

        // Then
        assertThat(errorReportingService.getQueueSize()).isEqualTo(0);
    }

    @Test
    void shouldHandleMultipleErrorsInQueue() {
        // Given
        RuntimeException error1 = new RuntimeException("First error");
        IllegalArgumentException error2 = new IllegalArgumentException("Second error");
        IllegalStateException error3 = new IllegalStateException("Third error");

        // When
        errorReportingService.reportError(error1);
        errorReportingService.reportError(error2);
        errorReportingService.reportError(error3);

        // Then
        assertThat(errorReportingService.getQueueSize()).isEqualTo(3);
    }

    @Test
    void shouldCloseCleanly() throws InterruptedException {
        // Given
        RuntimeException testException = new RuntimeException("Test error");
        errorReportingService.reportError(testException);

        // When
        errorReportingService.close();

        // Then
        assertThat(errorReportingService.getQueueSize()).isEqualTo(0);
    }
}
