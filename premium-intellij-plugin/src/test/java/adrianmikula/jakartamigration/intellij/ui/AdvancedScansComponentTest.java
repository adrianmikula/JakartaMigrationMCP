package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.service.AdvancedScanningService;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import static org.assertj.core.api.Assertions.assertThat;

public class AdvancedScansComponentTest extends BasePlatformTestCase {

    private AdvancedScansComponent advancedScansComponent;
    private AdvancedScanningService scanningService;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        scanningService = new AdvancedScanningService();
        advancedScansComponent = new AdvancedScansComponent(getProject(), scanningService);
    }

    public void testInitialization() {
        assertThat(advancedScansComponent.getPanel()).isNotNull();
        assertThat(advancedScansComponent.getJpaStatusLabel().getText()).isEqualTo("Not scanned yet");
        assertThat(advancedScansComponent.getBeanValidationStatusLabel().getText()).isEqualTo("Not scanned yet");
        assertThat(advancedScansComponent.getServletJspStatusLabel().getText()).isEqualTo("Not scanned yet");
    }

    public void testTabsExists() {
        assertThat(advancedScansComponent.getTabbedPane().getTabCount()).isGreaterThanOrEqualTo(3);
        assertThat(advancedScansComponent.getTabbedPane().getTitleAt(0)).isEqualTo("JPA Annotations");
    }

    public void testTablesInitialized() {
        assertThat(advancedScansComponent.getJpaTable()).isNotNull();
        assertThat(advancedScansComponent.getBeanValidationTable()).isNotNull();
        assertThat(advancedScansComponent.getServletJspTable()).isNotNull();
    }
}
