package adrianmikula.jakartamigration.experiment.service;

import adrianmikula.jakartamigration.experiment.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class HistoryServiceTest {

    @Test
    void recordAndGetRun(@TempDir Path tempDir) throws Exception {
        HistoryService service = new HistoryService(tempDir);
        ExperimentResult result = buildResult("run1", ExperimentStatus.SUCCESS);

        service.recordRun(result);
        Optional<ExperimentResult> loaded = service.getRun("run1");

        assertTrue(loaded.isPresent());
        assertEquals("run1", loaded.get().runId());
        assertEquals(ExperimentStatus.SUCCESS, loaded.get().status());
    }

    @Test
    void getNonExistentRun_returnsEmpty(@TempDir Path tempDir) throws Exception {
        HistoryService service = new HistoryService(tempDir);
        Optional<ExperimentResult> result = service.getRun("nonexistent");
        assertTrue(result.isEmpty());
    }

    @Test
    void listRuns_returnsChronologically(@TempDir Path tempDir) throws Exception {
        HistoryService service = new HistoryService(tempDir);
        ExperimentResult r1 = buildResult("run1", ExperimentStatus.SUCCESS);
        ExperimentResult r2 = buildResult("run2", ExperimentStatus.FAILED);

        service.recordRun(r1);
        service.recordRun(r2);

        List<ExperimentResult> runs = service.listRuns(Optional.empty(), Optional.empty(), 10);
        assertEquals(2, runs.size());
        assertEquals("run2", runs.get(0).runId());
        assertEquals("run1", runs.get(1).runId());
    }

    @Test
    void listRuns_filtersBySequenceName(@TempDir Path tempDir) throws Exception {
        HistoryService service = new HistoryService(tempDir);
        service.recordRun(buildResult("run1", ExperimentStatus.SUCCESS, "seqA"));
        service.recordRun(buildResult("run2", ExperimentStatus.FAILED, "seqB"));

        List<ExperimentResult> filtered = service.listRuns(Optional.of("seqA"), Optional.empty(), 10);
        assertEquals(1, filtered.size());
        assertEquals("seqA", filtered.get(0).sequenceName());
    }

    @Test
    void listRuns_filtersByStatus(@TempDir Path tempDir) throws Exception {
        HistoryService service = new HistoryService(tempDir);
        service.recordRun(buildResult("run1", ExperimentStatus.SUCCESS));
        service.recordRun(buildResult("run2", ExperimentStatus.FAILED));

        List<ExperimentResult> failed = service.listRuns(Optional.empty(), Optional.of(ExperimentStatus.FAILED), 10);
        assertEquals(1, failed.size());
        assertEquals(ExperimentStatus.FAILED, failed.get(0).status());
    }

    @Test
    void listRuns_skips_corrupted_json_files(@TempDir Path tempDir) throws Exception {
        HistoryService service = new HistoryService(tempDir);
        service.recordRun(buildResult("run1", ExperimentStatus.SUCCESS));
        service.recordRun(buildResult("run2", ExperimentStatus.FAILED));

        Path historyDir = tempDir.resolve(".experiments/history");
        Files.writeString(historyDir.resolve("corrupted.json"), "not valid json {{{", StandardCharsets.UTF_8);

        List<ExperimentResult> runs = service.listRuns(Optional.empty(), Optional.empty(), 10);
        assertEquals(2, runs.size());
    }

    @Test
    void listRuns_skips_empty_json_files(@TempDir Path tempDir) throws Exception {
        HistoryService service = new HistoryService(tempDir);
        service.recordRun(buildResult("run1", ExperimentStatus.SUCCESS));

        Path historyDir = tempDir.resolve(".experiments/history");
        Files.writeString(historyDir.resolve("empty.json"), "", StandardCharsets.UTF_8);

        List<ExperimentResult> runs = service.listRuns(Optional.empty(), Optional.empty(), 10);
        assertEquals(1, runs.size());
        assertEquals("run1", runs.get(0).runId());
    }

    @Test
    void getRun_throws_ioexception_for_corrupted_json(@TempDir Path tempDir) throws Exception {
        HistoryService service = new HistoryService(tempDir);
        Path historyDir = tempDir.resolve(".experiments/history");
        Files.createDirectories(historyDir);
        Files.writeString(historyDir.resolve("bad.json"), "not valid json", StandardCharsets.UTF_8);

        assertThrows(IOException.class, () -> service.getRun("bad"));
    }

    private ExperimentResult buildResult(String runId, ExperimentStatus status) {
        return buildResult(runId, status, "seq1");
    }

    private ExperimentResult buildResult(String runId, ExperimentStatus status, String sequenceName) {
        return new ExperimentResult(
            runId,
            sequenceName,
            Instant.now(),
            Instant.now(),
            status,
            List.of(),
            new TestOutcome(10, 8, 2, 0, java.time.Duration.ofSeconds(30)),
            "diff summary",
            null
        );
    }
}
