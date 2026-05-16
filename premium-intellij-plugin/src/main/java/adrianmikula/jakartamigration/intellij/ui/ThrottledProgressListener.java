package adrianmikula.jakartamigration.intellij.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Throttled wrapper for ScanProgressListener that debounces rapid progress updates.
 * Prevents flooding the EDT event queue with too many UI updates during scanning.
 * 
 * This wrapper ensures that:
 * - Progress updates are batched and sent at a controlled rate
 * - onScanPhase updates are debounced (only the latest update within the throttle window is sent)
 * - onSubScanComplete updates are batched and sent periodically
 * - All listener callbacks are dispatched asynchronously to avoid blocking background threads
 */
public class ThrottledProgressListener implements ScanProgressListener {
    private static final Logger LOG = Logger.getInstance(ThrottledProgressListener.class);
    
    private final ScanProgressListener delegate;
    private final ScheduledExecutorService scheduler;
    private final long throttleMillis;
    
    // Debounce state for onScanPhase
    private volatile String pendingPhase;
    private volatile int pendingCompleted;
    private volatile int pendingTotal;
    private volatile boolean phaseUpdateScheduled = false;
    
    // Batch state for onSubScanComplete
    private final java.util.Map<String, Integer> pendingSubScanUpdates = new java.util.concurrent.ConcurrentHashMap<>();
    private volatile boolean subScanUpdateScheduled = false;
    
    // Rate limiting
    private final AtomicLong lastUpdateTime = new AtomicLong(0);
    private final long minUpdateIntervalMs;
    
    /**
     * Creates a throttled progress listener with default throttle rate (100ms).
     * 
     * @param delegate The actual progress listener to forward updates to
     */
    public ThrottledProgressListener(ScanProgressListener delegate) {
        this(delegate, 100, 50);
    }
    
