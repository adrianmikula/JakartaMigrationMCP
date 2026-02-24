package adrianmikula.jakartamigration.intellij.ui;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SubtaskTableComponentTest extends BasePlatformTestCase {

    private SubtaskTableComponent subtaskTableComponent;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        subtaskTableComponent = new SubtaskTableComponent(getProject());
    }

    public void testInitialization() {
        assertThat(subtaskTableComponent.getPanel()).isNotNull();
    }

    public void testSetSubtasks() {
        List<SubtaskTableComponent.SubtaskItem> subtasks = new ArrayList<>();
        subtasks.add(new SubtaskTableComponent.SubtaskItem(
                "Update Imports", "Replace javax with jakarta", null, "open-rewrite"));

        subtaskTableComponent.setSubtasks(subtasks);
        assertThat(subtaskTableComponent.getPanel()).isNotNull();
    }
}
