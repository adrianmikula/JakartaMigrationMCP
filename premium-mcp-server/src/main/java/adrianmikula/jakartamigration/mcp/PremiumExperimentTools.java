package adrianmikula.jakartamigration.mcp;

import adrianmikula.jakartamigration.config.FeatureFlag;
import adrianmikula.jakartamigration.config.FeatureFlagsService;
import adrianmikula.jakartamigration.experiment.domain.ExperimentStatus;
import adrianmikula.jakartamigration.experiment.domain.ExperimentResult;
import adrianmikula.jakartamigration.experiment.domain.MigrationSequence;
import adrianmikula.jakartamigration.experiment.domain.SequenceStep;
import adrianmikula.jakartamigration.experiment.mcp.ExperimentTools;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import adrianmikula.jakartamigration.mcp.util.JsonUtils;

/**
 * Premium MCP Tools for Migration Sequence Experimentation.
 *
 * These tools provide experiment engine capabilities through MCP interface.
 *
 * PREMIUM TOOLS - Requires JetBrains Marketplace subscription
 */
@Component
@Slf4j
public class PremiumExperimentTools {

    private final ExperimentTools experimentTools;
    private final FeatureFlagsService featureFlagsService;

    public PremiumExperimentTools(ExperimentTools experimentTools, FeatureFlagsService featureFlagsService) {
        this.experimentTools = experimentTools;
        this.featureFlagsService = featureFlagsService;
    }

    @McpTool(name = "createMigrationSequence", description = "Creates a new named migration sequence with ordered steps. Returns the created sequence JSON. Requires PREMIUM license.")
    public String createMigrationSequence(
            @McpToolParam(description = "Name for the sequence", required = true) String name,
            @McpToolParam(description = "Description of the sequence", required = false) String description,
            @McpToolParam(description = "JSON array of sequence steps", required = true) String stepsJson,
            @McpToolParam(description = "Comma-separated tags", required = false) String tags) {
        featureFlagsService.requireEnabled(FeatureFlag.EXPERIMENT_ENGINE);
        try {
            log.info("Creating migration sequence: {}", name);

            List<SequenceStep> steps = JsonUtils.fromJsonList(stepsJson, SequenceStep.class);
            List<String> tagList = tags != null && !tags.isEmpty()
                ? List.of(tags.split(","))
                : List.of();

            MigrationSequence sequence = experimentTools.createMigrationSequence(name, description, steps, tagList);
            return JsonUtils.toJson(sequence);
        } catch (Exception e) {
            log.error("Unexpected error creating migration sequence", e);
            return JsonUtils.createErrorResponse("Unexpected error: " + e.getMessage());
        }
    }

    @McpTool(name = "addSequenceStep", description = "Appends a step to an existing migration sequence. Returns the updated sequence JSON. Requires PREMIUM license.")
    public String addSequenceStep(
            @McpToolParam(description = "Name of the sequence", required = true) String sequenceName,
            @McpToolParam(description = "JSON of the step to add", required = true) String stepJson) {
        featureFlagsService.requireEnabled(FeatureFlag.EXPERIMENT_ENGINE);
        try {
            log.info("Adding step to sequence: {}", sequenceName);

            SequenceStep step = JsonUtils.fromJson(stepJson, SequenceStep.class);
            MigrationSequence updated = experimentTools.addSequenceStep(sequenceName, step);
            return JsonUtils.toJson(updated);
        } catch (Exception e) {
            log.error("Unexpected error adding sequence step", e);
            return JsonUtils.createErrorResponse("Unexpected error: " + e.getMessage());
        }
    }

    @McpTool(name = "listMigrationSequences", description = "Lists saved migration sequences, optionally filtered by tag. Requires PREMIUM license.")
    public String listMigrationSequences(
            @McpToolParam(description = "Tag to filter by", required = false) String tag) {
        featureFlagsService.requireEnabled(FeatureFlag.EXPERIMENT_ENGINE);
        try {
            log.info("Listing migration sequences");

            List<MigrationSequence> sequences = experimentTools.listMigrationSequences(tag);
            return JsonUtils.toJson(sequences);
        } catch (Exception e) {
            log.error("Unexpected error listing migration sequences", e);
            return JsonUtils.createErrorResponse("Unexpected error: " + e.getMessage());
        }
    }

