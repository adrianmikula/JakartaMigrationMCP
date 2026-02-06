package adrianmikula.jakartamigration.intellij.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

/**
 * Registry of all Jakarta Migration MCP tools with their metadata.
 * Provides tool definitions for IntelliJ AI Assistant integration.
 */
public class McpToolRegistry {

    private static final String SERVER_NAME = "jakarta-migration-mcp";
    private static final String SERVER_VERSION = "1.0.0";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Get all registered MCP tools with their definitions.
     * @return List of tool definitions for AI Assistant
     */
    public static List<McpToolDefinition> getAllTools() {
        List<McpToolDefinition> tools = new ArrayList<>();

        tools.add(createAnalyzeJakartaReadinessTool());
        tools.add(createAnalyzeMigrationImpactTool());
        tools.add(createDetectBlockersTool());
        tools.add(createRecommendVersionsTool());
        tools.add(createApplyOpenRewriteRefactoringTool());
        tools.add(createScanBinaryDependencyTool());
        tools.add(createUpdateDependencyTool());
        tools.add(createGenerateMigrationPlanTool());
        tools.add(createValidateMigrationTool());

        return Collections.unmodifiableList(tools);
    }

    /**
     * Tool: analyzeJakartaReadiness
     * Analyzes a project's readiness for Jakarta EE migration.
     */
    private static McpToolDefinition createAnalyzeJakartaReadinessTool() {
        Map<String, McpToolDefinition.PropertySchema> properties = new LinkedHashMap<>();
        properties.put("projectPath", McpToolDefinition.PropertySchema.string(
                "Absolute path to the project root directory to analyze"
        ));
        properties.put("includeTransitiveDependencies", McpToolDefinition.PropertySchema.booleanSchema(
                "Whether to include transitive dependencies in the analysis"
        ));
        properties.put("analysisLevel", McpToolDefinition.PropertySchema.string(
                "Depth of analysis: 'basic', 'detailed', or 'comprehensive'"
        ));

        McpToolDefinition.InputSchema inputSchema = new McpToolDefinition.InputSchema(properties,
                Arrays.asList("projectPath"));

        return new McpToolDefinition(
                "analyzeJakartaReadiness",
                "Analyzes a Java project's readiness for migration from Java EE 8 (javax.*) to Jakarta EE 9+ (jakarta.*). " +
                "Scans source code, dependencies, and configuration to identify migration blockers, " +
                "affected packages, and provides a comprehensive migration readiness score.",
                inputSchema,
                SERVER_NAME,
                SERVER_VERSION
        );
    }

    /**
     * Tool: analyzeMigrationImpact
     * Analyzes the impact of Jakarta migration on a project.
     */
    private static McpToolDefinition createAnalyzeMigrationImpactTool() {
        Map<String, McpToolDefinition.PropertySchema> properties = new LinkedHashMap<>();
        properties.put("projectPath", McpToolDefinition.PropertySchema.string(
                "Absolute path to the project root directory"
        ));
        properties.put("scope", McpToolDefinition.PropertySchema.string(
                "Analysis scope: 'dependencies', 'code', 'configuration', or 'all'"
        ));
        properties.put("includeRiskAssessment", McpToolDefinition.PropertySchema.booleanSchema(
                "Whether to include detailed risk assessment"
        ));
        properties.put("outputFormat", McpToolDefinition.PropertySchema.string(
                "Output format: 'summary', 'detailed', or 'json'"
        ));

        McpToolDefinition.InputSchema inputSchema = new McpToolDefinition.InputSchema(properties,
                Arrays.asList("projectPath"));

        return new McpToolDefinition(
                "analyzeMigrationImpact",
                "Provides detailed analysis of migration impact including affected dependencies, " +
                "breaking changes, risk assessment, and estimated migration effort. " +
                "Helps understand the scope and complexity of the migration effort.",
                inputSchema,
                SERVER_NAME,
                SERVER_VERSION
        );
    }

    /**
     * Tool: detectBlockers
     * Detects migration blockers in a project.
     */
    private static McpToolDefinition createDetectBlockersTool() {
        Map<String, McpToolDefinition.PropertySchema> properties = new LinkedHashMap<>();
        properties.put("projectPath", McpToolDefinition.PropertySchema.string(
                "Absolute path to the project root directory"
        ));
        properties.put("severityLevel", McpToolDefinition.PropertySchema.string(
                "Minimum severity level to report: 'error', 'warning', or 'info'"
        ));

        McpToolDefinition.InputSchema inputSchema = new McpToolDefinition.InputSchema(properties,
                Arrays.asList("projectPath"));

        return new McpToolDefinition(
                "detectBlockers",
                "Identifies migration blockers that prevent successful Jakarta EE migration. " +
                "Reports critical issues like incompatible dependencies, unsupported APIs, " +
                "and configuration problems that must be resolved before migration.",
                inputSchema,
                SERVER_NAME,
                SERVER_VERSION
        );
    }

