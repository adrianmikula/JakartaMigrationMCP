package adrianmikula.jakartamigration.advancedscanning.service.impl;

import adrianmikula.jakartamigration.advancedscanning.domain.AppServerProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.AppServerUsage;
import adrianmikula.jakartamigration.advancedscanning.service.AppServerScanner;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class AppServerScannerImpl implements AppServerScanner {

    private static final Map<String, String> SERVER_PATTERNS = new HashMap<>();
    static {
        SERVER_PATTERNS.put("weblogic", "WebLogic");
        SERVER_PATTERNS.put("websphere", "WebSphere");
        SERVER_PATTERNS.put("jboss", "JBoss");
        SERVER_PATTERNS.put("wildfly", "WildFly");
        SERVER_PATTERNS.put("glassfish", "GlassFish");
        SERVER_PATTERNS.put("tibco", "TIBCO");
        SERVER_PATTERNS.put("oracle.weblogic", "WebLogic");
        SERVER_PATTERNS.put("com.ibm.websphere", "WebSphere");
    }

    private static final Pattern MAVEN_DEP = Pattern.compile(
        "<dependency>\\s*<groupId>([^<]+)</groupId>\\s*<artifactId>([^<]+)</artifactId>"
    );
    private static final Pattern GRADLE_DEP = Pattern.compile(
        "(implementation|compile)\\s*['\"]([^:]+):([^:]+)"
    );
    private static final Pattern DESCRIPTOR_PATTERN = Pattern.compile(
        "(weblogic\\.xml|websphere\\.xml|jboss\\.xml|glassfish\\.xml|META-INF/application\\.xml)"
    );

    @Override
    public AppServerProjectScanResult scanProject(Path projectPath) {
        log.info("Starting app server scan for project: {}", projectPath);
        List<AppServerUsage> usages = new ArrayList<>();
        
        try {
            // Scan for server-specific dependencies
            Path pomPath = projectPath.resolve("pom.xml");
            if (Files.exists(pomPath)) {
                usages.addAll(scanMaven(pomPath));
            }
            
            Path gradlePath = projectPath.resolve("build.gradle");
            if (Files.exists(gradlePath)) {
                usages.addAll(scanGradle(gradlePath));
            }
            
            // Scan for server-specific descriptors
            usages.addAll(scanDescriptors(projectPath));
            
        } catch (Exception e) {
            log.error("Error scanning app server: {}", e.getMessage());
        }
        
        return new AppServerProjectScanResult(projectPath.toString(), usages);
    }

    private List<AppServerUsage> scanMaven(Path pomPath) {
        List<AppServerUsage> usages = new ArrayList<>();
        try {
            String content = Files.readString(pomPath);
            Matcher matcher = MAVEN_DEP.matcher(content);
            while (matcher.find()) {
                String dep = matcher.group(1) + ":" + matcher.group(2);
                for (Map.Entry<String, String> entry : SERVER_PATTERNS.entrySet()) {
                    if (dep.toLowerCase().contains(entry.getKey())) {
                        usages.add(new AppServerUsage(
                            pomPath.toString(),
                            entry.getValue(),
                            "Maven Dependency",
                            dep
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

    private List<AppServerUsage> scanGradle(Path gradlePath) {
        List<AppServerUsage> usages = new ArrayList<>();
        try {
            String content = Files.readString(gradlePath);
            Matcher matcher = GRADLE_DEP.matcher(content);
            while (matcher.find()) {
                String dep = matcher.group(2) + ":" + matcher.group(3);
                for (Map.Entry<String, String> entry : SERVER_PATTERNS.entrySet()) {
                    if (dep.toLowerCase().contains(entry.getKey())) {
                        usages.add(new AppServerUsage(
                            gradlePath.toString(),
                            entry.getValue(),
                            "Gradle Dependency",
                            dep
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

    private List<AppServerUsage> scanDescriptors(Path projectPath) {
        List<AppServerUsage> usages = new ArrayList<>();
        try (var paths = Files.walk(projectPath)) {
            paths.filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".xml"))
                .forEach(path -> {
                    try {
                        String content = Files.readString(path);
                        Matcher matcher = DESCRIPTOR_PATTERN.matcher(content);
                        if (matcher.find()) {
                            String desc = matcher.group(1);
                            String serverType = desc.replace(".xml", "").replace("M-INF/", "");
                            usages.add(new AppServerUsage(
                                path.toString(),
                                serverType + " descriptor",
                                "Server Descriptor",
                                desc
                            ));
                        }
                    } catch (IOException e) {
                        log.warn("Error reading descriptor: {}", e.getMessage());
                    }
                });
        } catch (IOException e) {
            log.error("Error scanning descriptors: {}", e.getMessage());
        }
        return usages;
    }
}