    @McpTool(name = "getMigrationSequence", description = "Returns full JSON of a named migration sequence. Requires PREMIUM license.")
    public String getMigrationSequence(
            @McpToolParam(description = "Name of the sequence", required = true) String name) {
        featureFlagsService.requireEnabled(FeatureFlag.EXPERIMENT_ENGINE);
        try {
            log.info("Getting migration sequence: {}", name);

            Optional<MigrationSequence> sequence = experimentTools.getMigrationSequence(name);
            return sequence.map(JsonUtils::toJson).orElseGet(() -> JsonUtils.createErrorResponse("Sequence not found: " + name));
        } catch (Exception e) {
            log.error("Unexpected error getting migration sequence", e);
            return JsonUtils.createErrorResponse("Unexpected error: " + e.getMessage());
        }
    }

    @McpTool(name = "runMigrationExperiment", description = "Executes a migration sequence in an isolated testcontainer. Returns run result JSON with runId and status. Requires PREMIUM license.")
    public String runMigrationExperiment(
            @McpToolParam(description = "Name of the sequence to run", required = true) String sequenceName,
            @McpToolParam(description = "Path to project root directory", required = true) String projectPath,
            @McpToolParam(description = "Git ref to snapshot (default HEAD)", required = false) String gitRef) {
        featureFlagsService.requireEnabled(FeatureFlag.EXPERIMENT_ENGINE);
        try {
            log.info("Running migration experiment '{}' on project: {}", sequenceName, projectPath);

            Path project = Paths.get(projectPath);
            if (!project.toFile().exists() || !project.toFile().isDirectory()) {
                return JsonUtils.createErrorResponse("Project path does not exist or is not a directory: " + projectPath);
            }

            Optional<String> ref = Optional.ofNullable(gitRef);
            ExperimentResult result = experimentTools.runMigrationExperiment(sequenceName, project, ref);
            return JsonUtils.toJson(result);
        } catch (Exception e) {
            log.error("Unexpected error running migration experiment", e);
            return JsonUtils.createErrorResponse("Unexpected error: " + e.getMessage());
        }
    }

    @McpTool(name = "getExperimentHistory", description = "Lists past experiment runs, optionally filtered by sequence name and status. Requires PREMIUM license.")
    public String getExperimentHistory(
            @McpToolParam(description = "Filter by sequence name", required = false) String sequenceName,
            @McpToolParam(description = "Filter by status (RUNNING, SUCCESS, FAILED, CANCELLED)", required = false) String status,
            @McpToolParam(description = "Maximum results to return", required = false) Integer limit) {
        featureFlagsService.requireEnabled(FeatureFlag.EXPERIMENT_ENGINE);
        try {
            log.info("Getting experiment history");

            ExperimentStatus statusEnum = status != null ? ExperimentStatus.valueOf(status) : null;
            int limitVal = limit != null ? limit : 50;

            List<ExperimentResult> runs = experimentTools.getExperimentHistory(sequenceName, statusEnum, limitVal);
            return JsonUtils.toJson(runs);
        } catch (Exception e) {
            log.error("Unexpected error getting experiment history", e);
            return JsonUtils.createErrorResponse("Unexpected error: " + e.getMessage());
        }
    }

    @McpTool(name = "getExperimentResult", description = "Returns full JSON of a specific experiment run by runId. Requires PREMIUM license.")
    public String getExperimentResult(
            @McpToolParam(description = "Run ID to retrieve", required = true) String runId) {
        featureFlagsService.requireEnabled(FeatureFlag.EXPERIMENT_ENGINE);
        try {
            log.info("Getting experiment result: {}", runId);

            Optional<ExperimentResult> result = experimentTools.getExperimentResult(runId);
            return result.map(JsonUtils::toJson).orElseGet(() -> JsonUtils.createErrorResponse("Run not found: " + runId));
        } catch (Exception e) {
            log.error("Unexpected error getting experiment result", e);
            return JsonUtils.createErrorResponse("Unexpected error: " + e.getMessage());
        }
    }

    @McpTool(name = "compareExperiments", description = "Compares two experiment runs and returns a comparison report highlighting differences. Requires PREMIUM license.")
    public String compareExperiments(
            @McpToolParam(description = "First run ID", required = true) String runIdA,
            @McpToolParam(description = "Second run ID", required = true) String runIdB) {
        featureFlagsService.requireEnabled(FeatureFlag.EXPERIMENT_ENGINE);
        try {
            log.info("Comparing experiments: {} vs {}", runIdA, runIdB);

            var report = experimentTools.compareExperiments(runIdA, runIdB);
            return JsonUtils.toJson(report);
        } catch (Exception e) {
            log.error("Unexpected error comparing experiments", e);
            return JsonUtils.createErrorResponse("Unexpected error: " + e.getMessage());
        }
    }
}
