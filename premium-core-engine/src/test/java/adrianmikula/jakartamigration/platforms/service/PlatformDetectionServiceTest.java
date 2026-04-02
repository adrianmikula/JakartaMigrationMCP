package adrianmikula.jakartamigration.platforms.service;

import adrianmikula.jakartamigration.platforms.config.PlatformConfigLoader;
import adrianmikula.jakartamigration.platforms.model.DetectionPattern;
import adrianmikula.jakartamigration.platforms.model.JakartaCompatibility;
import adrianmikula.jakartamigration.platforms.model.PlatformConfig;
import adrianmikula.jakartamigration.platforms.model.PlatformDetection;
import adrianmikula.jakartamigration.platforms.model.PlatformScanResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for PlatformDetectionService
 */
public class PlatformDetectionServiceTest {
    
    private PlatformDetectionService detectionService;
    private Path tempDir;
    
    @BeforeEach
    void setUp() {
        detectionService = new PlatformDetectionService();
    }
    
    @Test
    void testScanProject_NoPlatformsFound_ReturnsEmptyResult() {
        // Given
        Path emptyProjectPath = Path.of("/empty/project");
        
        // When
        PlatformScanResult result = detectionService.scanProject(emptyProjectPath);
        
        // Then
        assertThat(result.detectedPlatforms()).isEmpty();
        assertThat(result.recommendations()).contains("No application servers detected in the project");
    }
    
    @Test
    void testDetectPlatform_TomcatFound_ReturnsDetection() {
        // Given
        Path projectPath = createMockProjectWithTomcat();
        adrianmikula.jakartamigration.platforms.model.PlatformConfig tomcatConfig = createTomcatConfig();
        
        // When
        adrianmikula.jakartamigration.platforms.model.PlatformDetection detection = detectionService.detectPlatform(projectPath, tomcatConfig);
        
        // Then
        assertThat(detection).isNotNull();
        assertThat(detection.platformType()).isEqualTo("tomcat");
        assertThat(detection.platformName()).isEqualTo("Apache Tomcat");
        assertThat(detection.detectedVersion()).isEqualTo("10.1.5");
        assertThat(detection.isJakartaCompatible()).isTrue();
    }
    
    @Test
    void testDetectPlatform_WildFlyFound_ReturnsDetection() {
        // Given
        Path projectPath = createMockProjectWithWildFly();
        PlatformConfig wildflyConfig = createWildFlyConfig();
        
        // When
        PlatformDetection detection = detectionService.detectPlatform(projectPath, wildflyConfig);
        
        // Then
        assertThat(detection).isNotNull();
        assertThat(detection.platformType()).isEqualTo("wildfly");
        assertThat(detection.platformName()).isEqualTo("WildFly");
        assertThat(detection.detectedVersion()).isEqualTo("27.1.0");
        assertThat(detection.isJakartaCompatible()).isTrue();
    }
    
    @Test
    void testDetectPlatform_NoMatchingFiles_ReturnsNull() {
        // Given
        Path projectPath = createEmptyProject();
        PlatformConfig tomcatConfig = createTomcatConfig();
        
        // When
        PlatformDetection detection = detectionService.detectPlatform(projectPath, tomcatConfig);
        
        // Then
        assertThat(detection).isNull();
    }
    
    @Test
    void testCalculateRiskScore_NoPlatforms_ReturnsZero() {
        // Given
        List<PlatformDetection> emptyDetections = List.of();
        
        // When
        int riskScore = detectionService.calculateRiskScore(emptyDetections);
        
        // Then
        assertThat(riskScore).isEqualTo(0);
    }
    
    @Test
    void testCalculateRiskScore_NonJakartaCompatible_ReturnsHighScore() {
        // Given
        PlatformDetection incompatibleDetection = new PlatformDetection(
            "tomcat",
            "Apache Tomcat",
            "9.0.0",
            false,
            "10.0",
            Map.of("java", "11+", "jakarta", "9+")
        );
        
        // When
        int riskScore = detectionService.calculateRiskScore(List.of(incompatibleDetection));
        
        // Then
        assertThat(riskScore).isGreaterThan(100); // Major version change penalty
    }
    
