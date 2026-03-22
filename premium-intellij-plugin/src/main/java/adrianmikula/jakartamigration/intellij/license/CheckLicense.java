package adrianmikula.jakartamigration.intellij.license;

import adrianmikula.jakartamigration.intellij.ui.SupportComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicLong;

/**
 * License verification for Jakarta Migration Premium plugin.
 * 
 * This class checks whether the user has a valid license for the Premium features.
 * It uses JetBrains LicensingFacade API to verify the license status.
 * 
 * Based on JetBrains marketplace-makemecoffee-plugin example.
 */
public class CheckLicense {
    private static final Logger LOG = Logger.getInstance(CheckLicense.class);
    
    private static final @NonNls String PLUGIN_ID = "com.adrianmikula.jakarta-migration";
    private static final String PRODUCT_CODE = "PJAKARTAMIGRATI";
    
    private static final AtomicReference<Boolean> cachedLicenseStatus = new AtomicReference<>();
    private static long lastCheckTime = 0;
    private static final long CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000; // Check once per day

    /**
     * Checks if the plugin is licensed.
     * Uses caching to avoid too frequent checks (max once per day).
     * 
     * @return true if licensed, false otherwise
     */
    public static boolean isLicensed() {
        long currentTime = System.currentTimeMillis();
        
        // Check if we have a recent result
        if (currentTime - lastCheckTime < CHECK_INTERVAL_MS) {
            Boolean cached = cachedLicenseStatus.get();
            if (cached != null) {
                LOG.info("CheckLicense: Using cached license status: " + cached);
                return cached;
            }
        }
        
        // Perform license check
        boolean licensed = performLicenseCheck();
        cachedLicenseStatus.set(licensed);
        lastCheckTime = currentTime;
        
        LOG.info("CheckLicense: License check completed. Licensed: " + licensed);
        return licensed;
    }

    /**
     * Performs the actual license check using JetBrains LicensingFacade.
     */
    private static boolean performLicenseCheck() {
        try {
            // Get the licensing facade
            var licensingFacade = getLicensingFacade();
            
            if (licensingFacade == null) {
                LOG.info("CheckLicense: LicensingFacade not available (not in paid IDE or not initialized)");
                // For free IDEs or when LicensingFacade is not available,
                // we rely on our own trial/premium system property
                return checkTrialStatus();
            }
            
            // Check if we have a valid license
            PluginId pluginId = PluginId.getId(PLUGIN_ID);
            
            // Use reflection to call getLicenseStatus since licensingFacade is an Object
            Class<?> licensingFacadeClass = Class.forName("com.intellij.ide.LicensingFacade");
            var licenseStatusMethod = licensingFacadeClass.getMethod("getLicenseStatus", PluginId.class);
            var licenseStatus = licenseStatusMethod.invoke(licensingFacade, pluginId);
            
            if (licenseStatus != null) {
                // Use reflection to call isValid() since licenseStatus is an Object
                var isValidMethod = licenseStatus.getClass().getMethod("isValid");
                boolean valid = (boolean) isValidMethod.invoke(licenseStatus);
                LOG.info("CheckLicense: License status from LicensingFacade: " + licenseStatus + ", valid: " + valid);
                return valid;
            }
            
            LOG.info("CheckLicense: No license status found, checking trial");
            return checkTrialStatus();
            
        } catch (Exception e) {
            LOG.warn("CheckLicense: Error checking license", e);
            // Fall back to trial check
            return checkTrialStatus();
        }
    }

    /**
     * Gets the licensing facade from JetBrains Platform.
     * Uses reflection to avoid compile-time dependency on licensing API.
     */
    private static Object getLicensingFacade() {
        try {
            Class<?> licensingFacadeClass = Class.forName("com.intellij.ide.LicensingFacade");
            var method = licensingFacadeClass.getMethod("getInstance");
            return method.invoke(null);
        } catch (ClassNotFoundException e) {
            LOG.info("CheckLicense: LicensingFacade class not found (running in free IDE)");
            return null;
        } catch (Exception e) {
            LOG.warn("CheckLicense: Error getting LicensingFacade", e);
            return null;
        }
    }

    /**
     * Checks our own trial/premium status as fallback.
     * This is used when LicensingFacade is not available.
     */
    private static boolean checkTrialStatus() {
        // Check system property set by trial activation
        String premiumProp = System.getProperty("jakarta.migration.premium");
        if ("true".equals(premiumProp)) {
            // Check if trial has expired
            String trialEnd = System.getProperty("jakarta.migration.trial.end");
            if (trialEnd != null) {
                try {
                    long endTime = Long.parseLong(trialEnd);
                    if (System.currentTimeMillis() > endTime) {
                        LOG.info("CheckLicense: Trial has expired");
                        System.setProperty("jakarta.migration.premium", "false");
                        return false;
                    }
                } catch (NumberFormatException e) {
                    LOG.warn("CheckLicense: Invalid trial end time", e);
                    return false;
                }
            }
            LOG.info("CheckLicense: Trial/premium is active");
            return true;
        }
        
        LOG.info("CheckLicense: No trial/premium active");
        return false;
    }

    /**
     * Starts a free trial for the user.
     * Sets the trial end time and activates premium features.
     * 
     * @param project Current project
     */
    public static void startTrial() {
        LOG.info("CheckLicense: Trial started via startTrial() method");
        System.setProperty("jakarta.migration.premium", "true");
        System.setProperty("jakarta.migration.trial.end", 
                String.valueOf(System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000));
        
        // Clear license cache to force fresh check
        clearCache();
        
        // Notify SupportComponent to refresh
        SupportComponent.setPremiumActive(true);
    }
    
    /**
     * Gets the license status as a user-friendly string.
     */
    @NotNull
    public static String getLicenseStatusString() {
        if (isLicensed()) {
            return "Premium Active";
        }
        
        String trialEnd = System.getProperty("jakarta.migration.trial.end");
        if (trialEnd != null) {
            try {
                long endTime = Long.parseLong(trialEnd);
                long remaining = endTime - System.currentTimeMillis();
                if (remaining > 0) {
                    long daysRemaining = remaining / (24 * 60 * 60 * 1000);
                    return "Trial - " + daysRemaining + " days remaining";
                }
            } catch (NumberFormatException e) {
                // Ignore
            }
        }
        
        return "Free";
    }

    /**
     * Clears the cached license status to force a fresh check.
     * Useful when user purchases a license.
     */
    public static void clearCache() {
        cachedLicenseStatus.set(null);
        lastCheckTime = 0;
        LOG.info("CheckLicense: Cache cleared");
    }
}
