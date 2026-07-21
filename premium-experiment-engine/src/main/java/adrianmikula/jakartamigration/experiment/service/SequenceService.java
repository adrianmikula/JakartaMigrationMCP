package adrianmikula.jakartamigration.experiment.service;

import adrianmikula.jakartamigration.experiment.domain.MigrationSequence;
import adrianmikula.jakartamigration.experiment.domain.SequenceStep;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SequenceService {
    private final Path experimentsDir;
    private final Path sequencesDir;

    public SequenceService(Path projectRoot) {
        this.experimentsDir = projectRoot.resolve(".experiments");
        this.sequencesDir = experimentsDir.resolve("sequences");
    }

    public synchronized void saveSequence(MigrationSequence sequence) throws IOException {
        ensureDirectoriesExist();
        Path file = sequencesDir.resolve(sequence.name() + ".json");
        String json = toJson(sequence);
        Files.writeString(file, json, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public synchronized Optional<MigrationSequence> loadSequence(String name) throws IOException {
        Path file = sequencesDir.resolve(name + ".json");
        if (!Files.exists(file)) return Optional.empty();
        String json = Files.readString(file, StandardCharsets.UTF_8);
        return Optional.of(fromJson(json, MigrationSequence.class));
    }

    public synchronized List<MigrationSequence> listSequences(Optional<String> tagFilter) throws IOException {
        ensureDirectoriesExist();
        try (Stream<Path> stream = Files.list(sequencesDir)) {
            return stream
                .filter(p -> p.toString().endsWith(".json"))
                .map(p -> {
                    try {
                        String json = Files.readString(p, StandardCharsets.UTF_8);
                        return fromJson(json, MigrationSequence.class);
                    } catch (IOException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .filter(seq -> tagFilter.isEmpty() || seq.tags().contains(tagFilter.get()))
                .sorted(Comparator.comparing(MigrationSequence::createdAt).reversed())
                .collect(Collectors.toList());
        }
    }

    public synchronized MigrationSequence appendStep(String name, SequenceStep step) throws IOException {
        MigrationSequence existing = loadSequence(name)
            .orElseThrow(() -> new NoSuchElementException("Sequence not found: " + name));
        List<SequenceStep> updated = new ArrayList<>(existing.steps());
        updated.add(step);
        MigrationSequence updatedSeq = new MigrationSequence(
            existing.name(),
            existing.description(),
            updated,
            existing.createdAt(),
            existing.tags()
        );
        saveSequence(updatedSeq);
        return updatedSeq;
    }

    public synchronized boolean deleteSequence(String name) throws IOException {
        Path file = sequencesDir.resolve(name + ".json");
        return Files.deleteIfExists(file);
    }

    private void ensureDirectoriesExist() throws IOException {
        Files.createDirectories(sequencesDir);
    }

    private String toJson(Object obj) throws IOException {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                .writeValueAsString(obj);
        } catch (Exception e) {
            throw new IOException("Failed to serialize to JSON", e);
        }
    }

    private <T> T fromJson(String json, Class<T> clazz) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                .readValue(json, clazz);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize JSON", e);
        }
    }
}
