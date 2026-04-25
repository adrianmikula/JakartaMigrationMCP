package adrianmikula.jakartamigration.analytics.service;

import adrianmikula.jakartamigration.analytics.config.SupabaseConfig;
import adrianmikula.jakartamigration.analytics.model.ErrorReport;
import adrianmikula.jakartamigration.analytics.model.UsageEvent;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Wrapper class for Supabase client operations with error handling and retry logic.
 * Provides a clean interface for database operations while handling network issues gracefully.
 */
@Slf4j
public class SupabaseClientWrapper implements AutoCloseable {
    
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;
    private static final long RETRY_BACKOFF_MULTIPLIER = 2;
    
    private final SupabaseConfig config;
    private final boolean isConfigured;
    
    public SupabaseClientWrapper(SupabaseConfig config) {
        this.config = config;
        this.isConfigured = config.isConfigured();
        
        if (isConfigured) {
            log.info("SupabaseClientWrapper initialized with URL: {}", maskUrl(config.getSupabaseUrl()));
        } else {
            log.warn("SupabaseClientWrapper initialized but not configured - operations will be logged only");
        }
    }
    
    /**
     * Inserts usage events into the database.
     * Implements retry logic for network failures.
     */
    public void insertUsageEvents(List<UsageEvent> events) {
        if (!isConfigured) {
            logUsageEvents(events);
            return;
        }
        
        if (events.isEmpty()) {
            return;
        }
        
        int attempt = 0;
        Exception lastException = null;
        
        while (attempt < MAX_RETRIES) {
            try {
                StringBuilder eventsJson = new StringBuilder("[");
                
                for (int i = 0; i < events.size(); i++) {
                    UsageEvent event = events.get(i);
                    if (i > 0) eventsJson.append(",");
                    
                    eventsJson.append("{");
                    eventsJson.append("\"user_id\":\"").append(escapeJson(event.getUserId())).append("\",");
                    eventsJson.append("\"event_type\":\"").append(escapeJson(event.getEventType().getValue())).append("\",");
                    eventsJson.append("\"current_ui_tab\":").append(event.getCurrentUiTab() != null ? "\"" + escapeJson(event.getCurrentUiTab()) + "\"" : "null").append(",");
                    eventsJson.append("\"plugin_version\":").append(event.getPluginVersion() != null ? "\"" + escapeJson(event.getPluginVersion()) + "\"" : "null").append(",");
                    eventsJson.append("\"trigger_action\":").append(event.getTriggerAction() != null ? "\"" + escapeJson(event.getTriggerAction()) + "\"" : "null").append(",");
                    
                    // Add event_data if present
                    if (event.getEventData() != null && !event.getEventData().isEmpty()) {
                        eventsJson.append("\"event_data\":{");
                        boolean first = true;
                        for (Map.Entry<String, Object> entry : event.getEventData().entrySet()) {
                            if (!first) eventsJson.append(",");
                            eventsJson.append("\"").append(escapeJson(entry.getKey())).append("\":\"").append(escapeJson(entry.getValue().toString())).append("\"");
                            first = false;
                        }
                        eventsJson.append("}");
                    } else {
                        eventsJson.append("\"event_data\":null");
                    }
                    
                    eventsJson.append(",\"created_at\":\"").append(formatTimestamp(event.getTimestamp())).append("\"");
                    eventsJson.append("}");
                }
                
                eventsJson.append("]");
                
                // Make HTTP POST request to Supabase
                String supabaseUrl = config.getSupabaseUrl() + "/rest/v1/usage_events";
                String apiKey = config.getSupabaseAnonKey();
                
                URL url = new URL(supabaseUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                
                try {
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setRequestProperty("apikey", apiKey);
                    connection.setRequestProperty("Authorization", "Bearer " + apiKey);
                    connection.setRequestProperty("Prefer", "return=minimal");
                    connection.setDoOutput(true);
                    
                    // Send the JSON data
                    byte[] input = eventsJson.toString().getBytes(StandardCharsets.UTF_8);
                    connection.getOutputStream().write(input);
                    
                    // Check response
                    int responseCode = connection.getResponseCode();
                    if (responseCode >= 200 && responseCode < 300) {
                        log.info("Jakarta Migration Plugin - OPT-OUT anonymous usage data: Successfully inserted {} usage events to Supabase", events.size());
                        log.debug("Response code: {}", responseCode);
                        return; // Success
                    } else {
                        String errorResponse = new String(connection.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
                        log.error("HTTP error response: {} - Response body: {}", responseCode, errorResponse);
                        log.error("Request URL: {}", maskUrl(supabaseUrl));
                        log.error("Request headers: Content-Type=application/json, apikey=***, Authorization=Bearer ***");
                        log.error("Request body size: {} bytes", input.length);
                        throw new IOException("HTTP " + responseCode + ": " + errorResponse);
                    }
                } finally {
                    connection.disconnect();
                }
                
            } catch (Exception e) {
                lastException = e;
                attempt++;
                
                // Enhanced error logging
                log.error("Exception during attempt {}/{} to insert {} usage events", attempt, MAX_RETRIES, events.size(), e);
                log.error("Exception type: {}", e.getClass().getSimpleName());
                log.error("Exception message: {}", e.getMessage());
                if (e.getCause() != null) {
                    log.error("Root cause: {}", e.getCause().getMessage());
                }
                
                // Log configuration status
                log.error("Supabase configured: {}", isConfigured);
                log.error("Supabase URL: {}", maskUrl(config.getSupabaseUrl()));
                log.error("API key present: {}", config.getSupabaseAnonKey() != null && !config.getSupabaseAnonKey().isEmpty());
                
                if (attempt < MAX_RETRIES) {
                    long delay = RETRY_DELAY_MS * (long) Math.pow(RETRY_BACKOFF_MULTIPLIER, attempt - 1);
                    log.warn("Failed to insert usage events (attempt {}/{}), retrying in {}ms", 
                        attempt, MAX_RETRIES, delay, e);
                    
                    try {
                        TimeUnit.MILLISECONDS.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during retry delay", ie);
                    }
                }
            }
        }
        
        log.error("Failed to insert {} usage events after {} attempts. Last exception: {}", events.size(), MAX_RETRIES, lastException.getClass().getSimpleName());
        log.error("Final error message: {}", lastException.getMessage());
        // Fallback to logging
        logUsageEvents(events);
    }
    
    /**
     * Inserts error reports into the database.
     * Implements retry logic for network failures.
     */
    public void insertErrorReports(List<ErrorReport> reports) {
        if (!isConfigured) {
            logErrorReports(reports);
            return;
        }
        
        if (reports.isEmpty()) {
            return;
        }
        
        int attempt = 0;
        Exception lastException = null;
        
        while (attempt < MAX_RETRIES) {
            try {
                StringBuilder reportsJson = new StringBuilder("[");
                
                for (int i = 0; i < reports.size(); i++) {
                    ErrorReport report = reports.get(i);
                    if (i > 0) reportsJson.append(",");
                    
                    reportsJson.append("{");
                    reportsJson.append("\"user_id\":\"").append(escapeJson(report.getUserId())).append("\",");
                    reportsJson.append("\"plugin_version\":\"").append(escapeJson(report.getPluginVersion())).append("\",");
                    reportsJson.append("\"current_tab\":\"").append(escapeJson(report.getCurrentTab())).append("\",");
                    reportsJson.append("\"error_type\":\"").append(escapeJson(report.getErrorType())).append("\",");
                    reportsJson.append("\"error_message\":\"").append(escapeJson(report.getErrorMessage())).append("\",");
                    reportsJson.append("\"stack_trace\":\"").append(escapeJson(report.getStackTrace())).append("\",");
                    reportsJson.append("\"created_at\":\"").append(formatTimestamp(report.getTimestamp())).append("\"");
                    reportsJson.append("}");
                }
                
                reportsJson.append("]");
                
                // Make HTTP POST request to Supabase
                String supabaseUrl = config.getSupabaseUrl() + "/rest/v1/error_reports";
                String apiKey = config.getSupabaseAnonKey();
                
                URL url = new URL(supabaseUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                
                try {
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json");
                    connection.setRequestProperty("apikey", apiKey);
                    connection.setRequestProperty("Authorization", "Bearer " + apiKey);
                    connection.setRequestProperty("Prefer", "return=minimal");
                    connection.setDoOutput(true);
                    
                    // Send the JSON data
                    byte[] input = reportsJson.toString().getBytes(StandardCharsets.UTF_8);
                    connection.getOutputStream().write(input);
                    
                    // Check response
                    int responseCode = connection.getResponseCode();
                    if (responseCode >= 200 && responseCode < 300) {
                        log.info("Jakarta Migration Plugin - OPT-OUT anonymous error reporting: Successfully inserted {} error reports to Supabase", reports.size());
                        log.debug("Response code: {}", responseCode);
                        return; // Success
                    } else {
                        String errorResponse = new String(connection.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
                        throw new IOException("HTTP " + responseCode + ": " + errorResponse);
                    }
                } finally {
                    connection.disconnect();
                }
                
            } catch (Exception e) {
                lastException = e;
                attempt++;
                
                if (attempt < MAX_RETRIES) {
                    long delay = RETRY_DELAY_MS * (long) Math.pow(RETRY_BACKOFF_MULTIPLIER, attempt - 1);
                    log.warn("Failed to insert error reports (attempt {}/{}), retrying in {}ms", 
                        attempt, MAX_RETRIES, delay, e);
                    
                    try {
                        TimeUnit.MILLISECONDS.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during retry delay", ie);
                    }
                }
            }
        }
        
        log.error("Failed to insert {} error reports after {} attempts", reports.size(), MAX_RETRIES, lastException);
        // Fallback to logging
        logErrorReports(reports);
    }
    
    /**
     * Logs user activity without requiring a separate users table.
     * The simplified schema stores user_id directly in usage_events and error_reports tables.
     * This is OPT-OUT anonymous usage data collection for product improvement.
     */
    public void logUserActivity(String userId, String pluginVersion) {
        if (!isConfigured) {
            log.debug("Would log user {} activity with version {} (not configured) - OPT-OUT anonymous usage collection", maskUserId(userId), pluginVersion);
            return;
        }
        
        log.info("Jakarta Migration Plugin - OPT-OUT anonymous usage data: User {} activity logged with version {} (simplified schema - no separate users table)", 
            maskUserId(userId), pluginVersion);
    }
    
    /**
     * Formats timestamp for Supabase ISO format.
     */
    private String formatTimestamp(Instant timestamp) {
        return DateTimeFormatter.ISO_INSTANT.format(timestamp);
    }
    
    /**
     * Escapes special characters in JSON strings.
     */
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
    
    /**
     * Masks sensitive URL information for logging.
     */
    private String maskUrl(String url) {
        if (url == null || url.isEmpty()) {
            return "empty";
        }
        // Show first 20 characters and mask the rest
        if (url.length() > 20) {
            return url.substring(0, 20) + "***";
        }
        return url;
    }
    
    /**
     * Masks user ID for privacy in logs.
     */
    private String maskUserId(String userId) {
        if (userId == null || userId.length() < 8) return "***";
        return userId.substring(0, 4) + "***" + userId.substring(userId.length() - 4);
    }
    
    /**
     * Fallback logging for usage events when database is unavailable.
     * This is OPT-OUT anonymous usage data collection for product improvement.
     */
    private void logUsageEvents(List<UsageEvent> events) {
        log.info("Jakarta Migration Plugin - OPT-OUT anonymous usage data: Logging {} usage events (database not available):", events.size());
        log.info("Supabase configured: {}, URL: {}", isConfigured, maskUrl(config.getSupabaseUrl()));
        for (UsageEvent event : events) {
            log.info("  Event: {} | User: {} | Tab: {} | Action: {} | Version: {} | Timestamp: {} | Data: {}", 
                event.getEventType(),
                maskUserId(event.getUserId()),
                event.getCurrentUiTab() != null ? event.getCurrentUiTab() : "N/A",
                event.getTriggerAction() != null ? event.getTriggerAction() : "N/A",
                event.getPluginVersion() != null ? event.getPluginVersion() : "N/A",
                event.getTimestamp(),
                event.getEventData() != null ? event.getEventData() : "none");
        }
    }
    
    /**
     * Fallback logging for error reports when database is unavailable.
     * This is OPT-OUT anonymous error reporting for product improvement.
     */
    private void logErrorReports(List<ErrorReport> reports) {
        log.info("Jakarta Migration Plugin - OPT-OUT anonymous error reporting: Logging {} error reports (database not available):", reports.size());
        log.info("Supabase configured: {}, URL: {}", isConfigured, maskUrl(config.getSupabaseUrl()));
        for (ErrorReport report : reports) {
            log.info("  Error: {} | User: {} | Tab: {} | Message: {} | Version: {}", 
                report.getErrorType(),
                maskUserId(report.getUserId()),
                report.getCurrentTab(),
                report.getErrorMessage(),
                report.getPluginVersion());
        }
    }
    
    /**
     * Checks if the client is properly configured.
     */
    public boolean isConfigured() {
        return isConfigured;
    }
    
    @Override
    public void close() {
        log.debug("SupabaseClientWrapper closed");
    }
}
