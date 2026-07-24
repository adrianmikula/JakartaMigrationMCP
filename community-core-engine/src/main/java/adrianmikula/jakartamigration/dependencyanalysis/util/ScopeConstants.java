package adrianmikula.jakartamigration.dependencyanalysis.util;

import java.util.Map;
import java.util.Set;

/**
 * Shared scope constants and mappings.
 * Consolidates scope handling across scanners.
 */
public final class ScopeConstants {

    // Maven scopes
    public static final String COMPILE = "compile";
    public static final String PROVIDED = "provided";
    public static final String RUNTIME = "runtime";
    public static final String TEST = "test";
    public static final String SYSTEM = "system";
    public static final String IMPORT = "import";

    // All Maven scopes
    public static final Set<String> ALL_MAVEN_SCOPES = Set.of(
            COMPILE, PROVIDED, RUNTIME, TEST, SYSTEM, IMPORT
    );

    // Default scopes for command-line dependency tree
    public static final Set<String> DEFAULT_MAVEN_SCOPES = Set.of(
            COMPILE, PROVIDED, RUNTIME, TEST
    );

    // Gradle configurations that map to compile scope
    // Note: 'compile' was removed in Gradle 7+ and is not included here
    public static final Set<String> GRADLE_COMPILE_CONFIGS = Set.of(
            "implementation", "api", "compileClasspath"
    );

    // Gradle configurations that map to test scope
    public static final Set<String> GRADLE_TEST_CONFIGS = Set.of(
            "testImplementation", "testCompileClasspath", "testRuntime",
            "testRuntimeClasspath", "testRuntimeOnly", "testCompileOnly"
    );

    // Gradle configurations that map to provided scope
    public static final Set<String> GRADLE_PROVIDED_CONFIGS = Set.of(
            "compileOnly"
    );

    // Gradle configurations that map to runtime scope
    public static final Set<String> GRADLE_RUNTIME_CONFIGS = Set.of(
            "runtime", "runtimeClasspath", "runtimeOnly"
    );

    // Resolvable Gradle configurations for the --configuration CLI flag.
    // Only classpath configurations can be resolved by Gradle's dependencies task.
    // Dependency buckets like 'implementation' and 'api' are NOT resolvable and
    // will cause "Configuration with name 'implementation' not found" errors.
    public static final Set<String> GRADLE_RESOLVABLE_CONFIGS = Set.of(
            "compileClasspath", "runtimeClasspath",
            "testCompileClasspath", "testRuntimeClasspath"
    );

    // Configuration to Maven scope mapping
    private static final Map<String, String> CONFIGURATION_SCOPE_MAP = Map.ofEntries(
            Map.entry("implementation", COMPILE),
            Map.entry("api", COMPILE),
            Map.entry("compile", COMPILE),
            Map.entry("compileClasspath", COMPILE),
            Map.entry("runtime", RUNTIME),
            Map.entry("runtimeClasspath", RUNTIME),
            Map.entry("runtimeOnly", RUNTIME),
            Map.entry("testImplementation", TEST),
            Map.entry("testCompileClasspath", TEST),
            Map.entry("testRuntime", TEST),
            Map.entry("testRuntimeClasspath", TEST),
            Map.entry("testRuntimeOnly", TEST),
            Map.entry("testCompileOnly", TEST),
            Map.entry("compileOnly", PROVIDED),
            Map.entry("annotationProcessor", COMPILE),
            Map.entry("kapt", COMPILE),
            Map.entry("implementation.platform", COMPILE),
            Map.entry("api.platform", COMPILE)
    );

    private ScopeConstants() {
        // Constants class
    }

    /**
     * Maps a Gradle configuration name to Maven scope.
     */
    public static String mapConfigurationToScope(String configuration) {
        return CONFIGURATION_SCOPE_MAP.getOrDefault(configuration, COMPILE);
    }

    /**
     * Checks if a scope is a compile-like scope.
     */
    public static boolean isCompileScope(String scope) {
        return COMPILE.equals(scope) || PROVIDED.equals(scope);
    }

    /**
     * Checks if a scope is a test scope.
     */
    public static boolean isTestScope(String scope) {
        return TEST.equals(scope);
    }

    /**
     * Checks if a scope is a runtime scope.
     */
    public static boolean isRuntimeScope(String scope) {
        return RUNTIME.equals(scope);
    }

    /**
     * Normalizes a scope string to a standard Maven scope.
     */
    public static String normalizeScope(String scope) {
        if (scope == null || scope.isEmpty()) {
            return COMPILE;
        }
        return switch (scope.toLowerCase()) {
            case "compile", "compileonly" -> COMPILE;
            case "provided" -> PROVIDED;
            case "runtime", "runtimeonly" -> RUNTIME;
            case "test", "testimplementation", "testruntime" -> TEST;
            case "system" -> SYSTEM;
            case "import" -> IMPORT;
            default -> COMPILE;
        };
    }
}