    /**
     * Tool: recommendVersions
     * Recommends compatible versions for dependencies.
     */
    private static McpToolDefinition createRecommendVersionsTool() {
        Map<String, McpToolDefinition.PropertySchema> properties = new LinkedHashMap<>();
        properties.put("projectPath", McpToolDefinition.PropertySchema.string(
                "Absolute path to the project root directory"
        ));
        properties.put("includeAlternatives", McpToolDefinition.PropertySchema.booleanSchema(
                "Whether to include alternative dependency recommendations"
        ));
        properties.put("targetJakartaVersion", McpToolDefinition.PropertySchema.string(
                "Target Jakarta EE version: '9', '9.1', '10', or '11'"
        ));

        McpToolDefinition.InputSchema inputSchema = new McpToolDefinition.InputSchema(properties,
                Arrays.asList("projectPath"));

        return new McpToolDefinition(
                "recommendVersions",
                "Analyzes project dependencies and recommends compatible Jakarta EE versions. " +
                "Provides version upgrade paths for libraries that have Jakarta EE variants, " +
                "suggests alternatives for deprecated dependencies, and identifies version conflicts.",
                inputSchema,
                SERVER_NAME,
                SERVER_VERSION
        );
    }

    /**
     * Tool: applyOpenRewriteRefactoring
     * Applies OpenRewrite refactoring for javax to jakarta migration.
     */
    private static McpToolDefinition createApplyOpenRewriteRefactoringTool() {
        Map<String, McpToolDefinition.PropertySchema> properties = new LinkedHashMap<>();
        properties.put("projectPath", McpToolDefinition.PropertySchema.string(
                "Absolute path to the project root directory"
        ));
        properties.put("filePatterns", McpToolDefinition.PropertySchema.array(
                "Glob patterns for files to refactor (e.g., ['**/*.java', '**/*.xml'])"
        ));
        properties.put("dryRun", McpToolDefinition.PropertySchema.booleanSchema(
                "If true, only preview changes without applying them"
        ));
        properties.put("skipTests", McpToolDefinition.PropertySchema.booleanSchema(
                "If true, skip refactoring test files"
        ));

        McpToolDefinition.InputSchema inputSchema = new McpToolDefinition.InputSchema(properties,
                Arrays.asList("projectPath", "filePatterns"));

        return new McpToolDefinition(
                "applyOpenRewriteRefactoring",
                "Applies OpenRewrite refactoring recipes to automatically migrate javax packages " +
                "and imports to jakarta equivalents. Supports dry-run mode to preview changes " +
                "before applying them. Can refactor both source code and configuration files.",
                inputSchema,
                SERVER_NAME,
                SERVER_VERSION
        );
    }

    /**
     * Tool: scanBinaryDependency
     * Scans a binary JAR dependency for Jakarta EE compatibility.
     */
    private static McpToolDefinition createScanBinaryDependencyTool() {
        Map<String, McpToolDefinition.PropertySchema> properties = new LinkedHashMap<>();
        properties.put("jarPath", McpToolDefinition.PropertySchema.string(
                "Absolute path to the JAR file to scan"
        ));
        properties.put("includeMethods", McpToolDefinition.PropertySchema.booleanSchema(
                "Whether to scan for problematic method references"
        ));
        properties.put("outputDetail", McpToolDefinition.PropertySchema.string(
                "Output detail level: 'summary', 'methods', or 'full'"
        ));

        McpToolDefinition.InputSchema inputSchema = new McpToolDefinition.InputSchema(properties,
                Arrays.asList("jarPath"));

        return new McpToolDefinition(
                "scanBinaryDependency",
                "Scans a compiled JAR dependency for Jakarta EE compatibility issues. " +
                "Analyzes bytecode to identify references to javax packages that may " +
                "cause runtime issues after migration. Provides detailed reports of " +
                "problematic classes and methods.",
                inputSchema,
                SERVER_NAME,
                SERVER_VERSION
        );
    }

