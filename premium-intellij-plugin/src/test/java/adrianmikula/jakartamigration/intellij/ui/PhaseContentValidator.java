package adrianmikula.jakartamigration.intellij.ui;

import adrianmikula.jakartamigration.intellij.ui.MigrationPhasesComponent;
import adrianmikula.jakartamigration.intellij.ui.MigrationStrategyComponent.MigrationStrategy;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

/**
 * Utility class for validating phase content quality and 2026 industry standards compliance.
 * Provides comprehensive validation methods for migration phase descriptions.
 */
public class PhaseContentValidator {

    private static final Pattern JAKARTA_EE_10_PATTERN = Pattern.compile("(?i)(Jakarta EE\\s*10|Core Profile|cloud.native|microservices|API gateway|service mesh|observability|distributed tracing|chaos engineering|feature flags|blue-green|canary|infrastructure.as.code|APM|monitoring|performance profiling|static analysis|code quality|security scanning|compliance)", Pattern.CASE_INSENSITIVE);

    private static final Pattern MODERN_TOOLS_PATTERN = Pattern.compile("(?i)(OpenRewrite|Maven|Gradle|Testcontainers|JUnit\\s*5|Pact|Postman|Insomnia|Cypress|Playwright|k6|Gatling|Gremlin|Chaos Monkey|Istio|Linkerd|ArgoCD|Flagger|Spinnaker|LaunchDarkly|Split.io|SonarQube|OWASP|Snyk|ASM|Byte Buddy|Project Reactor|RxJava|Mockito|ArchUnit|JProfiler|YourKit|Terraform|Pulumi|Datadog|New Relic|Dynatrace|Prometheus|Grafana|Jaeger|OpenTelemetry|Swagger|OpenAPI)", Pattern.CASE_INSENSITIVE);

    /**
     * Validates that phase descriptions contain 2026 industry-standard content.
     * 
     * @param description The phase description to validate
     * @return true if the description contains modern industry standards
     */
    public static boolean contains2026Standards(@NotNull String description) {
        if (description == null || description.trim().isEmpty()) {
            return false;
        }

        // Check for Jakarta EE 10+ specific concepts
        boolean hasJakartaEE10 = JAKARTA_EE_10_PATTERN.matcher(description).find();

        // Check for modern migration tools and practices
        boolean hasModernTools = MODERN_TOOLS_PATTERN.matcher(description).find();

        // Check for comprehensive content indicators
        boolean isComprehensive = description.length() > 500 && // Substantial content
                description.contains("testing") && 
                description.contains("monitoring") && 
                description.contains("performance") &&
                description.contains("security");

        return hasJakartaEE10 || hasModernTools || isComprehensive;
    }

    /**
     * Validates that phase description has appropriate length for 3x expansion.
     * 
     * @param description The phase description to validate
     * @return true if the description meets length requirements
     */
    public static boolean meetsLengthRequirements(@NotNull String description) {
        if (description == null || description.trim().isEmpty()) {
            return false;
        }

        // Count words and characters for comprehensive assessment
        int wordCount = description.split("\\s+").length;
        int charCount = description.length();

        // Requirements for 3x expansion: substantial content with detailed guidance
        boolean meetsWordCount = wordCount >= 150; // At least 150 words
        boolean meetsCharCount = charCount >= 1000; // At least 1000 characters

        return meetsWordCount && meetsCharCount;
    }

    /**
     * Validates phase content for specific strategy requirements.
     * 
     * @param strategy The migration strategy
     * @param phaseIndex The phase index (0-based)
     * @param description The phase description to validate
     * @return true if the content meets strategy-specific requirements
     */
    public static boolean validatesPhaseContent(@NotNull MigrationStrategy strategy, int phaseIndex, @NotNull String description) {
        if (description == null || description.trim().isEmpty()) {
            return false;
        }

        switch (strategy) {
            case BIG_BANG:
                return validateBigBangPhase(description, phaseIndex);
            case INCREMENTAL:
                return validateIncrementalPhase(description, phaseIndex);
            case TRANSFORM:
                return validateTransformPhase(description, phaseIndex);
            case MICROSERVICES:
                return validateMicroservicesPhase(description, phaseIndex);
            case ADAPTER:
                return validateAdapterPhase(description, phaseIndex);
            case STRANGLER:
                return validateStranglerPhase(description, phaseIndex);
            default:
                return false;
        }
    }

    private static boolean validateBigBangPhase(String description, int phaseIndex) {
        // Big Bang should emphasize comprehensive planning and risk management
        return description.contains("comprehensive testing") &&
               description.contains("rollback plan") &&
               description.contains("monitoring") &&
               description.contains("backup");
    }

    private static boolean validateIncrementalPhase(String description, int phaseIndex) {
        // Incremental phases should emphasize gradual, controlled migration
        String[] expectedKeywords = switch (phaseIndex) {
            case 0 -> new String[]{"dependency", "analysis", "gradual"};
            case 1 -> new String[]{"import", "replacement", "systematic"};
            case 2 -> new String[]{"testing", "verification", "comprehensive"};
            case 3 -> new String[]{"production", "rollout", "monitoring"};
            default -> new String[]{};
        };

        for (String keyword : expectedKeywords) {
            if (!description.toLowerCase().contains(keyword)) {
                return false;
            }
        }
        return true;
    }

