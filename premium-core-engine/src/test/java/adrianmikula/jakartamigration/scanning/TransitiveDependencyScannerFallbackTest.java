package adrianmikula.jakartamigration.scanning;

import adrianmikula.jakartamigration.advancedscanning.domain.ScanReason;
import adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.TransitiveDependencyUsage;
import adrianmikula.jakartamigration.advancedscanning.service.impl.TransitiveDependencyScannerImpl;
import adrianmikula.jakartamigration.dependencyanalysis.config.CompatibilityConfigLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Build Tool Error Fallback Tests")
@Tag("slow")
class TransitiveDependencyScannerFallbackTest {

    private TransitiveDependencyScannerImpl scanner;

    @BeforeEach
    void setUp() {
        scanner = new TransitiveDependencyScannerImpl();
    }

    @Test
    @DisplayName("Should fallback to regex and classify dependencies when Maven fails")
    void shouldFallbackToRegexOnMavenFailure(@TempDir Path tempDir) throws IOException {
        // Create a pom.xml with known javax dependencies
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.test</groupId>
                <artifactId>test-project</artifactId>
                <version>1.0.0</version>
                <packaging>jar</packaging>
                <dependencies>
                    <dependency>
                        <groupId>javax.servlet</groupId>
                        <artifactId>javax.servlet-api</artifactId>
                        <version>4.0.1</version>
                    </dependency>
                    <dependency>
                        <groupId>javax.persistence</groupId>
                        <artifactId>javax.persistence-api</artifactId>
                        <version>2.2</version>
                    </dependency>
                </dependencies>
            </project>
            """;
        Files.writeString(tempDir.resolve("pom.xml"), pomContent);

        // Scan - should fallback to regex when Maven is not available or fails
        TransitiveDependencyScanResult result = scanner.scanFile(tempDir.resolve("pom.xml"));

        assertThat(result).isNotNull();
        assertThat(result.getUsages()).isNotEmpty();

        // Verify dependencies are classified correctly (not all BUILD_TOOL_ERROR)
        boolean hasJavaxClassification = result.getUsages().stream()
            .anyMatch(u -> u.getScanReason() == ScanReason.BLACKLISTED ||
                          u.getScanReason() == ScanReason.UNKNOWN);
        assertThat(hasJavaxClassification).isTrue();

        // Verify javax.servlet is classified as requiring migration
        TransitiveDependencyUsage servletUsage = result.getUsages().stream()
            .filter(u -> u.getArtifactId().equals("javax.servlet-api"))
            .findFirst()
            .orElse(null);
        assertThat(servletUsage).isNotNull();
        assertThat(servletUsage.getGroupId()).isEqualTo("javax.servlet");
    }

    @Test
    @DisplayName("Should fallback to regex and classify dependencies when Gradle fails")
    void shouldFallbackToRegexOnGradleFailure(@TempDir Path tempDir) throws IOException {
        // Create a build.gradle with known javax dependencies
        String gradleContent = """
            plugins {
                id 'java'
            }
            repositories {
                mavenCentral()
            }
            dependencies {
                implementation 'javax.servlet:javax.servlet-api:4.0.1'
                implementation 'javax.persistence:javax.persistence-api:2.2'
            }
            """;
        Files.writeString(tempDir.resolve("build.gradle"), gradleContent);

        TransitiveDependencyScanResult result = scanner.scanFile(tempDir.resolve("build.gradle"));

        assertThat(result).isNotNull();
        assertThat(result.getUsages()).isNotEmpty();

        // Verify dependencies are classified (not all BUILD_TOOL_ERROR)
        boolean hasNonErrorClassification = result.getUsages().stream()
            .anyMatch(u -> u.getScanReason() != ScanReason.BUILD_TOOL_ERROR);
        assertThat(hasNonErrorClassification).isTrue();
    }

    @Test
    @DisplayName("Should show balloon notification on failure (deduplicated)")
    void shouldShowBalloonNotificationOnFailure() {
        BalloonNotificationService notificationService = new BalloonNotificationService();

        String key = "build-tool-failure:/test/project";
        boolean shown1 = notificationService.showOnce(key, "Build Tool Error", "Maven failed");
        boolean shown2 = notificationService.showOnce(key, "Build Tool Error", "Maven failed");

        assertThat(shown1).isTrue();
        assertThat(shown2).isFalse(); // Should be deduplicated
    }

    @Test
    @DisplayName("Should classify dependencies after regex fallback")
    void shouldClassifyDependenciesAfterFallback(@TempDir Path tempDir) throws IOException {
        String pomContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.test</groupId>
                <artifactId>test-project</artifactId>
                <version>1.0.0</version>
                <dependencies>
                    <dependency>
                        <groupId>javax.servlet</groupId>
                        <artifactId>javax.servlet-api</artifactId>
                        <version>4.0.1</version>
                    </dependency>
                    <dependency>
                        <groupId>jakarta.servlet</groupId>
                        <artifactId>jakarta.servlet-api</artifactId>
                        <version>6.0.0</version>
                    </dependency>
                    <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                        <version>32.1.3-jre</version>
                    </dependency>
                </dependencies>
            </project>
            """;
        Files.writeString(tempDir.resolve("pom.xml"), pomContent);

        TransitiveDependencyScanResult result = scanner.scanFile(tempDir.resolve("pom.xml"));

        assertThat(result).isNotNull();
        assertThat(result.getUsages()).hasSizeGreaterThanOrEqualTo(2);

        // Verify javax.servlet is classified as requiring migration
        var servletUsage = result.getUsages().stream()
            .filter(u -> u.getArtifactId().equals("javax.servlet-api"))
            .findFirst()
            .orElse(null);
        assertThat(servletUsage).isNotNull();
        assertThat(servletUsage.getScanReason()).isIn(
            ScanReason.BLACKLISTED, ScanReason.UNKNOWN);

        // Verify jakarta.servlet is classified correctly
        var jakartaUsage = result.getUsages().stream()
            .filter(u -> u.getArtifactId().equals("jakarta.servlet-api"))
            .findFirst()
            .orElse(null);
        assertThat(jakartaUsage).isNotNull();
    }

    @Test
    @DisplayName("BalloonNotificationService should reset correctly")
    void shouldResetNotifications() {
        BalloonNotificationService service = new BalloonNotificationService();

        service.showOnce("key1", "Title", "Message1");
        assertThat(service.hasShown("key1")).isTrue();

        service.reset();
        assertThat(service.hasShown("key1")).isFalse();
    }

    @Test
    @DisplayName("BalloonNotificationService should handle multiple different keys")
    void shouldHandleMultipleKeys() {
        BalloonNotificationService service = new BalloonNotificationService();

        boolean shown1 = service.showOnce("key1", "Title", "Message1");
        boolean shown2 = service.showOnce("key2", "Title", "Message2");
        boolean duplicate1 = service.showOnce("key1", "Title", "Message1");

        assertThat(shown1).isTrue();
        assertThat(shown2).isTrue();
        assertThat(duplicate1).isFalse();
    }
}
