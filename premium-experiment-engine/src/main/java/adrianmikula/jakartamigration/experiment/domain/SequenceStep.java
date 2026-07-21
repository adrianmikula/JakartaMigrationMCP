package adrianmikula.jakartamigration.experiment.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;
import java.util.Map;

public record SequenceStep(
    SequenceStepType type,
    String recipe,
    String recipeVersion,
    String groupId,
    String artifactId,
    String version,
    String scope,
    String classifier,
    String sourceLevel,
    String targetLevel,
    List<String> transformations,
    String pluginVersion,
    List<String> tasks,
    String pattern,
    String replacement,
    String filePattern,
    String flags,
    Map<String, Object> extra
) {
    private static final List<String> DEFAULT_TRANSFORMATIONS = List.of("CLASSFILES", "XML", "PROPERTIES");
    private static final String DEFAULT_FLAGS = "g";

    public SequenceStep {
        if (extra == null) extra = Map.of();
    }

    public static SequenceStep openRewrite(String recipe) {
        return new SequenceStep(SequenceStepType.OPENREWRITE, recipe, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, Map.of());
    }

    public static SequenceStep openRewrite(String recipe, String recipeVersion) {
        return new SequenceStep(SequenceStepType.OPENREWRITE, recipe, recipeVersion, null, null, null, null, null, null, null, null, null, null, null, null, null, null, Map.of());
    }

    public static SequenceStep dependencyUpgrade(String groupId, String artifactId, String version) {
        return new SequenceStep(SequenceStepType.DEPENDENCY_UPGRADE, null, null, groupId, artifactId, version, null, null, null, null, null, null, null, null, null, null, null, Map.of());
    }

    public static SequenceStep eclipseTransformer(String sourceLevel, String targetLevel) {
        return new SequenceStep(SequenceStepType.ECLIPSE_TRANSFORMER, null, null, null, null, null, null, null, sourceLevel, targetLevel, DEFAULT_TRANSFORMATIONS, null, null, null, null, null, null, Map.of());
    }

    public static SequenceStep gradleJakartaPlugin(String pluginVersion) {
        return new SequenceStep(SequenceStepType.GRADLE_JAKARTA_PLUGIN, null, null, null, null, null, null, null, null, null, null, pluginVersion, null, null, null, null, null, Map.of());
    }

    public static SequenceStep regexReplacement(String pattern, String replacement, String filePattern) {
        return new SequenceStep(SequenceStepType.REGEX_REPLACEMENT, null, null, null, null, null, null, null, null, null, null, null, null, pattern, replacement, filePattern, DEFAULT_FLAGS, Map.of());
    }
}
