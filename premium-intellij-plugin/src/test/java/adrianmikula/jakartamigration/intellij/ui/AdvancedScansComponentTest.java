package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.service.AdvancedScanningService;
import adrianmikula.jakartamigration.coderefactoring.service.RecipeService;
import adrianmikula.jakartamigration.analysis.persistence.CentralMigrationAnalysisStore;
import adrianmikula.jakartamigration.analysis.persistence.SqliteMigrationAnalysisStore;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import static org.assertj.core.api.Assertions.assertThat;
import java.nio.file.Paths;

public class AdvancedScansComponentTest extends BasePlatformTestCase {

    private AdvancedScansComponent advancedScansComponent;
    private AdvancedScanningService scanningService;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Create mock stores for testing
        CentralMigrationAnalysisStore centralStore = new CentralMigrationAnalysisStore();
        SqliteMigrationAnalysisStore projectStore = new SqliteMigrationAnalysisStore(Paths.get(getProject().getBasePath()));
        RecipeService recipeService = new adrianmikula.jakartamigration.coderefactoring.service.impl.RecipeServiceImpl(centralStore, projectStore);
        scanningService = new AdvancedScanningService(recipeService);
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
