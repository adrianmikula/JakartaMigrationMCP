package adrianmikula.jakartamigration.analytics.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SupabaseConfig.
 */
class SupabaseConfigTest {

    @Test
    void shouldLoadDefaultConfiguration() {
        // When
        SupabaseConfig config = new SupabaseConfig();

        // Then
        assertThat(config).isNotNull();
        assertThat(config.isAnalyticsEnabled()).isTrue();
        assertThat(config.isErrorReportingEnabled()).isTrue();
        assertThat(config.getAnalyticsBatchSize()).isEqualTo(10);
        assertThat(config.getAnalyticsFlushIntervalSeconds()).isEqualTo(30);
    }

    @Test
    void shouldReturnConfigurationStatus() {
        // Given
        SupabaseConfig config = new SupabaseConfig();

        // When
        boolean configured = config.isConfigured();

        // Then
        // Should be false with default placeholder values
        assertThat(configured).isFalse();
    }

    @Test
    void shouldProvideConfigurationValues() {
        // Given
        SupabaseConfig config = new SupabaseConfig();

        // When & Then
        assertThat(config.getSupabaseUrl()).contains("your-project.supabase.co");
        assertThat(config.getSupabaseAnonKey()).contains("your-supabase-anon-key");
    }
}
