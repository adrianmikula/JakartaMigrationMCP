package adrianmikula.jakartamigration.dependencyanalysis.util;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared Gradle build file parsing utility.
 * Consolidates Gradle dependency parsing logic.
 */
public final class GradleBuildParser {

    // Comprehensive Gradle dependency pattern (from GradleMultiModuleParser)
    private static final Pattern DEPENDENCY_PATTERN = Pattern.compile(
            "(implementation|api|compile|runtime|testImplementation|testRuntime|compileOnly|runtimeOnly|" +
            "annotationProcessor|kapt|testCompileOnly|testRuntimeOnly|" +
            "implementation\\.platform|api\\.platform|compileClasspath)\\s*[(]?\\s*['\"]([^:]+):([^:]+):([^'\"]+)['\"]\\s*[)]?",
            Pattern.MULTILINE);

    // Simpler pattern for quick extraction
    private static final Pattern SIMPLE_DEPENDENCY_PATTERN = Pattern.compile(
            "['\"]([^':]+):([^':]+):([^'\"]+)['\"]",
            Pattern.MULTILINE);

    // Project-level group and version declarations (Groovy/Kotlin DSL)
    private static final Pattern GROUP_PATTERN = Pattern.compile(
            "(?:^|[\\s{])group\\s*=\\s*['\"]([^'\"]+)['\"]",
            Pattern.MULTILINE);

    private static final Pattern VERSION_PATTERN = Pattern.compile(
            "(?:^|[\\s{])version\\s*=\\s*['\"]([^'\"]+)['\"]",
            Pattern.MULTILINE);

    // Configuration to Maven scope mapping
    private static final Map<String, String> CONFIGURATION_SCOPE_MAP = Map.ofEntries(
            Map.entry("implementation", "compile"),
            Map.entry("api", "compile"),
            Map.entry("compile", "compile"),
            Map.entry("compileClasspath", "compile"),
            Map.entry("runtime", "runtime"),
            Map.entry("runtimeClasspath", "runtime"),
            Map.entry("runtimeOnly", "runtime"),
            Map.entry("testImplementation", "test"),
            Map.entry("testCompileClasspath", "test"),
            Map.entry("testRuntime", "test"),
            Map.entry("testRuntimeClasspath", "test"),
            Map.entry("testRuntimeOnly", "test"),
            Map.entry("compileOnly", "provided"),
            Map.entry("testCompileOnly", "test"),
            Map.entry("annotationProcessor", "compile"),
            Map.entry("kapt", "compile"),
            Map.entry("implementation.platform", "compile"),
            Map.entry("api.platform", "compile")
    );

    private GradleBuildParser() {
        // Utility class
    }

    /**
     * Parses Gradle build content and returns dependencies as a list of maps.
     * Each map contains: configuration, groupId, artifactId, version, scope.
     */
    public static List<Map<String, String>> parseDependencies(String content) {
        List<Map<String, String>> dependencies = new ArrayList<>();

        Matcher matcher = DEPENDENCY_PATTERN.matcher(content);
        while (matcher.find()) {
            String configuration = matcher.group(1);
            String groupId = matcher.group(2).trim();
            String artifactId = matcher.group(3).trim();
            String version = matcher.group(4).trim();
            String scope = mapConfigurationToScope(configuration);

            Map<String, String> dependency = new LinkedHashMap<>();
            dependency.put("configuration", configuration);
            dependency.put("groupId", groupId);
            dependency.put("artifactId", artifactId);
            dependency.put("version", version);
            dependency.put("scope", scope);
            dependencies.add(dependency);
        }

        return dependencies;
    }

    /**
     * Quick extraction of groupId:artifactId:version tuples.
     */
    public static List<String[]> extractCoordinates(String content) {
        List<String[]> coordinates = new ArrayList<>();

        Matcher matcher = SIMPLE_DEPENDENCY_PATTERN.matcher(content);
        while (matcher.find()) {
            coordinates.add(new String[]{
                    matcher.group(1).trim(),
                    matcher.group(2).trim(),
                    matcher.group(3).trim()
            });
        }

        return coordinates;
    }

    /**
     * Maps a Gradle configuration name to Maven scope.
     */
    public static String mapConfigurationToScope(String configuration) {
        return CONFIGURATION_SCOPE_MAP.getOrDefault(configuration, "compile");
    }

    /**
     * Returns all supported Gradle configuration names.
     */
    public static Set<String> getSupportedConfigurations() {
        return CONFIGURATION_SCOPE_MAP.keySet();
    }

    /**
     * Extracts the project group from build.gradle(.kts) content.
     * Handles both Groovy and Kotlin DSL forms such as {@code group = "..."}.
     *
     * @return the declared group, or null if not found
     */
    public static String extractProjectGroup(String content) {
        if (content == null) return null;
        Matcher matcher = GROUP_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    /**
     * Extracts the project version from build.gradle(.kts) content.
     *
     * @return the declared version, or null if not found
     */
    public static String extractProjectVersion(String content) {
        if (content == null) return null;
        Matcher matcher = VERSION_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }
}
