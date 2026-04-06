package adrianmikula.jakartamigration.intellij;

import adrianmikula.jakartamigration.intellij.license.SafeLicenseChecker;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

/**
 * SAFE startup activity to check license status on plugin startup.
 * 
 * This implementation uses SafeLicenseChecker to ensure the IDE never gets
 * locked due to licensing issues. The license check is performed asynchronously
 * and will not block IDE startup.
 * 
 * Key safety features:
 * - Non-blocking license checks
 * - Timeout protection  
 * - Graceful fallback on failures
 * - No UI dialogs during startup
 */
public class LicenseCheckStartupActivity implements StartupActivity {
    private static final Logger LOG = Logger.getInstance(LicenseCheckStartupActivity.class);

    @Override
    public void runActivity(@NotNull Project project) {
        LOG.info("LicenseCheckStartupActivity: Starting SAFE license check");
        
        // Use SafeLicenseChecker for non-blocking license check
        SafeLicenseChecker.checkLicenseOnStartup(project);
        
        LOG.info("LicenseCheckStartupActivity: SAFE license check initiated (non-blocking)");
    }
}