    /**
     * Creates a throttled progress listener with custom throttle rate.
     * 
     * @param delegate The actual progress listener to forward updates to
     * @param throttleMillis The debounce window in milliseconds (default: 100ms)
     * @param minUpdateIntervalMs Minimum interval between updates in milliseconds (default: 50ms)
     */
    public ThrottledProgressListener(ScanProgressListener delegate, long throttleMillis, long minUpdateIntervalMs) {
        this.delegate = delegate;
        this.throttleMillis = throttleMillis;
        this.minUpdateIntervalMs = minUpdateIntervalMs;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ProgressListener-Throttler");
            t.setDaemon(true);
            return t;
        });
        
        LOG.info("ThrottledProgressListener created with throttle=" + throttleMillis + "ms, minInterval=" + minUpdateIntervalMs + "ms");
    }
    
    @Override
    public void onScanPhase(String phase, int completed, int total) {
        // Store the latest phase update
        pendingPhase = phase;
        pendingCompleted = completed;
        pendingTotal = total;
        
        // Schedule a debounced update if not already scheduled
        if (!phaseUpdateScheduled) {
            phaseUpdateScheduled = true;
            scheduler.schedule(this::dispatchPhaseUpdate, throttleMillis, TimeUnit.MILLISECONDS);
        }
    }
    
    @Override
    public void onScanComplete() {
        // Flush any pending updates before dispatching complete
        flushPendingUpdates();
        
        // Dispatch complete immediately (this is important)
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                delegate.onScanComplete();
            } catch (Exception e) {
                LOG.error("Error in delegate.onScanComplete", e);
            }
        });
    }
    
    @Override
    public void onScanError(Exception error) {
        // Flush any pending updates before dispatching error
        flushPendingUpdates();
        
        // Dispatch error immediately (this is important)
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                delegate.onScanError(error);
            } catch (Exception e) {
                LOG.error("Error in delegate.onScanError", e);
            }
        });
    }
    
    @Override
    public void onSubScanComplete(String scanType, int resultCount) {
        // Batch sub-scan updates
        pendingSubScanUpdates.put(scanType, resultCount);
        
        // Schedule a batched update if not already scheduled
        if (!subScanUpdateScheduled) {
            subScanUpdateScheduled = true;
            scheduler.schedule(this::dispatchSubScanUpdates, throttleMillis, TimeUnit.MILLISECONDS);
        }
    }
    
    /**
     * Dispatches the latest phase update to the delegate.
     * Called by the scheduler after the debounce window.
     */
    private void dispatchPhaseUpdate() {
        phaseUpdateScheduled = false;
        
        String phase = pendingPhase;
        int completed = pendingCompleted;
        int total = pendingTotal;
        
        if (phase == null) {
            return;
        }
        
        // Rate limiting: ensure minimum interval between updates
        long now = System.currentTimeMillis();
        long lastUpdate = lastUpdateTime.get();
        long timeSinceLastUpdate = now - lastUpdate;
        
        if (timeSinceLastUpdate < minUpdateIntervalMs) {
            // Too soon, reschedule
            scheduler.schedule(this::dispatchPhaseUpdate, 
                minUpdateIntervalMs - timeSinceLastUpdate, TimeUnit.MILLISECONDS);
            return;
        }
        
        lastUpdateTime.set(now);
        
        // Dispatch to EDT asynchronously
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                delegate.onScanPhase(phase, completed, total);
            } catch (Exception e) {
                LOG.error("Error in delegate.onScanPhase", e);
            }
        });
    }
    
    /**
     * Dispatches all pending sub-scan updates to the delegate.
     * Called by the scheduler after the debounce window.
     */
    private void dispatchSubScanUpdates() {
        subScanUpdateScheduled = false;
        
        // Rate limiting
        long now = System.currentTimeMillis();
        long lastUpdate = lastUpdateTime.get();
        long timeSinceLastUpdate = now - lastUpdate;
        
        if (timeSinceLastUpdate < minUpdateIntervalMs) {
            // Too soon, reschedule
            scheduler.schedule(this::dispatchSubScanUpdates, 
                minUpdateIntervalMs - timeSinceLastUpdate, TimeUnit.MILLISECONDS);
            return;
        }
        
        lastUpdateTime.set(now);
        
        // Create a snapshot of pending updates
        java.util.Map<String, Integer> updates = new java.util.HashMap<>(pendingSubScanUpdates);
        pendingSubScanUpdates.clear();
        
        if (updates.isEmpty()) {
            return;
        }
        
        // Dispatch to EDT asynchronously
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                for (java.util.Map.Entry<String, Integer> entry : updates.entrySet()) {
                    delegate.onSubScanComplete(entry.getKey(), entry.getValue());
                }
            } catch (Exception e) {
                LOG.error("Error in delegate.onSubScanComplete", e);
            }
        });
    }
    
    /**
     * Flushes all pending updates immediately.
     * Called when scan completes or errors to ensure all updates are delivered.
     */
    private void flushPendingUpdates() {
        // Cancel any scheduled updates
        phaseUpdateScheduled = false;
        subScanUpdateScheduled = false;
        
        // Dispatch pending phase update if any
        if (pendingPhase != null) {
            try {
                delegate.onScanPhase(pendingPhase, pendingCompleted, pendingTotal);
            } catch (Exception e) {
                LOG.error("Error flushing phase update", e);
            }
            pendingPhase = null;
        }
        
        // Dispatch pending sub-scan updates if any
        if (!pendingSubScanUpdates.isEmpty()) {
            try {
                for (java.util.Map.Entry<String, Integer> entry : pendingSubScanUpdates.entrySet()) {
                    delegate.onSubScanComplete(entry.getKey(), entry.getValue());
                }
            } catch (Exception e) {
                LOG.error("Error flushing sub-scan updates", e);
            }
            pendingSubScanUpdates.clear();
        }
    }
    
    /**
     * Shuts down the scheduler and cleans up resources.
     * Should be called when the listener is no longer needed.
     */
    public void shutdown() {
        try {
            scheduler.shutdown();
            if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
