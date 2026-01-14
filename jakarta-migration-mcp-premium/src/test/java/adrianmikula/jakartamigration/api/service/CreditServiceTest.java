package adrianmikula.jakartamigration.api.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for CreditService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreditService Unit Tests")
class CreditServiceTest {

    private CreditService creditService;
    private static final String TEST_LICENSE_KEY = "PREMIUM-test-key-123";

    @BeforeEach
    void setUp() {
        creditService = new CreditService();
    }

    @Test
    @DisplayName("Should return zero balance for non-existent license key")
    void shouldReturnZeroBalanceForNonExistentLicenseKey() {
        // When
        int balance = creditService.getBalance("non-existent-key");

        // Then
        assertThat(balance).isZero();
    }

    @Test
    @DisplayName("Should return zero balance for null license key")
    void shouldReturnZeroBalanceForNullLicenseKey() {
        // When
        int balance = creditService.getBalance(null);

        // Then
        assertThat(balance).isZero();
    }

    @Test
    @DisplayName("Should return zero balance for blank license key")
    void shouldReturnZeroBalanceForBlankLicenseKey() {
        // When
        int balance = creditService.getBalance("   ");

        // Then
        assertThat(balance).isZero();
    }

    @Test
    @DisplayName("Should initialize credits for new license key")
    void shouldInitializeCreditsForNewLicenseKey() {
        // When
        creditService.initializeCredits(TEST_LICENSE_KEY, 50);

        // Then
        assertThat(creditService.getBalance(TEST_LICENSE_KEY)).isEqualTo(50);
    }

    @Test
    @DisplayName("Should not override existing credits when initializing")
    void shouldNotOverrideExistingCreditsWhenInitializing() {
        // Given
        creditService.initializeCredits(TEST_LICENSE_KEY, 50);
        int initialBalance = creditService.getBalance(TEST_LICENSE_KEY);

        // When
        creditService.initializeCredits(TEST_LICENSE_KEY, 100);

        // Then
        assertThat(creditService.getBalance(TEST_LICENSE_KEY)).isEqualTo(initialBalance);
    }

    @Test
    @DisplayName("Should consume credits successfully")
    void shouldConsumeCreditsSuccessfully() {
        // Given
        creditService.initializeCredits(TEST_LICENSE_KEY, 50);

        // When
        String transactionId = creditService.consumeCredits(TEST_LICENSE_KEY, 10, "testTool");

        // Then
        assertThat(transactionId).isNotNull();
        assertThat(creditService.getBalance(TEST_LICENSE_KEY)).isEqualTo(40);
    }

    @Test
    @DisplayName("Should return null when consuming more credits than available")
    void shouldReturnNullWhenConsumingMoreCreditsThanAvailable() {
        // Given
        creditService.initializeCredits(TEST_LICENSE_KEY, 10);

        // When
        String transactionId = creditService.consumeCredits(TEST_LICENSE_KEY, 20, "testTool");

        // Then
        assertThat(transactionId).isNull();
        assertThat(creditService.getBalance(TEST_LICENSE_KEY)).isEqualTo(10);
    }

    @Test
    @DisplayName("Should return null when consuming credits with null license key")
    void shouldReturnNullWhenConsumingCreditsWithNullLicenseKey() {
        // When
        String transactionId = creditService.consumeCredits(null, 10, "testTool");

        // Then
        assertThat(transactionId).isNull();
    }

    @Test
    @DisplayName("Should return null when consuming credits with blank license key")
    void shouldReturnNullWhenConsumingCreditsWithBlankLicenseKey() {
        // When
        String transactionId = creditService.consumeCredits("   ", 10, "testTool");

        // Then
        assertThat(transactionId).isNull();
    }

    @Test
    @DisplayName("Should return null when consuming zero credits")
    void shouldReturnNullWhenConsumingZeroCredits() {
        // Given
        creditService.initializeCredits(TEST_LICENSE_KEY, 50);

        // When
        String transactionId = creditService.consumeCredits(TEST_LICENSE_KEY, 0, "testTool");

        // Then
        assertThat(transactionId).isNull();
    }

    @Test
    @DisplayName("Should return null when consuming negative credits")
    void shouldReturnNullWhenConsumingNegativeCredits() {
        // Given
        creditService.initializeCredits(TEST_LICENSE_KEY, 50);

        // When
        String transactionId = creditService.consumeCredits(TEST_LICENSE_KEY, -5, "testTool");

        // Then
        assertThat(transactionId).isNull();
    }

