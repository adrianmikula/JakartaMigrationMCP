package adrianmikula.jakartamigration.intellij;

import adrianmikula.jakartamigration.intellij.license.CheckLicense;
import adrianmikula.jakartamigration.intellij.ui.SupportComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Startup activity to check license status on plugin startup.
 * 
 * This ensures that the license status is checked and UI is updated
 * when the plugin first loads, not just when tool windows are opened.
 */
public class LicenseCheckStartupActivity implements StartupActivity {
    private static final Logger LOG = Logger.getInstance(LicenseCheckStartupActivity.class);

    @Override
    public void runActivity(@NotNull Project project) {
        LOG.info("LicenseCheckStartupActivity: Checking license status on plugin startup");
        
        // Check license status
        boolean isLicensed = CheckLicense.isLicensed();
        String licenseStatus = CheckLicense.getLicenseStatusString();
        
        LOG.info("LicenseCheckStartupActivity: License status=" + licenseStatus + ", isLicensed=" + isLicensed);
        
        // Update SupportComponent to reflect current license status
        ApplicationManager.getApplication().invokeLater(() -> {
            SupportComponent.setPremiumActive(isLicensed);
            SupportComponent.setLicenseStatus(licenseStatus);
        });
    }
}
