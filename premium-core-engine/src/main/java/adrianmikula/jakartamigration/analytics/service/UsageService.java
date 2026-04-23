package adrianmikula.jakartamigration.analytics.service;

import adrianmikula.jakartamigration.analytics.config.SupabaseConfig;
import adrianmikula.jakartamigration.analytics.model.UsageEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
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
    private final BlockingQueue<UsageEvent> eventQueue;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean isRunning;
    private final String pluginVersion;
    
    public UsageService(UserIdentificationService userIdentificationService) {
        this.userIdentificationService = userIdentificationService;
        this.supabaseConfig = new SupabaseConfig();
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
     * Tracks a credit usage event.
     */
    public void trackCreditUsage(String creditType) {
        if (!userIdentificationService.isAnalyticsEnabled()) {
            return;
        }
        
        UsageEvent event = UsageEvent.creditUsed(
            userIdentificationService.getAnonymousUserId(),
            creditType,
            pluginVersion
        );
        
        addEventToQueue(event);
        log.debug("Tracked credit usage event for type: {}", creditType);
    }
    
    /**
     * Tracks an upgrade button click event.
     */
    public void trackUpgradeClick(String source) {
        if (!userIdentificationService.isAnalyticsEnabled()) {
            return;
        }
        
        UsageEvent event = UsageEvent.upgradeClicked(
            userIdentificationService.getAnonymousUserId(),
            source,
            pluginVersion
        );
        
        addEventToQueue(event);
        log.debug("Tracked upgrade click event from source: {}", source);
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
            log.error("Failed to send batch of {} events to Supabase", batch.size(), e);
            // Re-add events to queue for retry (with limit to prevent infinite loops)
            for (UsageEvent event : batch) {
                if (!eventQueue.offer(event)) {
                    log.warn("Failed to re-queue event after send failure, dropping: {}", event.getEventType());
                }
            }
        }
    }
    
    /**
     * Sends a batch of events to Supabase.
     * This is a simplified implementation - in production you would use the Supabase Java client.
     */
    private void sendBatchToSupabase(List<UsageEvent> events) {
        // TODO: Implement actual Supabase client integration
        // For now, we'll log the events to demonstrate the structure
        
        log.info("Sending {} usage events to Supabase:", events.size());
        for (UsageEvent event : events) {
            log.info("  Event: {} | User: {} | Type: {} | Timestamp: {}", 
                event.getEventType(),
                event.getUserId(),
                event.getCreditType() != null ? event.getCreditType() : "N/A",
                event.getTimestamp());
        }
        
        // Implementation would look something like:
        /*
        SupabaseClient client = new SupabaseClient(supabaseConfig.getSupabaseUrl(), supabaseConfig.getSupabaseAnonKey());
        
        for (UsageEvent event : events) {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("user_id", event.getUserId());
            eventData.put("event_type", event.getEventType().getValue());
            eventData.put("credit_type", event.getCreditType());
            eventData.put("event_data", event.getEventData());
            eventData.put("created_at", event.getTimestamp().toString());
            
            client.from("usage_events").insert(eventData);
        }
        */
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
        
        log.info("UsageService closed");
    }
}
