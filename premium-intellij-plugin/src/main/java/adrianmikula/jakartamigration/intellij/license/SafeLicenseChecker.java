package adrianmikula.jakartamigration.intellij.license;

import adrianmikula.jakartamigration.intellij.config.LicenseFailsafeConfig;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Safe licensing wrapper that prevents IDE lockup during license checks.
 * 
 * This class provides non-blocking license validation with timeout protection
 * and graceful fallback mechanisms to ensure the IDE never gets locked due to
 * licensing issues.
 * 
 * Key safety features:
 * - Async license checks with timeout
 * - Non-blocking startup activity
 * - Graceful degradation on license failures
 * - Development mode bypass
 * - No UI dialogs during startup
 */
public class SafeLicenseChecker {
    private static final Logger LOG = Logger.getInstance(SafeLicenseChecker.class);
    
    // License check timeout from configuration
    private static final long LICENSE_CHECK_TIMEOUT_MS = LicenseFailsafeConfig.getLicenseTimeoutMs();
    
    // Cached license status with fallback
    private static final AtomicReference<LicenseResult> cachedResult = new AtomicReference<>();
    private static volatile long lastCheckTime = 0;
    private static final long CACHE_DURATION_MS = 30 * 60 * 1000; // 30 minutes
    
    /**
     * Result of license check with safety information
     */
    public static class LicenseResult {
        public final boolean isLicensed;
        public final String status;
        public final boolean isCertain;
        public final boolean isFallback;
        public final long timestamp;
        public final int daysUntilExpiration;
        public final boolean wasTrial;
        
        public LicenseResult(boolean isLicensed, String status, boolean isCertain, boolean isFallback) {
            this(isLicensed, status, isCertain, isFallback, -1, false);
        }
        
        public LicenseResult(boolean isLicensed, String status, boolean isCertain, boolean isFallback,
                            int daysUntilExpiration, boolean wasTrial) {
            this.isLicensed = isLicensed;
            this.status = status;
            this.isCertain = isCertain;
            this.isFallback = isFallback;
            this.timestamp = System.currentTimeMillis();
            this.daysUntilExpiration = daysUntilExpiration;
            this.wasTrial = wasTrial;
        }
        
        public static LicenseResult licensed(String status) {
            return new LicenseResult(true, status, true, false, -1, false);
        }
        
        public static LicenseResult licensed(String status, int daysUntilExpiration, boolean wasTrial) {
            return new LicenseResult(true, status, true, false, daysUntilExpiration, wasTrial);
        }
        
        public static LicenseResult unlicensed(String status) {
            return new LicenseResult(false, status, true, false, -1, false);
        }
        
        public static LicenseResult unlicensed(String status, boolean wasTrial) {
            return new LicenseResult(false, status, true, false, -1, wasTrial);
        }
        
        public static LicenseResult uncertain(String status, boolean isFallback) {
            return new LicenseResult(false, status, false, isFallback, -1, false);
        }
        
        public static LicenseResult fallback(boolean isLicensed, String status) {
            return new LicenseResult(isLicensed, status, false, true, -1, false);
        }
        
        public static LicenseResult fallback(boolean isLicensed, String status, boolean wasTrial) {
            return new LicenseResult(isLicensed, status, false, true, -1, wasTrial);
        }
    }
    
    /**
     * Check license status safely without blocking the IDE.
     * This method will never block and always returns a result.
     */
    @NotNull
    public static LicenseResult checkLicenseSafe() {
        // Development mode bypass
        if (LicenseFailsafeConfig.isDevMode()) {
            LOG.info("SafeLicenseChecker: Development mode detected - licensed by default");
            return LicenseResult.licensed("Development Mode");
        }
        
        // Safe mode force fallback
        if (LicenseFailsafeConfig.isSafeMode()) {
            LOG.info("SafeLicenseChecker: Safe mode detected - using fallback behavior");
            return getSafeFallbackResult();
        }
        
        // License completely disabled
        if (LicenseFailsafeConfig.isLicenseDisabled()) {
            LOG.info("SafeLicenseChecker: License checks disabled - using fallback behavior");
            return getSafeFallbackResult();
        }
        
        // Check cache first
        LicenseResult cached = getCachedResult();
        if (cached != null) {
            return cached;
        }
        
        // Perform async license check with timeout
        return performLicenseCheckWithTimeout();
    }
    
