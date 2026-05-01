package adrianmikula.jakartamigration.analytics.util;

import adrianmikula.jakartamigration.analytics.model.ErrorReport;
import adrianmikula.jakartamigration.analytics.model.UsageEvent;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Factory for creating test data for usage events and error reports.
 * Provides deterministic and random test data generation for comprehensive testing.
 */
public class TestDataFactory {
    
    private static final String TEST_USER_ID = "test-user-12345";
    private static final String TEST_PLUGIN_VERSION = "1.0.0-test";
    private static final String TEST_CURRENT_TAB = "Dependencies";
    private static final String TEST_ENVIRONMENT = "test";
    
    /**
     * Creates a basic credit usage event.
     */
    public static UsageEvent createCreditUsageEvent() {
        return UsageEvent.creditUsed(TEST_USER_ID, TEST_CURRENT_TAB, "scan_button", TEST_PLUGIN_VERSION, TEST_ENVIRONMENT);
    }
    
    /**
     * Creates a credit usage event with specified trigger action.
     */
    public static UsageEvent createCreditUsageEvent(String triggerAction) {
        return UsageEvent.creditUsed(TEST_USER_ID, TEST_CURRENT_TAB, triggerAction, TEST_PLUGIN_VERSION, TEST_ENVIRONMENT);
    }
    
    /**
     * Creates an upgrade click event.
     */
    public static UsageEvent createUpgradeClickEvent() {
        return UsageEvent.upgradeClicked(TEST_USER_ID, "truncation_notice", TEST_CURRENT_TAB, TEST_PLUGIN_VERSION, TEST_ENVIRONMENT);
    }
    
    /**
     * Creates an upgrade click event with specified source.
     */
    public static UsageEvent createUpgradeClickEvent(String source) {
        return UsageEvent.upgradeClicked(TEST_USER_ID, source, TEST_CURRENT_TAB, TEST_PLUGIN_VERSION, TEST_ENVIRONMENT);
    }
    
    /**
     * Creates a usage event with custom parameters.
     */
    public static UsageEvent createCustomUsageEvent(String userId, UsageEvent.EventType eventType, 
                                               String currentUiTab, String triggerAction, String pluginVersion, Map<String, Object> eventData) {
        return UsageEvent.builder()
            .userId(userId)
            .eventType(eventType)
            .currentUiTab(currentUiTab)
            .triggerAction(triggerAction)
            .pluginVersion(pluginVersion)
            .eventData(eventData)
            .timestamp(Instant.now())
            .build();
    }
    
    /**
     * Creates a usage event with specific timestamp.
     */
    public static UsageEvent createUsageEventWithTimestamp(Instant timestamp) {
        return UsageEvent.builder()
            .userId(TEST_USER_ID)
            .eventType(UsageEvent.EventType.CREDIT_USED)
            .currentUiTab(TEST_CURRENT_TAB)
            .triggerAction("scan_button")
            .pluginVersion(TEST_PLUGIN_VERSION)
            .timestamp(timestamp)
            .build();
    }
    
    /**
     * Creates a basic error report.
     */
    public static ErrorReport createErrorReport() {
        RuntimeException exception = new RuntimeException("Test error message");
        return ErrorReport.fromException(TEST_USER_ID, TEST_PLUGIN_VERSION, TEST_CURRENT_TAB, exception, TEST_ENVIRONMENT);
    }
    
    /**
     * Creates an error report from specified exception.
     */
    public static ErrorReport createErrorReport(Throwable exception) {
        return ErrorReport.fromException(TEST_USER_ID, TEST_PLUGIN_VERSION, TEST_CURRENT_TAB, exception, TEST_ENVIRONMENT);
    }
    
    /**
     * Creates an error report with custom parameters.
     */
    public static ErrorReport createCustomErrorReport(String userId, String pluginVersion, 
                                                  String currentTab, Throwable exception) {
        return ErrorReport.fromException(userId, pluginVersion, currentTab, exception, TEST_ENVIRONMENT);
    }
    
