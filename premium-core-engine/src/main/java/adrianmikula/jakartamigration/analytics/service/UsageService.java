package adrianmikula.jakartamigration.analytics.service;

import adrianmikula.jakartamigration.analytics.config.SupabaseConfig;
import adrianmikula.jakartamigration.analytics.model.UsageEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service for tracking usage metrics and sending them to Supabase.
 * Implements batch processing to reduce API calls and improve performance.
 */
@Slf4j
public class UsageService implements AutoCloseable {
    
    private final UserIdentificationService userIdentificationService;
    private final SupabaseConfig supabaseConfig;
    private final SupabaseClientWrapper supabaseClient;
    private final BlockingQueue<UsageEvent> eventQueue;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean isRunning;
    private final String pluginVersion;
    
    public UsageService(UserIdentificationService userIdentificationService) {
        this.userIdentificationService = userIdentificationService;
        this.supabaseConfig = new SupabaseConfig();
        this.supabaseClient = new SupabaseClientWrapper(supabaseConfig);
        this.eventQueue = new LinkedBlockingQueue<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "UsageService-Scheduler");
            t.setDaemon(true);
            return t;
        });
        this.isRunning = new AtomicBoolean(false);
        this.pluginVersion = getPluginVersion();
        
        if (userIdentificationService.isAnalyticsEnabled()) {
            startBatchProcessor();
        }
        
        log.info("UsageService initialized - Analytics enabled: {}", 
            userIdentificationService.isAnalyticsEnabled());
    }
    
    /**
     * Starts the batch processor for sending events to Supabase.
     */
    private void startBatchProcessor() {
        isRunning.set(true);
        
        // Schedule periodic batch processing
        scheduler.scheduleAtFixedRate(
            this::processBatch,
            supabaseConfig.getAnalyticsFlushIntervalSeconds(),
            supabaseConfig.getAnalyticsFlushIntervalSeconds(),
            TimeUnit.SECONDS
        );
        
        log.debug("UsageService batch processor started with interval: {} seconds", 
            supabaseConfig.getAnalyticsFlushIntervalSeconds());
    }
    
    /**
     * Tracks a credit usage event with context information.
     */
    public void trackCreditUsage(String creditType, String currentUiTab, String triggerAction) {
        if (!userIdentificationService.isAnalyticsEnabled()) {
            return;
        }
        
        UsageEvent event = UsageEvent.creditUsed(
            userIdentificationService.getAnonymousUserId(),
            creditType,
            currentUiTab,
            triggerAction,
            pluginVersion
        );
        
        addEventToQueue(event);
        log.debug("Tracked credit usage event for type: {} in tab: {} with action: {}", 
            creditType, currentUiTab, triggerAction);
    }
    
    /**
     * Tracks an upgrade button click event with context information.
     */
    public void trackUpgradeClick(String source, String currentUiTab) {
        if (!userIdentificationService.isAnalyticsEnabled()) {
            return;
        }
        
        UsageEvent event = UsageEvent.upgradeClicked(
            userIdentificationService.getAnonymousUserId(),
            source,
            currentUiTab,
            pluginVersion
        );
        
        addEventToQueue(event);
        log.debug("Tracked upgrade click event from source: {} in tab: {}", source, currentUiTab);
    }
    
    /**
     * Adds an event to the processing queue.
     */
    private void addEventToQueue(UsageEvent event) {
        if (!eventQueue.offer(event)) {
            log.warn("Event queue is full, dropping event: {}", event.getEventType());
        }
    }
    
    /**
     * Processes a batch of events and sends them to Supabase.
     */
    private void processBatch() {
        if (!isRunning.get()) {
            return;
        }
        
        List<UsageEvent> batch = new ArrayList<>();
        int batchSize = supabaseConfig.getAnalyticsBatchSize();
        
        // Collect events from queue
        eventQueue.drainTo(batch, batchSize);
        
        if (batch.isEmpty()) {
            return;
        }
        
        try {
            sendBatchToSupabase(batch);
            log.debug("Successfully sent batch of {} events to Supabase", batch.size());
        } catch (Exception e) {
            log.error("Failed to send batch of {} events to Supabase. Queue size: {}. Configured: {}. Batch details: {}", 
                batch.size(), eventQueue.size(), supabaseClient.isConfigured(), getBatchDetails(batch), e);
            
            // Enhanced error logging
            log.error("Supabase URL configured: {}", supabaseConfig.isConfigured());
            log.error("Analytics enabled: {}", userIdentificationService.isAnalyticsEnabled());
            log.error("Plugin version: {}", pluginVersion);
            
            // Re-add events to queue for retry (with limit to prevent infinite loops)
            for (UsageEvent event : batch) {
                if (!eventQueue.offer(event)) {
                    log.warn("Failed to re-queue event after send failure, dropping: {}", event.getEventType());
                }
            }
        }
    }
    
    /**
     * Sends a batch of events to Supabase using the SupabaseClientWrapper.
     */
    private void sendBatchToSupabase(List<UsageEvent> events) {
        try {
            // Send events directly to database without user table dependency
            supabaseClient.insertUsageEvents(events);
            
        } catch (Exception e) {
            log.error("Failed to send batch of {} events to Supabase. Events: {}", 
                events.size(), getEventSummary(events), e);
            throw e; // Re-throw to trigger retry logic in processBatch
        }
    }
    
    /**
     * Gets the plugin version from gradle.properties.
     */
    private String getPluginVersion() {
        try {
            Properties props = new Properties();
            try (var is = getClass().getClassLoader().getResourceAsStream("gradle.properties")) {
                if (is != null) {
                    props.load(is);
                    return props.getProperty("version", "unknown");
                }
            }
        } catch (Exception e) {
            log.warn("Failed to load plugin version", e);
        }
        return "unknown";
    }
    
    /**
     * Forces immediate processing of all queued events.
     */
    public void flush() {
        if (isRunning.get()) {
            processBatch();
        }
    }
    
    /**
     * Gets the current queue size.
     */
    public int getQueueSize() {
        return eventQueue.size();
    }
    
    @Override
    public void close() {
        isRunning.set(false);
        
        // Process remaining events before shutdown
        flush();
        
        // Shutdown scheduler
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // Close Supabase client
        if (supabaseClient != null) {
            try {
                supabaseClient.close();
            } catch (Exception e) {
                log.warn("Error closing Supabase client", e);
            }
        }
        
        log.info("UsageService closed");
    }
    
    /**
     * Gets detailed information about a batch for debugging.
     */
    private String getBatchDetails(List<UsageEvent> batch) {
        if (batch.isEmpty()) return "empty batch";
        
        StringBuilder details = new StringBuilder();
        details.append("batch_size=").append(batch.size());
        
        // Count event types
        Map<String, Long> typeCounts = batch.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                e -> e.getEventType().getValue(), 
                java.util.stream.Collectors.counting()));
        details.append(", types=").append(typeCounts);
        
        // Show user IDs (masked)
        Set<String> userIds = batch.stream()
            .map(e -> maskUserId(e.getUserId()))
            .collect(java.util.stream.Collectors.toSet());
        details.append(", users=").append(userIds);
        
        return details.toString();
    }
    
    /**
     * Gets a summary of events for logging.
     */
    private String getEventSummary(List<UsageEvent> events) {
        return events.stream()
            .map(e -> String.format("%s(user=%s,type=%s)", 
                e.getEventType().getValue(), 
                maskUserId(e.getUserId()),
                e.getCreditType() != null ? e.getCreditType() : "N/A"))
            .collect(java.util.stream.Collectors.joining(", "));
    }
    
    /**
     * Masks user ID for privacy in logs.
     */
    private String maskUserId(String userId) {
        if (userId == null || userId.length() < 8) return "***";
        return userId.substring(0, 4) + "***" + userId.substring(userId.length() - 4);
    }
}
