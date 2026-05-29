package adrianmikula.jakartamigration.mcp.config;

import adrianmikula.jakartamigration.dependencyanalysis.service.DependencyAnalysisModule;
import adrianmikula.jakartamigration.dependencyanalysis.service.impl.PremiumDependencyAnalysisModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Premium MCP configuration that overrides community beans with
 * premium implementations using build-tool-based dependency resolution.
 */
@Configuration
public class PremiumMcpConfiguration {

    /**
     * Provides a {@link PremiumDependencyAnalysisModule} as the primary
     * {@link DependencyAnalysisModule}, replacing the community regex-based
     * implementation with one that uses actual Maven/Gradle commands for
     * accurate dependency resolution.
     *
     * <p>If build tools are unavailable, the premium builder automatically
     * falls back to the community regex parser.
     */
    @Bean
    @Primary
    public DependencyAnalysisModule premiumDependencyAnalysisModule() {
        return new PremiumDependencyAnalysisModule();
    }
}
