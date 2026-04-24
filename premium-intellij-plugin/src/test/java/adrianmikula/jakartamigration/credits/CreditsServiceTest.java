package adrianmikula.jakartamigration.credits;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class to verify CreditsService functionality for scan credit consumption.
 */
public class CreditsServiceTest {

    private CreditsService creditsService;
    private Path testStoragePath;

    @BeforeEach
    public void setUp() throws Exception {
        testStoragePath = Path.of(System.getProperty("java.io.tmpdir")).resolve("test-credits-" + System.currentTimeMillis());
        creditsService = new CreditsService(testStoragePath);
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (creditsService != null) {
            creditsService.close();
        }
    }

    @Test
    public void testCreditsServiceInitialization() {
        assertNotNull(creditsService);
        assertTrue(creditsService.hasCredits(CreditType.ACTIONS));
        assertEquals(10, creditsService.getRemainingCredits(CreditType.ACTIONS));
        assertEquals(10, creditsService.getCreditLimit(CreditType.ACTIONS));
    }

    @Test
    public void testCreditConsumptionForScan() {
        // Test initial state
        int initialCredits = creditsService.getRemainingCredits(CreditType.ACTIONS);
        assertTrue(initialCredits > 0);
        
        // Simulate consuming a credit for scan
        boolean creditConsumed = creditsService.useCredit(CreditType.ACTIONS);
        assertTrue(creditConsumed);
        
        // Verify credit was consumed
        int remainingCredits = creditsService.getRemainingCredits(CreditType.ACTIONS);
        assertEquals(initialCredits - 1, remainingCredits);
        
        // Verify used credits tracking
        assertEquals(1, creditsService.getUsedCredits(CreditType.ACTIONS));
    }

    @Test
    public void testCreditExhaustion() {
        // Consume all credits
        while (creditsService.hasCredits(CreditType.ACTIONS)) {
            creditsService.useCredit(CreditType.ACTIONS);
        }
        
        // Verify no credits remaining
        assertFalse(creditsService.hasCredits(CreditType.ACTIONS));
        assertEquals(0, creditsService.getRemainingCredits(CreditType.ACTIONS));
        assertEquals(10, creditsService.getUsedCredits(CreditType.ACTIONS));
        
        // Test consumption when exhausted
        boolean creditConsumedWhenExhausted = creditsService.useCredit(CreditType.ACTIONS);
        assertFalse(creditConsumedWhenExhausted);
    }

    @Test
    public void testMultipleCreditTypes() {
        // Test that unified credit system works correctly
        assertTrue(creditsService.hasCredits(CreditType.ACTIONS));
        
        // Consume from ACTIONS type (used for all operations)
        creditsService.useCredit(CreditType.ACTIONS);
        
        // Verify credit was consumed correctly
        assertEquals(9, creditsService.getRemainingCredits(CreditType.ACTIONS));
        assertEquals(1, creditsService.getUsedCredits(CreditType.ACTIONS));
    }

    @Test
    public void testCreditRefresh() {
        // Consume some credits
        creditsService.useCredit(CreditType.ACTIONS);
        creditsService.useCredit(CreditType.ACTIONS);
        
        assertEquals(8, creditsService.getRemainingCredits(CreditType.ACTIONS));
        assertEquals(2, creditsService.getUsedCredits(CreditType.ACTIONS));
        
        // Refresh cache
        creditsService.refreshCache();
        
        // Verify data is still accurate after refresh
        assertEquals(8, creditsService.getRemainingCredits(CreditType.ACTIONS));
        assertEquals(2, creditsService.getUsedCredits(CreditType.ACTIONS));
    }
}