    /**
     * Tool: updateDependency
     * Updates a dependency to a recommended version.
     */
    private static McpToolDefinition createUpdateDependencyTool() {
        Map<String, McpToolDefinition.PropertySchema> properties = new LinkedHashMap<>();
        properties.put("projectPath", McpToolDefinition.PropertySchema.string(
                "Absolute path to the project root directory"
        ));
        properties.put("groupId", McpToolDefinition.PropertySchema.string(
                "Maven group ID of the dependency"
        ));
        properties.put("artifactId", McpToolDefinition.PropertySchema.string(
                "Maven artifact ID of the dependency"
        ));
        properties.put("currentVersion", McpToolDefinition.PropertySchema.string(
                "Current version of the dependency"
        ));
        properties.put("recommendedVersion", McpToolDefinition.PropertySchema.string(
                "Version to upgrade to"
        ));
        properties.put("updateStrategy", McpToolDefinition.PropertySchema.string(
                "Update strategy: 'immediate', 'gradual', or 'preview'"
        ));

        McpToolDefinition.InputSchema inputSchema = new McpToolDefinition.InputSchema(properties,
                Arrays.asList("projectPath", "groupId", "artifactId", "currentVersion", "recommendedVersion"));

        return new McpToolDefinition(
                "updateDependency",
                "Updates a single dependency to a recommended Jakarta-compatible version. " +
                "Modifies build configuration files (pom.xml, build.gradle) with the new version. " +
                "Supports preview mode to show changes without applying them.",
                inputSchema,
                SERVER_NAME,
                SERVER_VERSION
        );
    }

    /**
     * Tool: generateMigrationPlan
     * Generates a phased migration plan.
     */
    private static McpToolDefinition createGenerateMigrationPlanTool() {
        Map<String, McpToolDefinition.PropertySchema> properties = new LinkedHashMap<>();
        properties.put("projectPath", McpToolDefinition.PropertySchema.string(
                "Absolute path to the project root directory"
        ));
        properties.put("phases", McpToolDefinition.PropertySchema.string(
                "Number of migration phases: 'single', 'multi', or 'iterative'"
        ));
        properties.put("includeRollback", McpToolDefinition.PropertySchema.booleanSchema(
                "Whether to include rollback procedures"
        ));
        properties.put("riskTolerance", McpToolDefinition.PropertySchema.string(
                "Risk tolerance level: 'low', 'medium', or 'high'"
        ));

        McpToolDefinition.InputSchema inputSchema = new McpToolDefinition.InputSchema(properties,
                Arrays.asList("projectPath"));

        return new McpToolDefinition(
                "generateMigrationPlan",
                "Generates a detailed, phased migration plan for Jakarta EE migration. " +
                "Breaks down the migration into manageable phases with clear milestones, " +
                "dependencies between tasks, and rollback procedures for each phase.",
                inputSchema,
                SERVER_NAME,
                SERVER_VERSION
        );
    }

    /**
     * Tool: validateMigration
     * Validates migration results.
     */
    private static McpToolDefinition createValidateMigrationTool() {
        Map<String, McpToolDefinition.PropertySchema> properties = new LinkedHashMap<>();
        properties.put("projectPath", McpToolDefinition.PropertySchema.string(
                "Absolute path to the project root directory"
        ));
        properties.put("validationType", McpToolDefinition.PropertySchema.string(
                "Validation type: 'compile', 'runtime', 'all', or 'custom'"
        ));
        properties.put("customChecks", McpToolDefinition.PropertySchema.array(
                "List of custom validation checks to perform"
        ));

        McpToolDefinition.InputSchema inputSchema = new McpToolDefinition.InputSchema(properties,
                Arrays.asList("projectPath"));

        return new McpToolDefinition(
                "validateMigration",
                "Validates that migration was successful by running compile checks, " +
                "test suites, and custom validation rules. Reports any remaining issues " +
                "that need to be addressed and provides a migration completion status.",
                inputSchema,
                SERVER_NAME,
                SERVER_VERSION
        );
    }

    /**
     * Get the server metadata for MCP discovery.
     * @return Map containing server name, version, and capabilities
     */
    public static Map<String, Object> getServerMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("name", SERVER_NAME);
        metadata.put("version", SERVER_VERSION);
        metadata.put("description", "MCP server for Jakarta EE migration analysis and automation");
        metadata.put("author", "Jakarta Migration Team");
        metadata.put("vendor", "jakarta-migration.com");

        Map<String, Object> capabilities = new LinkedHashMap<>();
        capabilities.put("tools", true);
        capabilities.put("resources", false);
        capabilities.put("prompts", false);
        metadata.put("capabilities", capabilities);

        return metadata;
    }
}