    @Test
    @DisplayName("Should handle multiple concurrent credit consumptions")
    void shouldHandleMultipleConcurrentCreditConsumptions() {
        // Given
        creditService.initializeCredits(TEST_LICENSE_KEY, 100);

        // When
        String tx1 = creditService.consumeCredits(TEST_LICENSE_KEY, 10, "tool1");
        String tx2 = creditService.consumeCredits(TEST_LICENSE_KEY, 20, "tool2");
        String tx3 = creditService.consumeCredits(TEST_LICENSE_KEY, 30, "tool3");

        // Then
        assertThat(tx1).isNotNull();
        assertThat(tx2).isNotNull();
        assertThat(tx3).isNotNull();
        assertThat(creditService.getBalance(TEST_LICENSE_KEY)).isEqualTo(40);
    }

    @Test
    @DisplayName("Should sync credits successfully")
    void shouldSyncCreditsSuccessfully() {
        // Given
        creditService.initializeCredits(TEST_LICENSE_KEY, 50);

        // When
        boolean synced = creditService.syncCredits(TEST_LICENSE_KEY, 100);

        // Then
        assertThat(synced).isTrue();
        assertThat(creditService.getBalance(TEST_LICENSE_KEY)).isEqualTo(100);
    }

    @Test
    @DisplayName("Should return false when syncing credits with null license key")
    void shouldReturnFalseWhenSyncingCreditsWithNullLicenseKey() {
        // When
        boolean synced = creditService.syncCredits(null, 100);

        // Then
        assertThat(synced).isFalse();
    }

    @Test
    @DisplayName("Should return false when syncing credits with blank license key")
    void shouldReturnFalseWhenSyncingCreditsWithBlankLicenseKey() {
        // When
        boolean synced = creditService.syncCredits("   ", 100);

        // Then
        assertThat(synced).isFalse();
    }

    @Test
    @DisplayName("Should return false when syncing negative credits")
    void shouldReturnFalseWhenSyncingNegativeCredits() {
        // Given
        creditService.initializeCredits(TEST_LICENSE_KEY, 50);

        // When
        boolean synced = creditService.syncCredits(TEST_LICENSE_KEY, -10);

        // Then
        assertThat(synced).isFalse();
        assertThat(creditService.getBalance(TEST_LICENSE_KEY)).isEqualTo(50);
    }

    @Test
    @DisplayName("Should update last sync time when syncing credits")
    void shouldUpdateLastSyncTimeWhenSyncingCredits() throws InterruptedException {
        // Given
        creditService.initializeCredits(TEST_LICENSE_KEY, 50);
        Instant beforeSync = creditService.getLastSync(TEST_LICENSE_KEY);
        
        Thread.sleep(10); // Small delay to ensure different timestamps

        // When
        creditService.syncCredits(TEST_LICENSE_KEY, 100);
        Instant afterSync = creditService.getLastSync(TEST_LICENSE_KEY);

        // Then
        assertThat(afterSync).isAfter(beforeSync);
    }

    @Test
    @DisplayName("Should update last sync time when consuming credits")
    void shouldUpdateLastSyncTimeWhenConsumingCredits() throws InterruptedException {
        // Given
        creditService.initializeCredits(TEST_LICENSE_KEY, 50);
        Instant beforeConsume = creditService.getLastSync(TEST_LICENSE_KEY);
        
        Thread.sleep(10); // Small delay to ensure different timestamps

        // When
        creditService.consumeCredits(TEST_LICENSE_KEY, 10, "testTool");
        Instant afterConsume = creditService.getLastSync(TEST_LICENSE_KEY);

        // Then
        assertThat(afterConsume).isAfter(beforeConsume);
    }

    @Test
    @DisplayName("Should return null for last sync when license key does not exist")
    void shouldReturnNullForLastSyncWhenLicenseKeyDoesNotExist() {
        // When
        Instant lastSync = creditService.getLastSync("non-existent-key");

        // Then
        assertThat(lastSync).isNull();
    }

    @Test
    @DisplayName("Should handle exact balance consumption")
    void shouldHandleExactBalanceConsumption() {
        // Given
        creditService.initializeCredits(TEST_LICENSE_KEY, 50);

        // When
        String transactionId = creditService.consumeCredits(TEST_LICENSE_KEY, 50, "testTool");

        // Then
        assertThat(transactionId).isNotNull();
        assertThat(creditService.getBalance(TEST_LICENSE_KEY)).isZero();
    }

    @Test
    @DisplayName("Should generate unique transaction IDs")
    void shouldGenerateUniqueTransactionIds() {
        // Given
        creditService.initializeCredits(TEST_LICENSE_KEY, 100);

        // When
        String tx1 = creditService.consumeCredits(TEST_LICENSE_KEY, 10, "tool1");
        String tx2 = creditService.consumeCredits(TEST_LICENSE_KEY, 10, "tool2");
        String tx3 = creditService.consumeCredits(TEST_LICENSE_KEY, 10, "tool3");

        // Then
        assertThat(tx1).isNotEqualTo(tx2);
        assertThat(tx2).isNotEqualTo(tx3);
        assertThat(tx1).isNotEqualTo(tx3);
    }
}

