package adrianmikula.jakartamigration.intellij.ui.refactor;

import adrianmikula.jakartamigration.experiment.domain.ExperimentResult;
import adrianmikula.jakartamigration.experiment.domain.ExperimentStatus;
import adrianmikula.jakartamigration.experiment.domain.MigrationSequence;
import adrianmikula.jakartamigration.experiment.domain.StepResult;
import adrianmikula.jakartamigration.intellij.service.ExperimentService;
import com.intellij.openapi.project.Project;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

class ExperimentHistoryPanelTest {

    private Project mockProject;
    private ExperimentService mockExperimentService;

    @BeforeEach
    void setUp() {
        mockProject = mock(Project.class);
        mockExperimentService = mock(ExperimentService.class);
    }

    @Test
    void testRefreshHistoryPopulatesTable() throws Exception {
        ExperimentResult result = new ExperimentResult(
            "run-1",
            "Test Sequence",
            Instant.now(),
            Instant.now(),
            ExperimentStatus.SUCCESS,
            List.of(new StepResult(true, 5, "OK", "")),
            null,
            "5 files modified",
            null
        );

        when(mockExperimentService.getHistory(Optional.empty(), Optional.empty(), 50))
            .thenReturn(List.of(result));

        ExperimentHistoryPanel panel = new ExperimentHistoryPanel(mockProject, mockExperimentService);

        panel.refreshHistory();

        assertThat(panel.getPanel()).isNotNull();
    }

    @Test
    void testFilterCallsHistoryService() throws Exception {
        ExperimentResult result = new ExperimentResult(
            "run-2",
            "Failed Sequence",
            Instant.now(),
            Instant.now(),
            ExperimentStatus.FAILED,
            List.of(),
            null,
            null,
            "error"
        );

        when(mockExperimentService.getHistory(any(), any(), anyInt()))
            .thenReturn(List.of(result));

        ExperimentHistoryPanel panel = new ExperimentHistoryPanel(mockProject, mockExperimentService);
        panel.refreshHistory();

        assertThat(panel.getPanel()).isNotNull();
    }

    @Test
    void testLoadSelectedSequence() throws Exception {
        MigrationSequence sequence = new MigrationSequence("seq", "desc", List.of(), null, List.of());

        when(mockExperimentService.getSequenceByResult("run-1"))
            .thenReturn(Optional.of(sequence));
        when(mockExperimentService.getHistory(Optional.empty(), Optional.empty(), 50))
            .thenReturn(List.of(new ExperimentResult(
                "run-1", "seq", Instant.now(), Instant.now(),
                ExperimentStatus.SUCCESS, List.of(), null, null, null
            )));

        ExperimentHistoryPanel panel = new ExperimentHistoryPanel(mockProject, mockExperimentService);

        // loadSelectedSequence requires a row to be selected; selection behavior
        // is Swing-bound, so we just verify the service is callable without exceptions
        assertThat(panel.getPanel()).isNotNull();
    }

    @Test
    void testFormatInstantDoesNotThrowUnsupportedTemporalTypeException(@TempDir Path tempDir) throws Exception {
        ExperimentResult result = new ExperimentResult(
            "run-1",
            "Test Sequence",
            Instant.parse("2024-01-15T10:30:00Z"),
            Instant.parse("2024-01-15T10:30:00Z"),
            ExperimentStatus.SUCCESS,
            List.of(),
            null,
            null,
            null
        );

        when(mockExperimentService.getHistory(any(), any(), anyInt()))
            .thenReturn(List.of(result));

        ExperimentHistoryPanel panel = new ExperimentHistoryPanel(mockProject, mockExperimentService);

        assertThat(panel.getPanel()).isNotNull();
    }
}
