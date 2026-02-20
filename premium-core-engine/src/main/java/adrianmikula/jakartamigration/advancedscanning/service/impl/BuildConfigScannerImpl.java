package adrianmikula.jakartamigration.advancedscanning.service.impl;

import adrianmikula.jakartamigration.advancedscanning.domain.BuildConfigProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.BuildConfigScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.BuildConfigUsage;
import adrianmikula.jakartamigration.advancedscanning.service.BuildConfigScanner;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Slf4j
public class BuildConfigScannerImpl implements BuildConfigScanner {

    // Map of javax.* dependencies to Jakarta equivalents
    private static final Map<String, String[]> DEPENDENCY_MAPPINGS = new HashMap<>();

    static {
        // Servlet
        DEPENDENCY_MAPPINGS.put("javax.servlet:javax.servlet-api", 
            new String[]{"jakarta.servlet:jakarta.servlet-api", "4.0.4"});
        DEPENDENCY_MAPPINGS.put("javax.servlet:servlet-api",
            new String[]{"jakarta.servlet:jakarta.servlet-api", "4.0.4"});

        // JPA
        DEPENDENCY_MAPPINGS.put("javax.persistence:javax.persistence-api",
            new String[]{"jakarta.persistence:jakarta.persistence-api", "2.2.3"});
        DEPENDENCY_MAPPINGS.put("org.hibernate:hibernate-core",
            new String[]{"org.hibernate:hibernate-core", "5.6.15.Final"}); // Need migration to 6.x

        // Bean Validation
        DEPENDENCY_MAPPINGS.put("javax.validation:validation-api",
            new String[]{"jakarta.validation:jakarta.validation-api", "2.0.2"});
        DEPENDENCY_MAPPINGS.put("javax.validation:validation-api:1.1.0.Final",
            new String[]{"jakarta.validation:jakarta.validation-api", "2.0.2"});

        // CDI
        DEPENDENCY_MAPPINGS.put("javax.inject:javax.inject",
            new String[]{"jakarta.inject:jakarta.inject-api", "1.0.5"});
        DEPENDENCY_MAPPINGS.put("javax.enterprise:cdi-api",
            new String[]{"jakarta.enterprise:jakarta.cdi-api", "2.0.2"});

        // JAXB
        DEPENDENCY_MAPPINGS.put("javax.xml.bind:jaxb-api",
            new String[]{"jakarta.xml.bind:jakarta.xml.bind-api", "2.3.3"});

        // JAX-RS
        DEPENDENCY_MAPPINGS.put("javax.ws.rs:javax.ws.rs-api",
            new String[]{"jakarta.ws.rs:jakarta.ws.rs-api", "2.1.6"});

        // JMS
        DEPENDENCY_MAPPINGS.put("javax.jms:javax.jms-api",
            new String[]{"jakarta.jms:jakarta.jms-api", "2.0.3"});

        // Mail
        DEPENDENCY_MAPPINGS.put("javax.mail:mail",
            new String[]{"com.sun.mail: jakarta.mail", "1.6.7"});

        // JSON
        DEPENDENCY_MAPPINGS.put("javax.json:javax.json-api",
            new String[]{"jakarta.json:jakarta.json-api", "1.1.6"});

        // JSONP
        DEPENDENCY_MAPPINGS.put("javax.json.bind:jsonb-api",
            new String[]{"jakarta.json.bind:jakarta.jsonb-api", "1.0.2"});

        // Annotations
        DEPENDENCY_MAPPINGS.put("javax.annotation:javax.annotation-api",
            new String[]{"jakarta.annotation:jakarta.annotation-api", "1.3.5"});
    }

    @Override
    public BuildConfigProjectScanResult scanProject(Path projectPath) {
        if (projectPath == null || !Files.exists(projectPath) || !Files.isDirectory(projectPath)) {
            log.warn("Invalid project path: {}", projectPath);
            return BuildConfigProjectScanResult.empty();
        }

        try {
            List<Path> buildFiles = discoverBuildFiles(projectPath);

            if (buildFiles.isEmpty()) {
                log.info("No build files found in project: {}", projectPath);
                return BuildConfigProjectScanResult.empty();
            }

            log.info("Scanning {} build files in project: {}", buildFiles.size(), projectPath);

            AtomicInteger totalScanned = new AtomicInteger(0);
            List<BuildConfigScanResult> results = new ArrayList<>();

            buildFiles.parallelStream().forEach(file -> {
                totalScanned.incrementAndGet();
                BuildConfigScanResult result = scanFile(file);
                if (result.hasJavaxDependencies()) {
                    synchronized(results) {
                        results.add(result);
                    }
                }
            });

            int totalDeps = results.stream()
                .mapToInt(r -> r.usages().size())
                .sum();

            log.info("Build config scan complete: {} files scanned, {} files with javax deps, {} total deps",
                totalScanned.get(), results.size(), totalDeps);

            return new BuildConfigProjectScanResult(
                results,
                totalScanned.get(),
                results.size(),
                totalDeps
            );

        } catch (Exception e) {
            log.error("Error scanning project for build config: {}", projectPath, e);
            return BuildConfigProjectScanResult.empty();
        }
    }

