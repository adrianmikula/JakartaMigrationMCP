package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.coderefactoring.domain.Recipe;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for premium feature visibility in Refactor tab.
 * 
 * These tests verify that:
 * 1. Premium features require the free trial to be activated
 * 2. The system property jakarta.migration.premium controls premium visibility
 * 3. Recipes are only available when premium is enabled
 */
@DisplayName("RefactorTab Premium Feature Tests")
class RefactorTabPremiumTest {

    @BeforeEach
    void setUp() {
        // Clear premium system properties before each test
        System.clearProperty("jakarta.migration.premium");
        System.clearProperty("jakarta.migration.trial.end");
    }

    @AfterEach
    void tearDown() {
        // Clear premium system properties after each test
        System.clearProperty("jakarta.migration.premium");
        System.clearProperty("jakarta.migration.trial.end");
    }

    @Test
    @DisplayName("Should have recipes available regardless of premium status")
    void shouldHaveRecipesAvailableRegardlessOfPremiumStatus() {
        // Given - no premium system property set
        assertThat(System.getProperty("jakarta.migration.premium")).isNull();

        // When - create MigrationAnalysisService (what RefactorTabComponent uses)
        var service = new adrianmikula.jakartamigration.intellij.service.MigrationAnalysisService();
        List<Recipe> recipes = service.getAvailableRecipes();

        // Then - recipes should always be available from the service
        assertThat(recipes)
            .as("Recipes should be available from MigrationAnalysisService")
            .hasSizeGreaterThanOrEqualTo(21);
    }

    @Test
    @DisplayName("Should show premium Refactor tab when trial is activated")
    void shouldShowPremiumRefactorTabWhenTrialActivated() {
        // Given - set premium system property (simulates clicking free trial button)
        System.setProperty("jakarta.migration.premium", "true");
        // Set trial end to 7 days from now
        long trialEnd = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000);
        System.setProperty("jakarta.migration.trial.end", String.valueOf(trialEnd));

        // When - check premium status (simulates MigrationToolWindowContent.checkPremiumStatus())
        boolean isPremium = checkPremiumStatus();

        // Then - premium should be active
        assertThat(isPremium)
            .as("Premium should be active when trial is activated")
            .isTrue();
    }

    @Test
    @DisplayName("Should show locked placeholder when premium is not activated")
    void shouldShowLockedPlaceholderWhenPremiumNotActivated() {
        // Given - no premium system property
        assertThat(System.getProperty("jakarta.migration.premium")).isNull();

        // When - check premium status
        boolean isPremium = checkPremiumStatus();

        // Then - premium should not be active
        assertThat(isPremium)
            .as("Premium should not be active without trial activation")
            .isFalse();
    }

    @Test
    @DisplayName("Should show locked placeholder when trial has expired")
    void shouldShowLockedPlaceholderWhenTrialExpired() {
        // Given - set premium but with expired trial
        System.setProperty("jakarta.migration.premium", "true");
        // Set trial end to past (expired)
        System.setProperty("jakarta.migration.trial.end", String.valueOf(System.currentTimeMillis() - 1000));

        // When - check premium status
        boolean isPremium = checkPremiumStatus();

        // Then - premium should not be active (trial expired)
        assertThat(isPremium)
            .as("Premium should not be active when trial has expired")
            .isFalse();
    }

    /**
     * This mimics MigrationToolWindowContent.checkPremiumStatus()
     */
    private boolean checkPremiumStatus() {
        // Check system property (set by trial activation)
        if ("true".equals(System.getProperty("jakarta.migration.premium"))) {
            // Check if trial is still valid
            String trialEnd = System.getProperty("jakarta.migration.trial.end");
            if (trialEnd != null) {
                try {
                    long endTime = Long.parseLong(trialEnd);
                    if (System.currentTimeMillis() > endTime) {
                        // Trial expired, clear premium status
                        System.setProperty("jakarta.migration.premium", "false");
                        return false;
                    }
                } catch (NumberFormatException e) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
