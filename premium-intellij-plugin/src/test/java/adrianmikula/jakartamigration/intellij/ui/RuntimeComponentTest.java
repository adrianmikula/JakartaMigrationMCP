package adrianmikula.jakartamigration.intellij.ui;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import static org.assertj.core.api.Assertions.assertThat;

public class RuntimeComponentTest extends BasePlatformTestCase {

    private RuntimeComponent runtimeComponent;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        runtimeComponent = new RuntimeComponent(getProject());
    }

    public void testInitialization() {
        assertThat(runtimeComponent.getPanel()).isNotNull();
        assertThat(runtimeComponent.getRunHealthCheckButton().isEnabled()).isFalse();
        assertThat(runtimeComponent.getAnalyzeErrorsButton().isEnabled()).isFalse();
        assertThat(runtimeComponent.getRemediateButton().isEnabled()).isFalse();
        assertThat(runtimeComponent.getErrorTableModel().getRowCount()).isEqualTo(0);
    }

    public void testSetSelectedJarPath() {
        java.nio.file.Path jarPath = java.nio.file.Paths.get("test.jar");
        runtimeComponent.setSelectedJarPath(jarPath);

        assertThat(runtimeComponent.getJarPathField().getText()).isEqualTo(jarPath.toString());
        assertThat(runtimeComponent.getRunHealthCheckButton().isEnabled()).isTrue();
        assertThat(runtimeComponent.getAnalyzeErrorsButton().isEnabled()).isTrue();
        assertThat(runtimeComponent.getRemediateButton().isEnabled()).isTrue();
    }

    public void testClearResults() {
        runtimeComponent.setSelectedJarPath(java.nio.file.Paths.get("test.jar"));
        runtimeComponent.getStatusLabel().setText("Some result");

        runtimeComponent.clearResults();

        assertThat(runtimeComponent.getErrorTableModel().getRowCount()).isEqualTo(0);
        assertThat(runtimeComponent.getStatusLabel().getText()).isEqualTo("Ready");
        assertThat(runtimeComponent.getProgressBar().getValue()).isEqualTo(0);
    }
}