    @Test
    void testCalculateRiskScore_JakartaCompatible_ReturnsLowScore() {
        // Given
        PlatformDetection compatibleDetection = new PlatformDetection(
            "tomcat",
            "Apache Tomcat",
            "10.1.5",
            true,
            "10.0",
            Map.of("java", "11+", "jakarta", "9+")
        );
        
        // When
        int riskScore = detectionService.calculateRiskScore(List.of(compatibleDetection));
        
        // Then
        assertThat(riskScore).isEqualTo(25); // Only runtime change base score
    }
    
    @Test
    void testIsVersionGreaterOrEqual_SameVersion_ReturnsTrue() {
        // Given
        String version1 = "10.1.5";
        String version2 = "10.1.5";
        
        // When
        boolean result = callPrivateMethod(detectionService, "isVersionGreaterOrEqual", 
            new Class<?>[]{String.class, String.class}, version1, version2);
        
        // Then
        assertThat(result).isTrue();
    }
    
    @Test
    void testIsVersionGreaterOrEqual_GreaterVersion_ReturnsTrue() {
        // Given
        String version1 = "10.2.0";
        String version2 = "10.1.5";
        
        // When
        boolean result = callPrivateMethod(detectionService, "isVersionGreaterOrEqual", 
            new Class<?>[]{String.class, String.class}, version1, version2);
        
        // Then
        assertThat(result).isTrue();
    }
    
    @Test
    void testIsVersionGreaterOrEqual_LowerVersion_ReturnsFalse() {
        // Given
        String version1 = "10.0.0";
        String version2 = "10.1.5";
        
        // When
        boolean result = callPrivateMethod(detectionService, "isVersionGreaterOrEqual", 
            new Class<?>[]{String.class, String.class}, version1, version2);
        
        // Then
        assertThat(result).isFalse();
    }
    
    // Helper methods
    private Path createMockProjectWithTomcat() {
        try {
            Path projectPath = Files.createTempDirectory("mock-tomcat-project");
            Path binDir = projectPath.resolve("bin");
            Files.createDirectories(binDir);
            
            // Create mock catalina.bat with version
            String catalinaContent = "Apache Tomcat/10.1.5.0";
            Path catalinaPath = binDir.resolve("catalina.bat");
            Files.writeString(catalinaPath, catalinaContent);
            
            return projectPath;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create mock project", e);
        }
    }
    
    private Path createMockProjectWithWildFly() {
        try {
            Path projectPath = Files.createTempDirectory("mock-wildfly-project");
            Path binDir = projectPath.resolve("bin");
            Files.createDirectories(binDir);
            
            // Create mock standalone.sh with version
            String standaloneContent = "WildFly Full Platform 27.1.0.Final";
            Path standalonePath = binDir.resolve("standalone.sh");
            Files.writeString(standalonePath, standaloneContent);
            
            return projectPath;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create mock project", e);
        }
    }
    
    private Path createEmptyProject() {
        try {
            return Files.createTempDirectory("empty-project");
        } catch (Exception e) {
            throw new RuntimeException("Failed to create empty project", e);
        }
    }
    
    private PlatformConfig createTomcatConfig() {
        return new PlatformConfig(
            "Apache Tomcat",
            "Apache Tomcat application server",
            List.of(
                new DetectionPattern("bin/catalina.bat", "Apache Tomcat/([0-9]+\\.[0-9]+\\.[0-9]+)", 1)
            ),
            new JakartaCompatibility("10.0", List.of("10.0", "10.1", "11.0", "11.1")),
            List.of("8.5", "9.0"),
            Map.of("java", "11+", "jakarta", "9+", "spring", "5+")
        );
    }
    
    private PlatformConfig createWildFlyConfig() {
        return new PlatformConfig(
            "WildFly",
            "Red Hat WildFly application server",
            List.of(
                new DetectionPattern("bin/standalone.sh", "WildFly Full Platform ([0-9]+\\.[0-9]+\\.[0-9]+)", 1)
            ),
            new JakartaCompatibility("27.0", List.of("27.0", "27.1", "28.0", "29.0")),
            List.of("20.0", "21.0", "23.0"),
            Map.of("java", "17+", "jakarta", "9+", "spring", "6+")
        );
    }
    
    @SuppressWarnings("unchecked")
    private <T> T callPrivateMethod(Object target, String methodName, Class<?>[] paramTypes, Object... paramValues) {
        try {
            java.lang.reflect.Method method = target.getClass().getDeclaredMethod(methodName, paramTypes);
            method.setAccessible(true);
            return (T) method.invoke(target, paramValues);
        } catch (Exception e) {
            throw new RuntimeException("Failed to call private method: " + methodName, e);
        }
    }
}
