package adrianmikula.jakartamigration.mcp;

import adrianmikula.jakartamigration.config.FeatureFlag;
import adrianmikula.jakartamigration.config.FeatureFlagsService;
import adrianmikula.jakartamigration.experiment.domain.ExperimentStatus;
import adrianmikula.jakartamigration.experiment.domain.ExperimentResult;
import adrianmikula.jakartamigration.experiment.domain.MigrationSequence;
import adrianmikula.jakartamigration.experiment.mcp.ExperimentTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PremiumExperimentToolsTest {

    @Mock
    private ExperimentTools experimentTools;

    @Mock
    private FeatureFlagsService featureFlagsService;

    @InjectMocks
    private PremiumExperimentTools tools;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        doNothing().when(featureFlagsService).requireEnabled(FeatureFlag.EXPERIMENT_ENGINE);
    }

    @Test
    void createMigrationSequence_requiresExperimentEngineFlag() throws Exception {
        when(experimentTools.createMigrationSequence(any(), any(), any(), any()))
                .thenReturn(new MigrationSequence("seq", "desc", Collections.emptyList(), Instant.now(), Collections.emptyList()));

        String result = tools.createMigrationSequence("seq", "desc", "[]", "");

        verify(featureFlagsService).requireEnabled(FeatureFlag.EXPERIMENT_ENGINE);
        assertThat(result).contains("\"name\":\"seq\"");
    }

    @Test
    void addSequenceStep_requiresExperimentEngineFlag() throws Exception {
        when(experimentTools.addSequenceStep(any(), any()))
                .thenReturn(new MigrationSequence("seq", "desc", Collections.emptyList(), Instant.now(), Collections.emptyList()));

        String result = tools.addSequenceStep("seq", "{}");

        verify(featureFlagsService).requireEnabled(FeatureFlag.EXPERIMENT_ENGINE);
        assertThat(result).contains("\"name\":\"seq\"");
    }

    @Test
    void listMigrationSequences_requiresExperimentEngineFlag() throws Exception {
        when(experimentTools.listMigrationSequences(any())).thenReturn(Collections.emptyList());

        String result = tools.listMigrationSequences(null);

        verify(featureFlagsService).requireEnabled(FeatureFlag.EXPERIMENT_ENGINE);
        assertThat(result).contains("[");
    }

    @Test
    void getMigrationSequence_requiresExperimentEngineFlag() throws Exception {
        when(experimentTools.getMigrationSequence(any()))
                .thenReturn(Optional.of(new MigrationSequence("seq", "desc", Collections.emptyList(), Instant.now(), Collections.emptyList())));

        String result = tools.getMigrationSequence("seq");

        verify(featureFlagsService).requireEnabled(FeatureFlag.EXPERIMENT_ENGINE);
        assertThat(result).contains("\"name\":\"seq\"");
    }

    @Test
    void runMigrationExperiment_requiresExperimentEngineFlag() throws Exception {
        ExperimentResult mockResult = new ExperimentResult(
                "run1", "seq", Instant.now(), Instant.now(),
                ExperimentStatus.SUCCESS, Collections.emptyList(), null, "summary", null);
        when(experimentTools.runMigrationExperiment(any(), any(), any())).thenReturn(mockResult);

        String result = tools.runMigrationExperiment("seq", tempDir.toString(), null);

        verify(featureFlagsService).requireEnabled(FeatureFlag.EXPERIMENT_ENGINE);
        assertThat(result).contains("\"runId\":\"run1\"");
    }

    @Test
    void getExperimentHistory_requiresExperimentEngineFlag() throws Exception {
        when(experimentTools.getExperimentHistory(any(), any(), anyInt())).thenReturn(Collections.emptyList());

        String result = tools.getExperimentHistory(null, null, 10);

        verify(featureFlagsService).requireEnabled(FeatureFlag.EXPERIMENT_ENGINE);
        assertThat(result).contains("[");
    }

    @Test
    void getExperimentResult_requiresExperimentEngineFlag() throws Exception {
        ExperimentResult mockResult = new ExperimentResult(
                "run1", "seq", Instant.now(), Instant.now(),
                ExperimentStatus.SUCCESS, Collections.emptyList(), null, "summary", null);
        when(experimentTools.getExperimentResult(any())).thenReturn(Optional.of(mockResult));

        String result = tools.getExperimentResult("run1");

        verify(featureFlagsService).requireEnabled(FeatureFlag.EXPERIMENT_ENGINE);
        assertThat(result).contains("\"runId\":\"run1\"");
    }

    @Test
    void compareExperiments_requiresExperimentEngineFlag() throws Exception {
        when(experimentTools.compareExperiments(any(), any()))
                .thenReturn(new adrianmikula.jakartamigration.experiment.domain.ComparisonReport(
                        "cmp1", null, null, "A", Collections.emptyList(), Collections.emptyList()));

        String result = tools.compareExperiments("run1", "run2");

        verify(featureFlagsService).requireEnabled(FeatureFlag.EXPERIMENT_ENGINE);
        assertThat(result).contains("\"comparisonId\":\"cmp1\"");
    }
}
