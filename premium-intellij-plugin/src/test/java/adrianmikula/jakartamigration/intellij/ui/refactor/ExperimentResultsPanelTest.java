package adrianmikula.jakartamigration.intellij.ui.refactor;

import adrianmikula.jakartamigration.experiment.domain.ExperimentResult;
import adrianmikula.jakartamigration.experiment.domain.ExperimentStatus;
import adrianmikula.jakartamigration.experiment.domain.MigrationSequence;
import adrianmikula.jakartamigration.experiment.domain.StepResult;
import adrianmikula.jakartamigration.intellij.service.ExperimentService;
import com.intellij.openapi.project.Project;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

class ExperimentResultsPanelTest {

    private Project mockProject;
    private ExperimentService mockExperimentService;

    @BeforeEach
    void setUp() {
        mockProject = mock(Project.class);
        mockExperimentService = mock(ExperimentService.class);
    }

    @Test
    void testRunExperimentCallsService() throws Exception {
        ExperimentResult result = new ExperimentResult(
            "run-1",
            "Test Sequence",
            Instant.now(),
            Instant.now().plusSeconds(10),
            ExperimentStatus.SUCCESS,
            List.of(new StepResult(true, 3, "OK", "")),
            null,
            "3 files modified",
            null
        );

        when(mockExperimentService.runExperiment("Test Sequence", Optional.empty()))
            .thenReturn(result);

        ExperimentResultsPanel panel = new ExperimentResultsPanel(mockProject, mockExperimentService);

        MigrationSequence sequence = new MigrationSequence("Test Sequence", "desc", List.of(), null, List.of());
        panel.runExperiment(sequence, Optional.empty());

        assertThat(panel.getPanel()).isNotNull();

        verify(mockExperimentService).runExperiment("Test Sequence", Optional.empty());
    }

    @Test
    void testOnExperimentCompletedCallback() throws Exception {
        ExperimentResult result = new ExperimentResult(
            "run-2",
            "Callback Sequence",
            Instant.now(),
            Instant.now().plusSeconds(5),
            ExperimentStatus.SUCCESS,
            List.of(),
            null,
            null,
            null
        );

        when(mockExperimentService.runExperiment(any(), any()))
            .thenReturn(result);

        ExperimentResultsPanel panel = new ExperimentResultsPanel(mockProject, mockExperimentService);

        java.util.concurrent.atomic.AtomicBoolean callbackFired = new java.util.concurrent.atomic.AtomicBoolean(false);
        panel.setOnExperimentCompleted(r -> callbackFired.set(true));

        MigrationSequence sequence = new MigrationSequence("Callback Sequence", "desc", List.of(), null, List.of());
        panel.runExperiment(sequence, Optional.empty());

        // Wait for the async callback to fire
        for (int i = 0; i < 100 && !callbackFired.get(); i++) {
            Thread.sleep(50);
        }

        assertThat(callbackFired.get()).isTrue();
    }
}
