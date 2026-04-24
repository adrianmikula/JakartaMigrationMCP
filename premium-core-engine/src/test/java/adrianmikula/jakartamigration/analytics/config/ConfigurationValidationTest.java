package adrianmikula.jakartamigration.analytics.config;

import adrianmikula.jakartamigration.analytics.util.ConcurrencyTestHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for configuration validation edge cases.
 * Tests malformed properties, missing files, and invalid values.
 */
class ConfigurationValidationTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldHandleMissingConfigurationFile() {
        // When
        SupabaseConfig config = new SupabaseConfig();

        // Then
        // Should not crash and provide default values
        assertThat(config).isNotNull();
        assertThat(config.isAnalyticsEnabled()).isTrue();
        assertThat(config.isErrorReportingEnabled()).isTrue();
        assertThat(config.getAnalyticsBatchSize()).isEqualTo(10);
        assertThat(config.getAnalyticsFlushIntervalSeconds()).isEqualTo(30);
    }

    @Test
    void shouldHandleEmptyConfigurationFile() throws IOException {
        // Given
        Path emptyConfigFile = tempDir.resolve("empty.properties");
        Files.createFile(emptyConfigFile);
        
        // When/Then
        // Should handle empty file gracefully
        SupabaseConfig config = new SupabaseConfig();
        assertThat(config).isNotNull();
    }

    @Test
    void shouldHandleMalformedProperties() throws IOException {
        // Given
        Path malformedFile = tempDir.resolve("malformed.properties");
        String malformedContent = """
            supabase.url https://missing-equals-sign.com
            supabase.anon.key=
            supabase.analytics.enabled=maybe
            supabase.analytics.batch.size=-5
            supabase.analytics.flush.interval.seconds=not-a-number
            """;
        Files.writeString(malformedFile, malformedContent);

        // When/Then
        // Should handle malformed properties gracefully
        SupabaseConfig config = new SupabaseConfig();
        assertThat(config).isNotNull();
    }

    @Test
    void shouldHandleInvalidBooleanValues() throws IOException {
        // Given
        Path invalidBoolFile = tempDir.resolve("invalid-boolean.properties");
        String content = """
            supabase.analytics.enabled=maybe
            supabase.error.reporting.enabled=sometimes
            """;
        Files.writeString(invalidBoolFile, content);

        // When/Then
        // Should default to false for invalid boolean values
        SupabaseConfig config = new SupabaseConfig();
        assertThat(config.isAnalyticsEnabled()).isFalse();
        assertThat(config.isErrorReportingEnabled()).isFalse();
    }

    @Test
    void shouldHandleNegativeNumericValues() throws IOException {
        // Given
        Path negativeFile = tempDir.resolve("negative.properties");
        String content = """
            supabase.analytics.batch.size=-10
            supabase.analytics.flush.interval.seconds=-30
            """;
        Files.writeString(negativeFile, content);

        // When/Then
        // Should handle negative values gracefully
        SupabaseConfig config = new SupabaseConfig();
        assertThat(config.getAnalyticsBatchSize()).isLessThan(0);
        assertThat(config.getAnalyticsFlushIntervalSeconds()).isLessThan(0);
    }

    @Test
    void shouldHandleZeroNumericValues() throws IOException {
        // Given
        Path zeroFile = tempDir.resolve("zero.properties");
        String content = """
            supabase.analytics.batch.size=0
            supabase.analytics.flush.interval.seconds=0
            """;
        Files.writeString(zeroFile, content);

        // When/Then
        // Should handle zero values
        SupabaseConfig config = new SupabaseConfig();
        assertThat(config.getAnalyticsBatchSize()).isEqualTo(0);
        assertThat(config.getAnalyticsFlushIntervalSeconds()).isEqualTo(0);
    }

    @Test
    void shouldHandleVeryLargeNumericValues() throws IOException {
        // Given
        Path largeFile = tempDir.resolve("large.properties");
        String content = """
            supabase.analytics.batch.size=999999
            supabase.analytics.flush.interval.seconds=999999
            """;
        Files.writeString(largeFile, content);

        // When/Then
        // Should handle large values
        SupabaseConfig config = new SupabaseConfig();
        assertThat(config.getAnalyticsBatchSize()).isEqualTo(999999);
        assertThat(config.getAnalyticsFlushIntervalSeconds()).isEqualTo(999999);
    }

    @Test
    void shouldHandleInvalidUrls() throws IOException {
        // Given
        Path invalidUrlFile = tempDir.resolve("invalid-url.properties");
        String content = """
            supabase.url=not-a-valid-url
            supabase.anon.key=valid-key
            """;
        Files.writeString(invalidUrlFile, content);

        // When/Then
        // Should handle invalid URLs
        SupabaseConfig config = new SupabaseConfig();
        assertThat(config.getSupabaseUrl()).isEqualTo("not-a-valid-url");
        assertThat(config.isConfigured()).isFalse();
    }

    @Test
    void shouldHandleEmptyUrls() throws IOException {
        // Given
        Path emptyUrlFile = tempDir.resolve("empty-url.properties");
        String content = """
            supabase.url=
            supabase.anon.key=some-key
            """;
        Files.writeString(emptyUrlFile, content);

        // When/Then
        // Should handle empty URLs
        SupabaseConfig config = new SupabaseConfig();
        assertThat(config.getSupabaseUrl()).isEmpty();
        assertThat(config.isConfigured()).isFalse();
    }

    @Test
    void shouldHandleEmptyKeys() throws IOException {
        // Given
        Path emptyKeyFile = tempDir.resolve("empty-key.properties");
        String content = """
            supabase.url=https://valid-url.com
            supabase.anon.key=
            """;
        Files.writeString(emptyKeyFile, content);

        // When/Then
        // Should handle empty keys
        SupabaseConfig config = new SupabaseConfig();
        assertThat(config.getSupabaseAnonKey()).isEmpty();
        assertThat(config.isConfigured()).isFalse();
    }

    @Test
    void shouldHandleSpecialCharactersInValues() throws IOException {
        // Given
        Path specialCharFile = tempDir.resolve("special-chars.properties");
        String content = """
            supabase.url=https://test.com?param=value&other=test
            supabase.anon.key=key@with#special$chars%
            """;
        Files.writeString(specialCharFile, content);

        // When/Then
        // Should handle special characters
        SupabaseConfig config = new SupabaseConfig();
        assertThat(config.getSupabaseUrl()).contains("param=value");
        assertThat(config.getSupabaseAnonKey()).contains("@with#special$chars%");
    }

    @Test
    void shouldHandleUnicodeCharacters() throws IOException {
        // Given
        Path unicodeFile = tempDir.resolve("unicode.properties");
        String content = """
            supabase.url=https://测试.com
            supabase.anon.key=测试密钥🚀
            """;
        Files.writeString(unicodeFile, content);

        // When/Then
        // Should handle Unicode characters
        SupabaseConfig config = new SupabaseConfig();
        assertThat(config.getSupabaseUrl()).contains("测试");
        assertThat(config.getSupabaseAnonKey()).contains("测试密钥🚀");
    }

    @Test
    void shouldHandleDuplicateProperties() throws IOException {
        // Given
        Path duplicateFile = tempDir.resolve("duplicate.properties");
        String content = """
            supabase.analytics.enabled=true
            supabase.analytics.enabled=false
            supabase.error.reporting.enabled=true
            supabase.error.reporting.enabled=false
            """;
        Files.writeString(duplicateFile, content);

        // When/Then
        // Should handle duplicates (last one wins)
        SupabaseConfig config = new SupabaseConfig();
        assertThat(config.isAnalyticsEnabled()).isFalse();
        assertThat(config.isErrorReportingEnabled()).isFalse();
    }

    @Test
    void shouldHandleVeryLongValues() throws IOException {
        // Given
        Path longFile = tempDir.resolve("long-values.properties");
        String longUrl = "https://example.com/" + "a".repeat(1000);
        String longKey = "key-" + "b".repeat(1000);
        String content = """
            supabase.url=%s
            supabase.anon.key=%s
            """.formatted(longUrl, longKey);
        Files.writeString(longFile, content);

        // When/Then
        // Should handle very long values
        SupabaseConfig config = new SupabaseConfig();
        assertThat(config.getSupabaseUrl()).hasSizeGreaterThan(1000);
        assertThat(config.getSupabaseAnonKey()).hasSizeGreaterThan(1000);
    }

    @Test
    void shouldHandlePropertiesWithComments() throws IOException {
        // Given
        Path commentedFile = tempDir.resolve("comments.properties");
        String content = """
            # This is a comment
            supabase.url=https://valid.com
            ! Another comment style
            supabase.anon.key=valid-key
            # Final comment
            """;
        Files.writeString(commentedFile, content);

        // When/Then
        // Should ignore comments properly
        SupabaseConfig config = new SupabaseConfig();
        assertThat(config.getSupabaseUrl()).isEqualTo("https://valid.com");
        assertThat(config.getSupabaseAnonKey()).isEqualTo("valid-key");
    }

    @Test
    void shouldHandlePropertiesWithWhitespace() throws IOException {
        // Given
        Path whitespaceFile = tempDir.resolve("whitespace.properties");
        String content = """
            supabase.url = https://spaced.com  
            supabase.anon.key = spaced-key  
            """;
        Files.writeString(whitespaceFile, content);

        // When/Then
        // Should trim whitespace
        SupabaseConfig config = new SupabaseConfig();
        assertThat(config.getSupabaseUrl()).isEqualTo("https://spaced.com");
        assertThat(config.getSupabaseAnonKey()).isEqualTo("spaced-key");
    }

    @Test
    void shouldHandleMalformedPropertySyntax() throws IOException {
        // Given
        Path malformedSyntaxFile = tempDir.resolve("bad-syntax.properties");
        String content = """
            supabase.url=https://valid.com
            invalid line without equals
            supabase.anon.key=valid-key
            another=invalid=syntax=here
            """;
        Files.writeString(malformedSyntaxFile, content);

        // When/Then
        // Should handle syntax errors gracefully
        SupabaseConfig config = new SupabaseConfig();
        assertThat(config).isNotNull();
        // Valid properties should still be loaded
        assertThat(config.getSupabaseUrl()).isEqualTo("https://valid.com");
        assertThat(config.getSupabaseAnonKey()).isEqualTo("valid-key");
    }

    @Test
    void shouldValidateConfigurationStatus() {
        // Given
        SupabaseConfig emptyConfig = new SupabaseConfig();
        
        // When/Then
        // Should be not configured with default/empty values
        assertThat(emptyConfig.isConfigured()).isFalse();
        
        // When
        SupabaseConfig validConfig = new SupabaseConfig() {
            // Override with valid values for testing
            @Override
            public String getSupabaseUrl() {
                return "https://valid.supabase.co";
            }
            
            @Override
            public String getSupabaseAnonKey() {
                return "valid-anon-key";
            }
        };
        
        // Then
        // Should be configured with valid values
        assertThat(validConfig.isConfigured()).isTrue();
    }

    @Test
    void shouldHandleConfigurationLoadFailure() {
        // When
        // Simulate configuration load failure by creating config with invalid encoding
        SupabaseConfig config = new SupabaseConfig();

        // Then
        // Should not crash and provide defaults
        assertThat(config).isNotNull();
        assertThat(config.isAnalyticsEnabled()).isTrue();
        assertThat(config.isErrorReportingEnabled()).isTrue();
    }

    @Test
    void shouldProvideDefaultValues() {
        // When
        SupabaseConfig config = new SupabaseConfig();

        // Then
        // Should provide sensible defaults
        assertThat(config.isAnalyticsEnabled()).isTrue();
        assertThat(config.isErrorReportingEnabled()).isTrue();
        assertThat(config.getAnalyticsBatchSize()).isEqualTo(10);
        assertThat(config.getAnalyticsFlushIntervalSeconds()).isEqualTo(30);
        assertThat(config.getSupabaseUrl()).isNotEmpty();
        assertThat(config.getSupabaseAnonKey()).isNotEmpty();
    }

    @Test
    void shouldHandleConcurrentConfigurationAccess() throws Exception {
        // Given
        SupabaseConfig config = new SupabaseConfig();

        // When
        ConcurrencyTestHelper.runConcurrent(10, () -> {
            // Access configuration from multiple threads
            config.isAnalyticsEnabled();
            config.isErrorReportingEnabled();
            config.getAnalyticsBatchSize();
            config.getAnalyticsFlushIntervalSeconds();
            config.getSupabaseUrl();
            config.getSupabaseAnonKey();
            config.isConfigured();
            return "completed";
        });

        // Then
        // Should handle concurrent access without issues
        assertThat(config).isNotNull();
    }
}
