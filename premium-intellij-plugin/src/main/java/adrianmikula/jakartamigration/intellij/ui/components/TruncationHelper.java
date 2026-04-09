package adrianmikula.jakartamigration.intellij.ui.components;

import adrianmikula.jakartamigration.credits.CreditsService;
import adrianmikula.jakartamigration.intellij.license.CheckLicense;

/**
 * Utility class for handling truncation logic across UI components.
 * Provides consistent truncation behavior for free vs premium users.
 */
public class TruncationHelper {

    private final CreditsService creditsService;

    public TruncationHelper() {
        this.creditsService = new CreditsService();
    }

    /**
     * Checks if truncation mode should be applied.
     * @return true if results should be truncated (free users see truncated results)
     */
    public boolean shouldTruncateResults() {
        boolean isPremium = CheckLicense.isLicensed();
        // Premium users see all results, free users see truncated results
        return !isPremium;
    }

    /**
     * Gets the truncation limit for dependencies from FreemiumConfig.
     * @return the maximum number of dependencies to show when truncating
     */
    public int getDependenciesTruncationLimit() {
        return creditsService.getFreemiumConfig().getDependenciesTruncationLimit();
    }

    /**
     * Gets the truncation limit for dashboard results from FreemiumConfig.
     * @return the maximum number of dashboard results to show when truncating
     */
    public int getDashboardTruncationLimit() {
        return creditsService.getFreemiumConfig().getDashboardTruncationLimit();
    }

    /**
     * Gets the truncation limit for advanced scan results from FreemiumConfig.
     * @return the maximum number of advanced scan results to show when truncating
     */
    public int getAdvancedScanTruncationLimit() {
        return creditsService.getFreemiumConfig().getAdvancedScanTruncationLimit();
    }

    /**
     * Applies truncation to a count value if necessary.
     * @param actualCount the actual number of results
     * @return the count to display (truncated if necessary)
     */
    public int applyTruncation(int actualCount) {
        if (!shouldTruncateResults()) {
            return actualCount;
        }
        int limit = getDashboardTruncationLimit();
        return Math.min(actualCount, limit);
    }

    /**
     * Applies truncation with a specific limit.
     * @param actualCount the actual number of results
     * @param limit the maximum number to show
     * @return the count to display (truncated if necessary)
     */
    public int applyTruncation(int actualCount, int limit) {
        if (!shouldTruncateResults()) {
            return actualCount;
        }
        return Math.min(actualCount, limit);
    }

    /**
     * Formats a count display with truncation indicator if needed.
     * @param actualCount the actual number of results
     * @return formatted string like "5" or "10 of 25" when truncated
     */
    public String formatTruncatedCount(int actualCount) {
        if (!shouldTruncateResults() || actualCount <= getDashboardTruncationLimit()) {
            return String.valueOf(actualCount);
        }
        return applyTruncation(actualCount) + " of " + actualCount;
    }

    /**
     * Formats a truncation message for display.
     * @param shown number of items shown
     * @param total total number of items available
     * @param itemName name of the items (e.g., "dependencies", "results")
     * @return formatted message like "Showing 10 of 25 dependencies"
     */
    public String formatTruncationMessage(int shown, int total, String itemName) {
        return String.format("Showing %d of %d %s", shown, total, itemName);
    }

    /**
     * Gets the default truncation limit for basic analysis (when no credits remain).
     * @return the default limit (10)
     */
    public int getBasicAnalysisTruncationLimit() {
        return 10; // Default limit for basic analysis when credits exhausted
    }
}
