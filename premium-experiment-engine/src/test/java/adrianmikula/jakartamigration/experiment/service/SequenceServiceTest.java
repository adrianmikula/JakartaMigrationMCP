package adrianmikula.jakartamigration.experiment.service;

import adrianmikula.jakartamigration.experiment.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SequenceServiceTest {

    @Test
    void saveAndLoadSequence(@TempDir Path tempDir) throws Exception {
        SequenceService service = new SequenceService(tempDir);
        MigrationSequence sequence = new MigrationSequence(
            "test-seq",
            "Test sequence",
            List.of(SequenceStep.openRewrite("org.openrewrite.java.migrate.jakarta.JavaxMigrationToJakarta")),
            Instant.now(),
            List.of("test")
        );

        service.saveSequence(sequence);
        Optional<MigrationSequence> loaded = service.loadSequence("test-seq");

        assertTrue(loaded.isPresent());
        assertEquals("test-seq", loaded.get().name());
        assertEquals("Test sequence", loaded.get().description());
        assertEquals(1, loaded.get().steps().size());
    }

    @Test
    void loadNonExistentSequence_returnsEmpty(@TempDir Path tempDir) throws Exception {
        SequenceService service = new SequenceService(tempDir);
        Optional<MigrationSequence> result = service.loadSequence("nonexistent");
        assertTrue(result.isEmpty());
    }

    @Test
    void listSequences_returnsAll(@TempDir Path tempDir) throws Exception {
        SequenceService service = new SequenceService(tempDir);
        service.saveSequence(new MigrationSequence("seq1", "Desc 1", List.of(), Instant.now(), List.of()));
        service.saveSequence(new MigrationSequence("seq2", "Desc 2", List.of(), Instant.now(), List.of("tag1")));

        List<MigrationSequence> all = service.listSequences(Optional.empty());
        assertEquals(2, all.size());
    }

    @Test
    void listSequences_filtersByTag(@TempDir Path tempDir) throws Exception {
        SequenceService service = new SequenceService(tempDir);
        service.saveSequence(new MigrationSequence("seq1", "Desc 1", List.of(), Instant.now(), List.of()));
        service.saveSequence(new MigrationSequence("seq2", "Desc 2", List.of(), Instant.now(), List.of("tag1")));

        List<MigrationSequence> filtered = service.listSequences(Optional.of("tag1"));
        assertEquals(1, filtered.size());
        assertEquals("seq2", filtered.get(0).name());
    }

    @Test
    void appendStep_addsToExistingSequence(@TempDir Path tempDir) throws Exception {
        SequenceService service = new SequenceService(tempDir);
        service.saveSequence(new MigrationSequence("seq1", "Desc", List.of(), Instant.now(), List.of()));

        MigrationSequence updated = service.appendStep("seq1", SequenceStep.regexReplacement("javax", "jakarta", "**/*.java"));
        assertEquals(1, updated.steps().size());
        assertEquals(SequenceStepType.REGEX_REPLACEMENT, updated.steps().get(0).type());
    }

    @Test
    void appendStep_nonExistent_throws(@TempDir Path tempDir) {
        SequenceService service = new SequenceService(tempDir);
        assertThrows(NoSuchElementException.class, () -> service.appendStep("nonexistent", SequenceStep.openRewrite("recipe")));
    }

    @Test
    void deleteSequence_removesFile(@TempDir Path tempDir) throws Exception {
        SequenceService service = new SequenceService(tempDir);
        service.saveSequence(new MigrationSequence("seq1", "Desc", List.of(), Instant.now(), List.of()));
        assertTrue(service.deleteSequence("seq1"));
        assertTrue(service.loadSequence("seq1").isEmpty());
    }
}
