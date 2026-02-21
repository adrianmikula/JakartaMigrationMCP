package adrianmikula.jakartamigration.advancedscanning.service.impl;

import adrianmikula.jakartamigration.advancedscanning.domain.TestContainerUsage;
import adrianmikula.jakartamigration.advancedscanning.domain.TestContainersProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.service.TestContainersScanner;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class TestContainersScannerImpl implements TestContainersScanner {

    private static final Map<String, String> CONTAINER_PATTERNS = new HashMap<>();
    static {
        CONTAINER_PATTERNS.put("org.eclipse.jetty", "Jetty");
        CONTAINER_PATTERNS.put("org.apache.tomcat", "Tomcat");
        CONTAINER_PATTERNS.put("org.wildfly", "WildFly");
        CONTAINER_PATTERNS.put("org.jboss.as", "JBoss AS");
        CONTAINER_PATTERNS.put("org.glassfish", "GlassFish");
        CONTAINER_PATTERNS.put("com.google.appengine", "Google App Engine");
        CONTAINER_PATTERNS.put("org.testcontainers", "Testcontainers");
    }

    private static final Pattern MAVEN_DEP = Pattern.compile(
        "<dependency>\\s*<groupId>([^<]+)</groupId>\\s*<artifactId>([^<]+)</artifactId>\\s*<version>([^<]*)</version>"
    );
    private static final Pattern GRADLE_DEP = Pattern.compile(
        "(testImplementation|testCompile|implementation|compile)\\s*['\"]([^:]+):([^:]+):([^'\"]+)['\"]"
    );

    @Override
    public TestContainersProjectScanResult scanProject(Path projectPath) {
        log.info("Starting test containers scan for project: {}", projectPath);
        List<TestContainerUsage> usages = new ArrayList<>();
        
        try {
            Path pomPath = projectPath.resolve("pom.xml");
            if (Files.exists(pomPath)) {
                usages.addAll(scanMaven(pomPath));
            }
            
            Path gradlePath = projectPath.resolve("build.gradle");
            if (Files.exists(gradlePath)) {
                usages.addAll(scanGradle(gradlePath));
            }
        } catch (Exception e) {
            log.error("Error scanning test containers: {}", e.getMessage());
        }
        
        return new TestContainersProjectScanResult(projectPath.toString(), usages);
    }

    private List<TestContainerUsage> scanMaven(Path pomPath) {
        List<TestContainerUsage> usages = new ArrayList<>();
        try {
            String content = Files.readString(pomPath);
            Matcher matcher = MAVEN_DEP.matcher(content);
            while (matcher.find()) {
                String groupId = matcher.group(1);
                String artifactId = matcher.group(2);
                String version = matcher.group(3);
                String key = groupId + ":" + artifactId;
                
                for (Map.Entry<String, String> entry : CONTAINER_PATTERNS.entrySet()) {
                    if (key.contains(entry.getKey())) {
                        usages.add(new TestContainerUsage(
                            pomPath.toString(),
                            entry.getValue(),
                            version,
                            "javax-only"
                        ));
                        break;
                    }
                }
            }
        } catch (IOException e) {
            log.warn("Error reading pom.xml: {}", e.getMessage());
        }
        return usages;
    }

    private List<TestContainerUsage> scanGradle(Path gradlePath) {
        List<TestContainerUsage> usages = new ArrayList<>();
        try {
            String content = Files.readString(gradlePath);
            Matcher matcher = GRADLE_DEP.matcher(content);
            while (matcher.find()) {
                String dep = matcher.group(2) + ":" + matcher.group(3);
                String version = matcher.group(4);
                
                for (Map.Entry<String, String> entry : CONTAINER_PATTERNS.entrySet()) {
                    if (dep.contains(entry.getKey())) {
                        usages.add(new TestContainerUsage(
                            gradlePath.toString(),
                            entry.getValue(),
                            version,
                            "javax-only"
                        ));
                        break;
                    }
                }
            }
        } catch (IOException e) {
            log.warn("Error reading build.gradle: {}", e.getMessage());
        }
        return usages;
    }
}