    private static boolean validateTransformPhase(String description, int phaseIndex) {
        // Transform phases should emphasize runtime transformation and build tools
        String[] expectedKeywords = switch (phaseIndex) {
            case 0 -> new String[]{"build", "configuration", "BOM"};
            case 1 -> new String[]{"runtime", "transformation", "Eclipse Transformer"};
            case 2 -> new String[]{"gradual", "migration", "module"};
            case 3 -> new String[]{"cleanup", "optimization", "monitoring"};
            default -> new String[]{};
        };

        for (String keyword : expectedKeywords) {
            if (!description.toLowerCase().contains(keyword)) {
                return false;
            }
        }
        return true;
    }

    private static boolean validateMicroservicesPhase(String description, int phaseIndex) {
        // Microservices phases should emphasize service-oriented concepts
        String[] expectedKeywords = switch (phaseIndex) {
            case 0 -> new String[]{"service", "inventory", "mapping"};
            case 1 -> new String[]{"shared", "libraries", "common"};
            case 2 -> new String[]{"service.by.service", "individual", "testing"};
            case 3 -> new String[]{"integration", "testing", "communication"};
            default -> new String[]{};
        };

        for (String keyword : expectedKeywords) {
            if (!description.toLowerCase().contains(keyword)) {
                return false;
            }
        }
        return true;
    }

    private static boolean validateAdapterPhase(String description, int phaseIndex) {
        // Adapter phases should emphasize design patterns and interfaces
        String[] expectedKeywords = switch (phaseIndex) {
            case 0 -> new String[]{"adapter", "interface", "design"};
            case 1 -> new String[]{"implementation", "translation", "jakarta"};
            case 2 -> new String[]{"gradual", "replacement", "direct"};
            case 3 -> new String[]{"cleanup", "removal", "dependencies"};
            default -> new String[]{};
        };

        for (String keyword : expectedKeywords) {
            if (!description.toLowerCase().contains(keyword)) {
                return false;
            }
        }
        return true;
    }

    private static boolean validateStranglerPhase(String description, int phaseIndex) {
        // Strangler phases should emphasize gradual replacement and service boundaries
        String[] expectedKeywords = switch (phaseIndex) {
            case 0 -> new String[]{"service", "boundaries", "routing"};
            case 1 -> new String[]{"extraction", "jakarta", "migration"};
            case 2 -> new String[]{"data", "consistency", "decomposition"};
            case 3 -> new String[]{"decommission", "legacy", "cleanup"};
            default -> new String[]{};
        };

        for (String keyword : expectedKeywords) {
            if (!description.toLowerCase().contains(keyword)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Validates that phase steps are comprehensive and actionable.
     * 
     * @param steps The phase steps array to validate
     * @return true if the steps are comprehensive and actionable
     */
    public static boolean validatesPhaseSteps(String[] steps) {
        if (steps == null || steps.length == 0) {
            return false;
        }

        // Each step should be specific and actionable
        for (String step : steps) {
            if (step == null || step.trim().isEmpty() || step.length() < 10) {
                return false;
            }
        }

        // Should have at least 3 steps for comprehensive coverage
        return steps.length >= 3;
    }

    /**
     * Generates a validation report for phase content.
     * 
     * @param strategy The migration strategy
     * @param phaseIndex The phase index
     * @param description The phase description
     * @return Validation report with recommendations
     */
    public static String generateValidationReport(@NotNull MigrationStrategy strategy, int phaseIndex, @NotNull String description) {
        StringBuilder report = new StringBuilder();
        report.append("Phase Validation Report\\n");
        report.append("Strategy: ").append(strategy.name()).append("\\n");
        report.append("Phase Index: ").append(phaseIndex).append("\\n");
        report.append("Description Length: ").append(description.length()).append(" characters\\n");
        report.append("Word Count: ").append(description.split("\\s+").length).append("\\n");
        
        boolean meets2026Standards = contains2026Standards(description);
        boolean meetsLengthRequirements = meetsLengthRequirements(description);
        boolean validatesContent = validatesPhaseContent(strategy, phaseIndex, description);
        
        report.append("2026 Standards Compliance: ").append(meets2026Standards ? "PASS" : "FAIL").append("\\n");
        report.append("Length Requirements: ").append(meetsLengthRequirements ? "PASS" : "FAIL").append("\\n");
        report.append("Content Validation: ").append(validatesContent ? "PASS" : "FAIL").append("\\n");
        
        if (!meets2026Standards) {
            report.append("\\nRecommendations:\\n");
            report.append("- Add more Jakarta EE 10+ specific concepts\\n");
            report.append("- Include modern migration tools and practices\\n");
            report.append("- Enhance cloud-native deployment guidance\\n");
        }
        
        if (!meetsLengthRequirements) {
            report.append("\\nRecommendations:\\n");
            report.append("- Expand content to meet 3x length requirements\\n");
            report.append("- Add more detailed examples and best practices\\n");
        }
        
        return report.toString();
    }
}
