package adrianmikula.jakartamigration.credits;

/**
 * Enum representing credit type available in freemium model.
 * Unified credit system - single credit type for all premium operations.
 */
public enum CreditType {
    /**
     * Actions credits - used for all premium operations (scans, advanced scans, refactoring, reports).
     * Free users get a limited number of action credits (default: 10).
     * When exhausted, users are prompted to upgrade.
     */
    ACTIONS("actions", "Actions", 10);

    private final String key;
    private final String displayName;
    private final int defaultLimit;

    CreditType(String key, String displayName, int defaultLimit) {
        this.key = key;
        this.displayName = displayName;
        this.defaultLimit = defaultLimit;
    }

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getDefaultLimit() {
        return defaultLimit;
    }

    /**
     * Get the CreditType from its key string.
     *
     * @param key the key to look up
     * @return the CreditType, or null if not found
     */
    public static CreditType fromKey(String key) {
        for (CreditType type : values()) {
            if (type.key.equals(key)) {
                return type;
            }
        }
        return null;
    }
}