    @Override
    public BuildConfigScanResult scanFile(Path filePath) {
        if (filePath == null || !Files.exists(filePath)) {
            return BuildConfigScanResult.empty(filePath);
        }

        try {
            String content = Files.readString(filePath);
            String fileName = filePath.getFileName().toString();
            String buildType = determineBuildType(fileName);

            List<BuildConfigUsage> usages = new ArrayList<>();

            if ("pom.xml".equals(buildType)) {
                usages = parsePomXml(content, filePath);
            } else if (buildType.startsWith("build.gradle")) {
                usages = parseGradle(content, filePath);
            }

            return new BuildConfigScanResult(filePath, usages, buildType);

        } catch (Exception e) {
            log.warn("Error scanning build file: {}", filePath, e);
            return BuildConfigScanResult.empty(filePath);
        }
    }

    private List<Path> discoverBuildFiles(Path projectPath) {
        List<Path> files = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(projectPath)) {
            paths
                .filter(Files::isRegularFile)
                .filter(p -> {
                    String name = p.getFileName().toString();
                    return "pom.xml".equals(name) || 
                           name.startsWith("build.gradle") ||
                           name.endsWith(".gradle");
                })
                .filter(p -> !p.toString().contains("/target/"))
                .filter(p -> !p.toString().contains("/build/"))
                .forEach(files::add);
        } catch (IOException e) {
            log.error("Error discovering build files", e);
        }

        return files;
    }

    private String determineBuildType(String fileName) {
        if (fileName.equals("pom.xml")) return "pom.xml";
        if (fileName.endsWith(".gradle.kts")) return "build.gradle.kts";
        if (fileName.startsWith("build.gradle")) return "build.gradle";
        return "unknown";
    }

    private List<BuildConfigUsage> parsePomXml(String content, Path filePath) {
        List<BuildConfigUsage> usages = new ArrayList<>();
        String[] lines = content.split("\n");

        // Match <groupId>javax.xxx</groupId> and <artifactId>xxx</artifactId>
        Pattern depPattern = Pattern.compile(
            "<dependency>.*?<groupId>([\\w.]+)</groupId>.*?<artifactId>([\\w.-]+)</artifactId>.*?(?:<version>([\\w.-]+)</version>)?.*?</dependency>",
            Pattern.DOTALL
        );
        Matcher matcher = depPattern.matcher(content);

        while (matcher.find()) {
            String groupId = matcher.group(1);
            String artifactId = matcher.group(2);
            String version = matcher.group(3);

            if (groupId.startsWith("javax.")) {
                String key = groupId + ":" + artifactId;
                String[] mapping = DEPENDENCY_MAPPINGS.get(key);

                int lineNumber = findLineNumber(lines, groupId);

                usages.add(new BuildConfigUsage(
                    groupId,
                    artifactId,
                    version,
                    mapping != null ? mapping[0].split(":")[0] : groupId.replace("javax", "jakarta"),
                    mapping != null ? mapping[0].split(":")[1] : artifactId,
                    mapping != null ? mapping[1] : null,
                    lineNumber
                ));
            }
        }

        return usages;
    }

    private List<BuildConfigUsage> parseGradle(String content, Path filePath) {
        List<BuildConfigUsage> usages = new ArrayList<>();
        String[] lines = content.split("\n");

        // Match 'groupId:artifactId:version' or "groupId:artifactId:version"
        Pattern depPattern = Pattern.compile("['\"]([\\w.]+):([\\w.-]+):([\\w.-]+)['\"]");
        Matcher matcher = depPattern.matcher(content);

        while (matcher.find()) {
            String groupId = matcher.group(1);
            String artifactId = matcher.group(2);
            String version = matcher.group(3);

            if (groupId.startsWith("javax.")) {
                String key = groupId + ":" + artifactId;
                String[] mapping = DEPENDENCY_MAPPINGS.get(key);

                int lineNumber = findLineNumber(lines, matcher.group(0));

                usages.add(new BuildConfigUsage(
                    groupId,
                    artifactId,
                    version,
                    mapping != null ? mapping[0].split(":")[0] : groupId.replace("javax", "jakarta"),
                    mapping != null ? mapping[0].split(":")[1] : artifactId,
                    mapping != null ? mapping[1] : null,
                    lineNumber
                ));
            }
        }

        return usages;
    }

    private int findLineNumber(String[] lines, String searchText) {
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains(searchText)) {
                return i + 1;
            }
        }
        return 1;
    }
}
