package adrianmikula.jakartamigration.experiment.service;

import adrianmikula.jakartamigration.experiment.domain.ExperimentResult;
import adrianmikula.jakartamigration.experiment.domain.ExperimentStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HistoryService {
    private static final Logger LOG = LoggerFactory.getLogger(HistoryService.class);

    private final Path experimentsDir;
    private final Path historyDir;

    public HistoryService(Path projectRoot) {
        this.experimentsDir = projectRoot.resolve(".experiments");
        this.historyDir = experimentsDir.resolve("history");
    }

    public synchronized void recordRun(ExperimentResult result) throws IOException {
        ensureDirectoriesExist();
        Path file = historyDir.resolve(result.runId() + ".json");
        String json = toJson(result);
        Files.writeString(file, json, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public synchronized Optional<ExperimentResult> getRun(String runId) throws IOException {
        Path file = historyDir.resolve(runId + ".json");
        if (!Files.exists(file)) return Optional.empty();
        String json = Files.readString(file, StandardCharsets.UTF_8);
        return Optional.of(fromJson(json, ExperimentResult.class));
    }

    public synchronized List<ExperimentResult> listRuns(
        Optional<String> sequenceNameFilter,
        Optional<ExperimentStatus> statusFilter,
        int limit
    ) throws IOException {
        ensureDirectoriesExist();
        try (Stream<Path> stream = Files.list(historyDir)) {
            return stream
                .filter(p -> p.toString().endsWith(".json"))
                .map(p -> {
                    try {
                        String json = Files.readString(p, StandardCharsets.UTF_8);
                        return fromJson(json, ExperimentResult.class);
                    } catch (Exception e) {
                        LOG.warn("Skipping corrupted history file: {}", p.getFileName(), e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .filter(result -> sequenceNameFilter.isEmpty() || sequenceNameFilter.get().equals(result.sequenceName()))
                .filter(result -> statusFilter.isEmpty() || statusFilter.get().equals(result.status()))
                .sorted(Comparator.comparing(ExperimentResult::startedAt).reversed())
                .limit(limit)
                .collect(Collectors.toList());
        }
    }

    private void ensureDirectoriesExist() throws IOException {
        Files.createDirectories(historyDir);
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

    private <T> T fromJson(String json, Class<T> clazz) throws IOException {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                .readValue(json, clazz);
        } catch (Exception e) {
            throw new IOException("Failed to deserialize JSON", e);
        }
    }
}
