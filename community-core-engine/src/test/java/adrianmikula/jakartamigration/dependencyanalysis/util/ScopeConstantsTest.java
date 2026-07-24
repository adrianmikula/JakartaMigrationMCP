package adrianmikula.jakartamigration.dependencyanalysis.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for ScopeConstants — validates scope definitions, configuration mappings,
 * and the critical distinction between resolvable and non-resolvable Gradle configurations.
 */
class ScopeConstantsTest {

    // ── Resolvable vs Non-Resolvable Configuration Tests ─────────────────────

    @Test
    @DisplayName("GRADLE_RESOLVABLE_CONFIGS should only contain classpath configurations")
    void resolvableConfigsShouldOnlyContainClasspathConfigs() {
        assertThat(ScopeConstants.GRADLE_RESOLVABLE_CONFIGS)
            .containsExactlyInAnyOrder(
                "compileClasspath",
                "runtimeClasspath",
                "testCompileClasspath",
                "testRuntimeClasspath"
            );
    }

    @Test
    @DisplayName("GRADLE_RESOLVABLE_CONFIGS should NOT contain dependency bucket configurations")
    void resolvableConfigsShouldNotContainBuckets() {
        assertThat(ScopeConstants.GRADLE_RESOLVABLE_CONFIGS)
            .doesNotContain("implementation", "api", "compileOnly", "runtimeOnly",
                "testImplementation", "testCompileOnly", "testRuntimeOnly");
    }

    @Test
    @DisplayName("GRADLE_COMPILE_CONFIGS should contain both buckets and classpath configs")
    void compileConfigsShouldContainBothBucketsAndClasspath() {
        assertThat(ScopeConstants.GRADLE_COMPILE_CONFIGS)
            .contains("implementation", "api", "compileClasspath");
    }

    @Test
    @DisplayName("GRADLE_COMPILE_CONFIGS should NOT contain deprecated 'compile' configuration")
    void compileConfigsShouldNotContainDeprecatedCompile() {
        assertThat(ScopeConstants.GRADLE_COMPILE_CONFIGS)
            .doesNotContain("compile");
    }

    // ── Maven Scope Constants ────────────────────────────────────────────────

    @Test
    @DisplayName("Maven scope constants should have correct values")
    void mavenScopeConstantsShouldHaveCorrectValues() {
        assertThat(ScopeConstants.COMPILE).isEqualTo("compile");
        assertThat(ScopeConstants.PROVIDED).isEqualTo("provided");
        assertThat(ScopeConstants.RUNTIME).isEqualTo("runtime");
        assertThat(ScopeConstants.TEST).isEqualTo("test");
        assertThat(ScopeConstants.SYSTEM).isEqualTo("system");
        assertThat(ScopeConstants.IMPORT).isEqualTo("import");
    }

    @Test
    @DisplayName("DEFAULT_MAVEN_SCOPES should include compile, provided, runtime, test")
    void defaultMavenScopesShouldIncludeCoreScopes() {
        assertThat(ScopeConstants.DEFAULT_MAVEN_SCOPES)
            .containsExactlyInAnyOrder("compile", "provided", "runtime", "test");
    }

    @Test
    @DisplayName("ALL_MAVEN_SCOPES should include system and import")
    void allMavenScopesShouldIncludeAll() {
        assertThat(ScopeConstants.ALL_MAVEN_SCOPES)
            .contains("system", "import");
    }

    // ── mapConfigurationToScope Tests ────────────────────────────────────────

    @Test
    @DisplayName("mapConfigurationToScope should map implementation to compile")
    void mapImplementationToCompile() {
        assertThat(ScopeConstants.mapConfigurationToScope("implementation"))
            .isEqualTo("compile");
    }

    @Test
    @DisplayName("mapConfigurationToScope should map api to compile")
    void mapApiToCompile() {
        assertThat(ScopeConstants.mapConfigurationToScope("api"))
            .isEqualTo("compile");
    }

    @Test
    @DisplayName("mapConfigurationToScope should map compileClasspath to compile")
    void mapCompileClasspathToCompile() {
        assertThat(ScopeConstants.mapConfigurationToScope("compileClasspath"))
            .isEqualTo("compile");
    }

    @Test
    @DisplayName("mapConfigurationToScope should map runtimeClasspath to runtime")
    void mapRuntimeClasspathToRuntime() {
        assertThat(ScopeConstants.mapConfigurationToScope("runtimeClasspath"))
            .isEqualTo("runtime");
    }

    @Test
    @DisplayName("mapConfigurationToScope should map testImplementation to test")
    void mapTestImplementationToTest() {
        assertThat(ScopeConstants.mapConfigurationToScope("testImplementation"))
            .isEqualTo("test");
    }

    @Test
    @DisplayName("mapConfigurationToScope should map compileOnly to provided")
    void mapCompileOnlyToProvided() {
        assertThat(ScopeConstants.mapConfigurationToScope("compileOnly"))
            .isEqualTo("provided");
    }

