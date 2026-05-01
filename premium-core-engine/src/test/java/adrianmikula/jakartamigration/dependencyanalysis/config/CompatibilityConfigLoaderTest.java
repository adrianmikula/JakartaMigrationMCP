package adrianmikula.jakartamigration.dependencyanalysis.config;

import adrianmikula.jakartamigration.dependencyanalysis.config.CompatibilityConfigLoader.ArtifactClassification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for CompatibilityConfigLoader classification logic.
 * Verifies that artifacts are correctly classified based on compatibility.yaml configuration.
 */
@Tag("slow")
class CompatibilityConfigLoaderTest {

    private CompatibilityConfigLoader configLoader;

    @BeforeEach
    void setUp() {
        configLoader = new CompatibilityConfigLoader();
    }

    @ParameterizedTest
    @DisplayName("Should classify JDK-provided artifacts as JDK_PROVIDED")
    @CsvSource({
        "javax.management, management-api",
        "javax.naming, naming-api",
        "javax.crypto, crypto-api",
        "javax.net, net-api",
        "javax.script, script-api",
        "javax.sql, sql-api"
    })
    void shouldClassifyJdkArtifactsAsJdkProvided(String groupId, String artifactId) {
        ArtifactClassification result = configLoader.classifyArtifact(groupId, artifactId);
        assertThat(result).isEqualTo(ArtifactClassification.JDK_PROVIDED);
    }

    @ParameterizedTest
    @DisplayName("Should classify safe library artifacts as JDK_PROVIDED")
    @CsvSource({
        "org.apache.commons, commons-lang",
        "commons-io, commons-io",
        "commons-collections, commons-collections",
        "org.apache.commons, commons-pool",
        "commons-fileupload, commons-fileupload",
        "commons-codec, commons-codec"
    })
    void shouldClassifySafeArtifactsAsJdkProvided(String groupId, String artifactId) {
        ArtifactClassification result = configLoader.classifyArtifact(groupId, artifactId);
        assertThat(result).isEqualTo(ArtifactClassification.JDK_PROVIDED);
    }

    @ParameterizedTest
    @DisplayName("Should classify upgrade-required artifacts as JAKARTA_REQUIRED")
    @CsvSource({
        "javax.servlet, javax.servlet-api",
        "javax.persistence, javax.persistence-api",
        "javax.validation, validation-api",
        "javax.ws.rs, javax.ws.rs-api",
        "javax.ejb, javax.ejb-api",
        "javax.jms, javax.jms-api",
        "javax.faces, javax.faces-api",
        "javax.enterprise, cdi-api"
    })
    void shouldClassifyUpgradeArtifactsAsJakartaRequired(String groupId, String artifactId) {
        ArtifactClassification result = configLoader.classifyArtifact(groupId, artifactId);
        assertThat(result).isEqualTo(ArtifactClassification.JAKARTA_REQUIRED);
    }

    @ParameterizedTest
    @DisplayName("Should classify review-required artifacts as CONTEXT_DEPENDENT")
    @CsvSource({
        "javax.xml.bind, jaxb-api",
        "javax.xml.ws, jaxws-api",
        "javax.mail, javax.mail-api",
        "javax.activation, activation-api",
        "javax.cache, cache-api",
        "javax.measure, measure-api",
        "javax.transaction, transaction-api"
    })
    void shouldClassifyReviewArtifactsAsContextDependent(String groupId, String artifactId) {
        ArtifactClassification result = configLoader.classifyArtifact(groupId, artifactId);
        assertThat(result).isEqualTo(ArtifactClassification.CONTEXT_DEPENDENT);
    }

    @Test
    @DisplayName("Should classify unknown artifacts as UNKNOWN")
    void shouldClassifyUnknownArtifactsAsUnknown() {
        ArtifactClassification result = configLoader.classifyArtifact("com.unknown.library", "unknown-artifact");
        assertThat(result).isEqualTo(ArtifactClassification.UNKNOWN);
    }

    @Test
    @DisplayName("Should classify null groupId as UNKNOWN")
    void shouldClassifyNullGroupIdAsUnknown() {
        ArtifactClassification result = configLoader.classifyArtifact(null, "some-artifact");
        assertThat(result).isEqualTo(ArtifactClassification.UNKNOWN);
    }

    @Test
    @DisplayName("Should classify empty groupId as UNKNOWN")
    void shouldClassifyEmptyGroupIdAsUnknown() {
        ArtifactClassification result = configLoader.classifyArtifact("", "some-artifact");
        assertThat(result).isEqualTo(ArtifactClassification.UNKNOWN);
    }

    @Test
    @DisplayName("Should match patterns with prefix matching")
    void shouldMatchPatternsWithPrefix() {
        // javax.servlet.core should match javax.servlet pattern
        ArtifactClassification result = configLoader.classifyArtifact("javax.servlet.core", "servlet-core");
        assertThat(result).isEqualTo(ArtifactClassification.JAKARTA_REQUIRED);
    }

    @Test
    @DisplayName("Should return JDK patterns list")
    void shouldReturnJdkPatterns() {
        assertThat(configLoader.getJdkPatterns()).isNotEmpty();
        assertThat(configLoader.getJdkPatterns()).contains("javax.management", "javax.crypto");
    }

    @Test
    @DisplayName("Should return safe patterns list")
    void shouldReturnSafePatterns() {
        assertThat(configLoader.getSafePatterns()).isNotEmpty();
        assertThat(configLoader.getSafePatterns()).contains("org.apache.commons");
    }

    @Test
    @DisplayName("Should return upgrade patterns list")
    void shouldReturnUpgradePatterns() {
        assertThat(configLoader.getUpgradePatterns()).isNotEmpty();
        assertThat(configLoader.getUpgradePatterns()).contains("javax.servlet", "javax.persistence");
    }

    @Test
    @DisplayName("Should return review patterns list")
    void shouldReturnReviewPatterns() {
        assertThat(configLoader.getReviewPatterns()).isNotEmpty();
        assertThat(configLoader.getReviewPatterns()).contains("javax.xml.bind", "javax.mail");
    }
}
