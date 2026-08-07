package adrianmikula.jakartamigration.mcp;

import adrianmikula.jakartamigration.experiment.mcp.ExperimentTools;
import adrianmikula.jakartamigration.experiment.service.DockerOrchestratorFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

@Configuration
public class PremiumExperimentConfig {

    @Bean
    public ExperimentTools experimentTools(
            @Value("${app.experiment.project-root:${user.dir}}") String projectRoot,
            @Value("${app.experiment.docker-image:eclipse-temurin:17-jdk}") String dockerImage,
            @Value("${app.experiment.test-timeout-seconds:300}") int testTimeoutSeconds) {
        return new ExperimentTools(
            Path.of(projectRoot),
            new DockerOrchestratorFactory(),
            dockerImage,
            testTimeoutSeconds
        );
    }
}
