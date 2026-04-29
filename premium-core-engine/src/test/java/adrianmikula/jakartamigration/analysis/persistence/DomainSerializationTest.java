package adrianmikula.jakartamigration.analysis.persistence;

import adrianmikula.jakartamigration.advancedscanning.domain.*;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;

@Tag("slow")
public class DomainSerializationTest {

    private final ObjectMapperService objectMapperService = new ObjectMapperService();

    @Test
    public void testSecurityApiProjectScanResultSerialization() {
        SecurityApiUsage usage = new SecurityApiUsage("javax.security.auth.Subject", "Subject",
                "jakarta.security.auth.Subject", 10, "test context");
        SecurityApiScanResult fileResult = new SecurityApiScanResult(Path.of("test.java"), List.of(usage), 100);
        SecurityApiProjectScanResult result = new SecurityApiProjectScanResult(List.of(fileResult), 1, 1, 1);

        String json = objectMapperService.toJson(result);
        SecurityApiProjectScanResult deserialized = objectMapperService.fromJson(json,
                SecurityApiProjectScanResult.class);

        assertThat(deserialized.getFileResults()).hasSize(1);
        assertThat(deserialized.getFileResults().get(0).getFilePath().toString()).contains("test.java");
        assertThat(deserialized.getFileResults().get(0).getUsages().get(0).getJavaxClass())
                .isEqualTo("javax.security.auth.Subject");
    }

    @Test
    public void testJpaProjectScanResultSerialization() {
        JpaScanResult fileResult = new JpaScanResult(Path.of("test.java"), List.of(), 0);
        JpaProjectScanResult result = new JpaProjectScanResult(List.of(fileResult), 1, 0, 0);

        String json = objectMapperService.toJson(result);
        JpaProjectScanResult deserialized = objectMapperService.fromJson(json, JpaProjectScanResult.class);

        assertThat(deserialized.fileResults()).hasSize(1);
        assertThat(deserialized.totalFilesScanned()).isEqualTo(1);
    }

    @Test
    public void testLoggingMetricsProjectScanResultSerialization() {
        LoggingMetricsUsage usage = new LoggingMetricsUsage("test.java", 5, "javax.logging.Logger",
                "org.apache.log4j.Logger", "getLogger");
        LoggingMetricsScanResult fileResult = new LoggingMetricsScanResult("test.java", List.of(usage));
        LoggingMetricsProjectScanResult result = new LoggingMetricsProjectScanResult("path", List.of(fileResult));

        String json = objectMapperService.toJson(result);
        LoggingMetricsProjectScanResult deserialized = objectMapperService.fromJson(json,
                LoggingMetricsProjectScanResult.class);

        assertThat(deserialized.getProjectPath()).isEqualTo("path");
        assertThat(deserialized.getFileResults()).hasSize(1);
        assertThat(deserialized.getTotalFindings()).isEqualTo(1);
    }
}