    @Test
    @DisplayName("mapConfigurationToScope should return compile for unknown configurations")
    void mapUnknownToCompile() {
        assertThat(ScopeConstants.mapConfigurationToScope("unknownConfig"))
            .isEqualTo("compile");
    }

    // ── isCompileScope / isTestScope / isRuntimeScope Tests ──────────────────

    @Test
    @DisplayName("isCompileScope should return true for compile and provided")
    void isCompileScopeShouldMatchCompileAndProvided() {
        assertThat(ScopeConstants.isCompileScope("compile")).isTrue();
        assertThat(ScopeConstants.isCompileScope("provided")).isTrue();
    }

    @Test
    @DisplayName("isCompileScope should return false for test and runtime")
    void isCompileScopeShouldRejectTestAndRuntime() {
        assertThat(ScopeConstants.isCompileScope("test")).isFalse();
        assertThat(ScopeConstants.isCompileScope("runtime")).isFalse();
    }

    @Test
    @DisplayName("isTestScope should return true only for test")
    void isTestScopeShouldMatchOnlyTest() {
        assertThat(ScopeConstants.isTestScope("test")).isTrue();
        assertThat(ScopeConstants.isTestScope("compile")).isFalse();
        assertThat(ScopeConstants.isTestScope("runtime")).isFalse();
    }

    @Test
    @DisplayName("isRuntimeScope should return true only for runtime")
    void isRuntimeScopeShouldMatchOnlyRuntime() {
        assertThat(ScopeConstants.isRuntimeScope("runtime")).isTrue();
        assertThat(ScopeConstants.isRuntimeScope("compile")).isFalse();
        assertThat(ScopeConstants.isRuntimeScope("test")).isFalse();
    }

    // ── normalizeScope Tests ─────────────────────────────────────────────────

    @Test
    @DisplayName("normalizeScope should normalize null to compile")
    void normalizeScopeShouldDefaultNullToCompile() {
        assertThat(ScopeConstants.normalizeScope(null)).isEqualTo("compile");
    }

    @Test
    @DisplayName("normalizeScope should normalize empty string to compile")
    void normalizeScopeShouldDefaultEmptyToCompile() {
        assertThat(ScopeConstants.normalizeScope("")).isEqualTo("compile");
    }

    @Test
    @DisplayName("normalizeScope should handle case-insensitive input")
    void normalizeScopeShouldHandleCaseInsensitive() {
        assertThat(ScopeConstants.normalizeScope("COMPILE")).isEqualTo("compile");
        assertThat(ScopeConstants.normalizeScope("Test")).isEqualTo("test");
        assertThat(ScopeConstants.normalizeScope("RUNTIME")).isEqualTo("runtime");
    }

    @Test
    @DisplayName("normalizeScope should map compileOnly to compile")
    void normalizeScopeShouldMapCompileOnlyToCompile() {
        assertThat(ScopeConstants.normalizeScope("compileonly")).isEqualTo("compile");
    }

    @Test
    @DisplayName("normalizeScope should map testImplementation to test")
    void normalizeScopeShouldMapTestImplementationToTest() {
        assertThat(ScopeConstants.normalizeScope("testimplementation")).isEqualTo("test");
    }

    @Test
    @DisplayName("normalizeScope should map runtimeOnly to runtime")
    void normalizeScopeShouldMapRuntimeOnlyToRuntime() {
        assertThat(ScopeConstants.normalizeScope("runtimeonly")).isEqualTo("runtime");
    }

    @Test
    @DisplayName("normalizeScope should return compile for unknown scope")
    void normalizeScopeShouldReturnCompileForUnknown() {
        assertThat(ScopeConstants.normalizeScope("unknown")).isEqualTo("compile");
    }

    // ── GRADLE_TEST_CONFIGS and GRADLE_RUNTIME_CONFIGS ───────────────────────

    @Test
    @DisplayName("GRADLE_TEST_CONFIGS should contain test-related configurations")
    void testConfigsShouldContainTestConfigurations() {
        assertThat(ScopeConstants.GRADLE_TEST_CONFIGS)
            .contains("testImplementation", "testCompileClasspath",
                "testRuntime", "testRuntimeClasspath", "testRuntimeOnly", "testCompileOnly");
    }

    @Test
    @DisplayName("GRADLE_RUNTIME_CONFIGS should contain runtime configurations")
    void runtimeConfigsShouldContainRuntimeConfigurations() {
        assertThat(ScopeConstants.GRADLE_RUNTIME_CONFIGS)
            .contains("runtime", "runtimeClasspath", "runtimeOnly");
    }

    @Test
    @DisplayName("GRADLE_PROVIDED_CONFIGS should contain compileOnly")
    void providedConfigsShouldContainCompileOnly() {
        assertThat(ScopeConstants.GRADLE_PROVIDED_CONFIGS)
            .containsExactly("compileOnly");
    }
}