    /**
     * Creates an error report with specific timestamp.
     */
    public static ErrorReport createErrorReportWithTimestamp(Instant timestamp) {
        RuntimeException exception = new RuntimeException("Test error message");
        ErrorReport report = ErrorReport.fromException(TEST_USER_ID, TEST_PLUGIN_VERSION, TEST_CURRENT_TAB, exception, TEST_ENVIRONMENT);
        // Create a new ErrorReport with custom timestamp using builder
        return ErrorReport.builder()
            .userId(report.getUserId())
            .pluginVersion(report.getPluginVersion())
            .currentTab(report.getCurrentTab())
            .errorType(report.getErrorType())
            .errorMessage(report.getErrorMessage())
            .stackTrace(report.getStackTrace())
            .environment(report.getEnvironment())
            .timestamp(timestamp)
            .build();
    }
    
    /**
     * Creates a large error report for testing size limits.
     */
    public static ErrorReport createLargeErrorReport() {
        String longMessage = "This is a very long error message ".repeat(1000);
        RuntimeException exception = new RuntimeException(longMessage);
        return ErrorReport.fromException(TEST_USER_ID, TEST_PLUGIN_VERSION, TEST_CURRENT_TAB, exception, TEST_ENVIRONMENT);
    }
    
    /**
     * Creates an error report with special characters.
     */
    public static ErrorReport createErrorReportWithSpecialChars() {
        String message = "Error with special chars: !@#$%^&*()_+-=[]{}|;':\",./<>?测试🚀📊";
        RuntimeException exception = new RuntimeException(message);
        return ErrorReport.fromException(TEST_USER_ID, TEST_PLUGIN_VERSION, TEST_CURRENT_TAB, exception, TEST_ENVIRONMENT);
    }
    
    /**
     * Creates a random usage event for load testing.
     */
    public static UsageEvent createRandomUsageEvent() {
        String[] triggerActions = {"scan_button", "export_button", "refresh_button", "settings_button"};
        String[] upgradeSources = {"truncation_notice", "feature_limit", "upgrade_button", "banner"};
        
        ThreadLocalRandom random = ThreadLocalRandom.current();
        
        if (random.nextBoolean()) {
            // Create credit usage event
            String triggerAction = triggerActions[random.nextInt(triggerActions.length)];
            return createCreditUsageEvent(triggerAction);
        } else {
            // Create upgrade click event
            String source = upgradeSources[random.nextInt(upgradeSources.length)];
            return createUpgradeClickEvent(source);
        }
    }
    
    /**
     * Creates a random error report for load testing.
     */
    public static ErrorReport createRandomErrorReport() {
        String[] errorTypes = {
            "RuntimeException", "IllegalArgumentException", "IllegalStateException", 
            "NullPointerException", "IOException", "SecurityException"
        };
        
        String[] messages = {
            "Test error message", "Invalid argument provided", "Illegal state detected",
            "Null pointer encountered", "I/O operation failed", "Security violation"
        };
        
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int index = random.nextInt(errorTypes.length);
        
        try {
            // Create exception dynamically based on type
            Class<?> exceptionClass = Class.forName("java.lang." + errorTypes[index]);
            return createErrorReport((Throwable) exceptionClass
                .getConstructor(String.class)
                .newInstance(messages[index]));
        } catch (Exception e) {
            // Fallback to RuntimeException if dynamic creation fails
            return createErrorReport(new RuntimeException(messages[index]));
        }
    }
    
    /**
     * Creates multiple usage events for batch testing.
     */
    public static UsageEvent[] createUsageEventsBatch(int count) {
        UsageEvent[] events = new UsageEvent[count];
        for (int i = 0; i < count; i++) {
            events[i] = createRandomUsageEvent();
        }
        return events;
    }
    
    /**
     * Creates multiple error reports for batch testing.
     */
    public static ErrorReport[] createErrorReportsBatch(int count) {
        ErrorReport[] reports = new ErrorReport[count];
        for (int i = 0; i < count; i++) {
            reports[i] = createRandomErrorReport();
        }
        return reports;
    }
    
    /**
     * Creates a unique user ID for testing.
     */
    public static String createUniqueUserId() {
        return "test-user-" + UUID.randomUUID().toString();
    }
    
    /**
     * Gets the standard test user ID.
     */
    public static String getTestUserId() {
        return TEST_USER_ID;
    }
    
    /**
     * Gets the standard test plugin version.
     */
    public static String getTestPluginVersion() {
        return TEST_PLUGIN_VERSION;
    }
    
    /**
     * Gets the standard test current tab.
     */
    public static String getTestCurrentTab() {
        return TEST_CURRENT_TAB;
    }
}