    /**
     * Check license status asynchronously.
     * Returns a CompletableFuture that completes with the license result.
     */
    @NotNull
    public static CompletableFuture<LicenseResult> checkLicenseAsync() {
        if (LicenseFailsafeConfig.isDevMode()) {
            return CompletableFuture.completedFuture(LicenseResult.licensed("Development Mode"));
        }
        
        if (LicenseFailsafeConfig.isSafeMode()) {
            return CompletableFuture.completedFuture(getSafeFallbackResult());
        }
        
        if (LicenseFailsafeConfig.isLicenseDisabled()) {
            return CompletableFuture.completedFuture(getSafeFallbackResult());
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Use ProgressManager to ensure proper cancellation
                LicenseResult[] result = new LicenseResult[1];
                
                ProgressManager.getInstance().runProcess(() -> {
                    result[0] = performLicenseCheckInternal();
                }, null);
                
                return result[0];
            } catch (Exception e) {
                LOG.warn("SafeLicenseChecker: Async license check failed", e);
                return getSafeFallbackResult();
            }
        }, AppExecutorUtil.getAppExecutorService());
    }
    
    /**
     * Check license status synchronously but with timeout protection.
     * This should only be used in non-critical paths.
     */
    @NotNull
    public static LicenseResult checkLicenseWithTimeout() {
        try {
            CompletableFuture<LicenseResult> future = checkLicenseAsync();
            return future.get(LICENSE_CHECK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            LOG.warn("SafeLicenseChecker: License check timed out or failed", e);
            return getSafeFallbackResult();
        }
    }
    
    /**
     * Non-blocking startup license check.
     * This method is safe to call from StartupActivity.
     */
    public static void checkLicenseOnStartup(@NotNull Project project) {
        LOG.info("SafeLicenseChecker: Performing non-blocking startup license check");
        
        // Schedule async license check without blocking startup
        CompletableFuture<LicenseResult> licenseFuture = checkLicenseAsync();
        
        licenseFuture.thenAcceptAsync(result -> {
            LOG.info("SafeLicenseChecker: Startup license check completed: " + result.status);
            
            // Update UI components safely on EDT
            ApplicationManager.getApplication().invokeLater(() -> {
                updateLicenseUI(result);
            });
            
        }).exceptionally(throwable -> {
            LOG.warn("SafeLicenseChecker: Startup license check failed", throwable);
            
            // Even on failure, update UI with fallback status
            ApplicationManager.getApplication().invokeLater(() -> {
                updateLicenseUI(getSafeFallbackResult());
            });
            
            return null;
        });
    }
    
    /**
     * Request license from user safely (never during startup).
     * This method can only be called from user actions, not from startup.
     */
    public static void requestLicenseSafely(@NotNull Project project, @Nullable String message) {
        // Never show dialogs during startup
        if (ApplicationManager.getApplication().isHeadlessEnvironment()) {
            LOG.warn("SafeLicenseChecker: Skipping license request in headless environment");
            return;
        }
        
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                CheckLicense.requestLicense(message);
            } catch (Exception e) {
                LOG.warn("SafeLicenseChecker: Failed to show license dialog", e);
            }
        });
    }
    
    /**
     * Get user-friendly license status string.
     */
    @NotNull
    public static String getLicenseStatusString() {
        LicenseResult result = checkLicenseSafe();
        return result.status;
    }
    
    /**
     * Check if premium features are available.
     */
    public static boolean isPremiumAvailable() {
        LicenseResult result = checkLicenseSafe();
        return result.isLicensed || result.isFallback;
    }
    
    // Private helper methods
    
    @Nullable
    private static LicenseResult getCachedResult() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCheckTime < CACHE_DURATION_MS) {
            LicenseResult cached = cachedResult.get();
            if (cached != null) {
                return cached;
            }
        }
        return null;
    }
    
    @NotNull
    private static LicenseResult performLicenseCheckWithTimeout() {
        try {
            CompletableFuture<LicenseResult> future = CompletableFuture.supplyAsync(() -> {
                return performLicenseCheckInternal();
            });
            
            LicenseResult result = future.get(LICENSE_CHECK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            cachedResult.set(result);
            lastCheckTime = System.currentTimeMillis();
            return result;
            
        } catch (Exception e) {
            LOG.warn("SafeLicenseChecker: License check with timeout failed", e);
            return getSafeFallbackResult();
        }
    }
    
    @NotNull
    private static LicenseResult performLicenseCheckInternal() {
        try {
            // Delegate to original CheckLicense but with safety wrapper
            Boolean licensed = CheckLicense.isLicensed();
            if (licensed == null) {
                return LicenseResult.uncertain("Checking...", false);
            }
            
            // Removed trial-related methods - credits system handles free tier
            
            if (licensed) {
                return LicenseResult.licensed(CheckLicense.getLicenseStatusString());
            } else {
                return LicenseResult.unlicensed(CheckLicense.getLicenseStatusString());
            }
            
        } catch (Exception e) {
            LOG.warn("SafeLicenseChecker: Internal license check failed", e);
            return getSafeFallbackResult();
        }
    }
    
    @NotNull
    private static LicenseResult getSafeFallbackResult() {
        // Credits system handles free tier - no trial fallback needed
        return LicenseResult.fallback(false, "Free");
    }
    
    private static void updateLicenseUI(@NotNull LicenseResult result) {
        // UI updates removed - credits system handles free tier status
        LOG.info("SafeLicenseChecker: License status updated - " + result.status);
    }
    
    /**
     * Clear the license cache to force fresh check.
     */
    public static void clearCache() {
        cachedResult.set(null);
        lastCheckTime = 0;
        LOG.info("SafeLicenseChecker: Cache cleared");
    }
}
