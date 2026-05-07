package adrianmikula.jakartamigration.intellij.ui;

/**
 * Tests for SourceScansComponent.
 * Implements plan: .kilo/plans/1778130109717-neon-tiger.md
 */
import adrianmikula.jakartamigration.intellij.service.AdvancedScanningService;
import adrianmikula.jakartamigration.coderefactoring.service.RecipeService;
import adrianmikula.jakartamigration.analysis.persistence.CentralMigrationAnalysisStore;
import adrianmikula.jakartamigration.analysis.persistence.SqliteMigrationAnalysisStore;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import static org.assertj.core.api.Assertions.assertThat;
import java.nio.file.Paths;

public class SourceScansComponentTest extends BasePlatformTestCase {

    private SourceScansComponent sourceScansComponent;
    private AdvancedScanningService scanningService;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        // Create mock stores for testing
        CentralMigrationAnalysisStore centralStore = new CentralMigrationAnalysisStore();
        SqliteMigrationAnalysisStore projectStore = new SqliteMigrationAnalysisStore(Paths.get(getProject().getBasePath()));
        RecipeService recipeService = new adrianmikula.jakartamigration.coderefactoring.service.impl.RecipeServiceImpl(centralStore, projectStore);
        scanningService = new AdvancedScanningService(recipeService);
        sourceScansComponent = new SourceScansComponent(getProject(), scanningService);
    }

    public void testInitialization() {
        assertThat(sourceScansComponent.getPanel()).isNotNull();
        assertThat(sourceScansComponent.getJpaStatusLabel().getText()).isEqualTo("Not scanned yet");
        assertThat(sourceScansComponent.getBeanValidationStatusLabel().getText()).isEqualTo("Not scanned yet");
        assertThat(sourceScansComponent.getServletJspStatusLabel().getText()).isEqualTo("Not scanned yet");
    }

    public void testTabsExists() {
        assertThat(sourceScansComponent.getTabbedPane().getTabCount()).isGreaterThanOrEqualTo(3);
        assertThat(sourceScansComponent.getTabbedPane().getTitleAt(0)).isEqualTo("JPA Annotations");
    }

    public void testTablesInitialized() {
        assertThat(sourceScansComponent.getJpaTable()).isNotNull();
        assertThat(sourceScansComponent.getBeanValidationTable()).isNotNull();
        assertThat(sourceScansComponent.getServletJspTable()).isNotNull();
    }
}
