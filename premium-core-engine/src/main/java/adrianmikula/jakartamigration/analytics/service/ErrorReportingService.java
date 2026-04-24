package adrianmikula.jakartamigration.analytics.service;

import adrianmikula.jakartamigration.analytics.config.SupabaseConfig;
import adrianmikula.jakartamigration.analytics.model.ErrorReport;
import lombok.extern.slf4j.Slf4j;

import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service for collecting and reporting errors to Supabase.
 * Implements asynchronous processing to avoid impacting user experience.
 */
@Slf4j
public class ErrorReportingService implements AutoCloseable {
    
    private final UserIdentificationService userIdentificationService;
    private final SupabaseConfig supabaseConfig;
    private final SupabaseClientWrapper supabaseClient;
    private final BlockingQueue<ErrorReport> errorQueue;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean isRunning;
    private final String pluginVersion;
    
    // Thread-safe current tab tracking
    private volatile String currentTab = "unknown";
    
    public ErrorReportingService(UserIdentificationService userIdentificationService) {
        this.userIdentificationService = userIdentificationService;
        this.supabaseConfig = new SupabaseConfig();
        this.supabaseClient = new SupabaseClientWrapper(supabaseConfig);
        this.errorQueue = new LinkedBlockingQueue<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ErrorReportingService-Scheduler");
            t.setDaemon(true);
            return t;
        });
        this.isRunning = new AtomicBoolean(false);
        this.pluginVersion = getPluginVersion();
        
        if (userIdentificationService.isErrorReportingEnabled()) {
            startErrorProcessor();
        }
        
        log.info("ErrorReportingService initialized - Error reporting enabled: {}", 
            userIdentificationService.isErrorReportingEnabled());
    }
    
    /**
     * Starts the error processor for sending reports to Supabase.
     */
    private void startErrorProcessor() {
        isRunning.set(true);
        
        // Schedule periodic error processing every 30 seconds
        scheduler.scheduleAtFixedRate(
            this::processErrors,
            30,
            30,
            TimeUnit.SECONDS
        );
        
        log.debug("ErrorReportingService processor started");
    }
    
    /**
     * Reports an error that occurred.
     */
    public void reportError(Throwable exception) {
        if (!userIdentificationService.isErrorReportingEnabled()) {
            return;
        }
        
        ErrorReport report = ErrorReport.fromException(
            userIdentificationService.getAnonymousUserId(),
            pluginVersion,
            currentTab,
            exception
        );
        
        addErrorToQueue(report);
        log.debug("Queued error report for: {}", exception.getClass().getSimpleName());
    }
    
    /**
     * Reports an error with additional context.
     */
    public void reportError(Throwable exception, String context) {
        if (!userIdentificationService.isErrorReportingEnabled()) {
            return;
        }
        
        ErrorReport report = ErrorReport.fromException(
            userIdentificationService.getAnonymousUserId(),
            pluginVersion,
            currentTab,
            exception
        );
        
        // Add context to error message
        if (context != null && !context.trim().isEmpty()) {
            report.setErrorMessage(report.getErrorMessage() + " [Context: " + context + "]");
        }
        
        addErrorToQueue(report);
        log.debug("Queued error report with context for: {}", exception.getClass().getSimpleName());
    }
    
    /**
     * Updates the currently active UI tab.
     */
    public void setCurrentTab(String tabName) {
        this.currentTab = tabName != null ? tabName : "unknown";
    }
    
    /**
     * Adds an error report to the processing queue.
     */
    private void addErrorToQueue(ErrorReport report) {
        if (!errorQueue.offer(report)) {
            log.warn("Error queue is full, dropping error report for: {}", report.getErrorType());
        }
    }
    
    /**
     * Processes queued error reports and sends them to Supabase.
     */
    private void processErrors() {
        if (!isRunning.get()) {
            return;
        }
        
        // Process up to 5 errors at a time to avoid overwhelming the system
        java.util.List<ErrorReport> batch = new java.util.ArrayList<>();
        errorQueue.drainTo(batch, 5);
        
        if (batch.isEmpty()) {
            return;
        }
        
        try {
            sendErrorsToSupabase(batch);
            log.debug("Successfully sent {} error reports to Supabase", batch.size());
        } catch (Exception e) {
            log.error("Failed to send {} error reports to Supabase", batch.size(), e);
            // Re-add errors to queue for retry (with limit to prevent infinite loops)
            for (ErrorReport report : batch) {
                if (!errorQueue.offer(report)) {
                    log.warn("Failed to re-queue error report after send failure, dropping: {}", report.getErrorType());
                }
            }
        }
    }
    
    /**
     * Sends error reports to Supabase using the SupabaseClientWrapper.
     */
    private void sendErrorsToSupabase(java.util.List<ErrorReport> errors) {
        try {
            // Log user activity before sending error reports
            for (ErrorReport error : errors) {
                supabaseClient.logUserActivity(error.getUserId(), error.getPluginVersion());
            }
            
            // Send error reports to database
            supabaseClient.insertErrorReports(errors);
            
        } catch (Exception e) {
            log.error("Failed to send {} error reports to Supabase", errors.size(), e);
            throw e; // Re-throw to trigger retry logic in processErrors
        }
    }
    
    /**
     * Gets plugin version from gradle.properties.
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
     * Forces immediate processing of all queued errors.
     */
    public void flush() {
        if (isRunning.get()) {
            processErrors();
        }
    }
    
    /**
     * Gets current queue size.
     */
    public int getQueueSize() {
        return errorQueue.size();
    }
    
    @Override
    public void close() {
        isRunning.set(false);
        
        // Process remaining errors before shutdown
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
        
        log.info("ErrorReportingService closed");
    }
}
