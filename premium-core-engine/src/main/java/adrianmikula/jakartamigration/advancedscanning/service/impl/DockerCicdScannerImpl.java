package adrianmikula.jakartamigration.advancedscanning.service.impl;

import adrianmikula.jakartamigration.advancedscanning.domain.DockerCicdUsage;
import adrianmikula.jakartamigration.advancedscanning.domain.FileScanResult;
import adrianmikula.jakartamigration.advancedscanning.domain.ProjectScanResult;
import adrianmikula.jakartamigration.advancedscanning.service.BaseScanner;
import adrianmikula.jakartamigration.advancedscanning.service.DockerCicdScanner;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class DockerCicdScannerImpl extends BaseScanner<DockerCicdUsage> implements DockerCicdScanner {

    // Simplified Java detection patterns - basic yes/no detection
    private static final Pattern[] SIMPLE_JAVA_PATTERNS = {
        Pattern.compile("(?i)java"),           // case-insensitive
        Pattern.compile("(?i)jdk"),
        Pattern.compile("(?i)maven"),
        Pattern.compile("(?i)gradle"),
        Pattern.compile("(?i)spring"),
        Pattern.compile("(?i)openjdk"),
        Pattern.compile("(?i)eclipse-temurin"),
        Pattern.compile("(?i)amazoncorretto"),
        Pattern.compile("(?i)javac"),
        Pattern.compile("(?i)jar"),
        Pattern.compile("(?i)junit"),
        Pattern.compile("(?i)tomcat"),
        Pattern.compile("(?i)jetty")
    };

    // Keep existing patterns for backward compatibility but use simplified patterns as primary
    private static final Pattern[] COMPILE_PATTERNS = {
        Pattern.compile("mvn\\s+(compile|package|install)"),
        Pattern.compile("gradle\\s+(build|jar|assemble)"),
        Pattern.compile("javac\\s+"),
        Pattern.compile("FROM\\s+(openjdk|eclipse-temurin|amazoncorretto):([0-9]+)"),
        Pattern.compile("JAVA_HOME\\s*=?\\s*[\"']?([0-9]+)"),
        Pattern.compile("image:\\s*(openjdk|eclipse-temurin|amazoncorretto):([0-9]+)")
    };

    private static final Pattern[] TEST_PATTERNS = {
        Pattern.compile("mvn\\s+test"),
        Pattern.compile("gradle\\s+test"),
        Pattern.compile("junit"),
        Pattern.compile("testcontainers"),
        Pattern.compile("-Dtest"),
        Pattern.compile("spring-boot:test")
    };

    private static final Pattern[] STATIC_ANALYSIS_PATTERNS = {
        Pattern.compile("sonar-scanner"),
        Pattern.compile("checkstyle"),
        Pattern.compile("spotbugs"),
        Pattern.compile("pmd"),
        Pattern.compile("findbugs"),
        Pattern.compile("jacoco"),
        Pattern.compile("cobertura")
    };

    private static final Pattern[] PACKAGING_PATTERNS = {
        Pattern.compile("docker\\s+build"),
        Pattern.compile("docker\\s+push"),
        Pattern.compile("mvn\\s+package"),
        Pattern.compile("gradle\\s+jar"),
        Pattern.compile("jar\\s+cf"),
        Pattern.compile("spring-boot:build"),
        Pattern.compile("COPY\\s+.*\\.(jar|war)\\s+/")
    };

    private static final Pattern[] RUNTIME_PATTERNS = {
        Pattern.compile("java\\s+-jar"),
        Pattern.compile("java\\s+-cp"),
        Pattern.compile("java\\s+-X"),
        Pattern.compile("spring-boot:run"),
        Pattern.compile("java\\s+-D"),
        Pattern.compile("ENTRYPOINT\\s+[\"']?java"),
        Pattern.compile("CMD\\s+[\"']?java"),
        Pattern.compile("java\\s+-agent")
    };

    @Override
    public ProjectScanResult<FileScanResult<DockerCicdUsage>> scanProject(Path projectPath) {
        if (projectPath == null || !Files.exists(projectPath) || !Files.isDirectory(projectPath)) {
            log.warn("Invalid project path: {}", projectPath);
            return ProjectScanResult.empty();
        }

        try {
            List<Path> dockerCicdFiles = discoverDockerCicdFiles(projectPath);

            if (dockerCicdFiles.isEmpty()) {
                log.info("No Docker/CI-CD files found in project: {}", projectPath);
                return ProjectScanResult.empty();
            }

            log.info("Scanning {} Docker/CI-CD files in project: {}", dockerCicdFiles.size(), projectPath);

            AtomicInteger totalScanned = new AtomicInteger(0);
            List<FileScanResult<DockerCicdUsage>> results = dockerCicdFiles.parallelStream()
                    .map(file -> {
                        totalScanned.incrementAndGet();
                        FileScanResult<DockerCicdUsage> result = scanFile(file);
                        if (result.hasIssues()) {
                            return result;
                        }
                        return null;
                    })
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList());

            int totalUsages = results.stream()
                    .mapToInt(r -> r.usages().size())
                    .sum();

            log.info("Docker/CI-CD scan complete: {} files scanned, {} files with Java references, {} total references",
                    totalScanned.get(), results.size(), totalUsages);

            return new ProjectScanResult<>(results, totalScanned.get(), results.size(), totalUsages);

        } catch (Exception e) {
            log.error("Error scanning project for Docker/CI-CD files: {}", projectPath, e);
            return ProjectScanResult.empty();
        }
    }

    @Override
    public FileScanResult<DockerCicdUsage> scanFile(Path filePath) {
        Path validatedPath = validateFilePath(filePath);
        if (validatedPath == null) {
            return FileScanResult.empty(filePath);
        }

        try {
            String content = Files.readString(validatedPath);
            String fileName = filePath.getFileName().toString();
            String parentPath = filePath.getParent() != null ? filePath.getParent().toString() : "";
            String[] lines = content.split("\n");

            List<DockerCicdUsage> usages = new ArrayList<>();
            DockerCicdUsage.DockerCicdFileType fileType = determineFileType(fileName, parentPath);

            // Use simplified Java detection for all platforms
            usages.addAll(scanForPatterns(lines, SIMPLE_JAVA_PATTERNS, DockerCicdUsage.DockerCicdReferenceType.RUNTIME, fileType));

            return new FileScanResult<>(filePath, usages, lines.length);

        } catch (Exception e) {
            log.warn("Error scanning Docker/CI-CD file: {}", filePath, e);
            return FileScanResult.empty(filePath);
        }
    }

    private List<Path> discoverDockerCicdFiles(Path projectPath) {
        return fileScanner.findFiles(projectPath, path -> {
            String fileName = path.getFileName().toString().toLowerCase();
            String parentPath = path.getParent().toString().toLowerCase();
            
            // Docker files
            if (fileName.equals("dockerfile") || fileName.endsWith(".dockerfile")) {
                return true;
            }
            
            // Docker Compose files
            if (fileName.startsWith("docker-compose") && (fileName.endsWith(".yml") || fileName.endsWith(".yaml"))) {
                return true;
            }
            
            // GitHub Actions
            if (parentPath.contains(".github/workflows") && (fileName.endsWith(".yml") || fileName.endsWith(".yaml"))) {
                return true;
            }
            
            // GitLab CI
            if (fileName.equals(".gitlab-ci.yml")) {
                return true;
            }
            
            // Jenkins
            if (fileName.startsWith("jenkinsfile")) {
                return true;
            }
            
            // Azure Pipelines
            if (fileName.equals("azure-pipelines.yml") || fileName.equals("azure-pipelines.yaml")) {
                return true;
            }
            
            // Bitbucket Pipelines
            if (fileName.equals(".bitbucket-pipelines.yml")) {
                return true;
            }
            
            // AWS CodeBuild
            if (fileName.equals("buildspec.yml")) {
                return true;
            }
            
            // Google Cloud Build
            if (fileName.equals("cloudbuild.yaml")) {
                return true;
            }
            
            // Kubernetes manifests
            if ((fileName.endsWith(".yaml") || fileName.endsWith(".yml")) && 
                parentPath.contains("k8s")) {
                return true;
            }
            
            // Shell scripts
            if (fileName.endsWith(".sh") || fileName.endsWith(".bat") || fileName.endsWith(".ps1")) {
                return true;
            }
            
            return false;
        });
    }

    private DockerCicdUsage.DockerCicdFileType determineFileType(String fileName, String parentPath) {
        fileName = fileName.toLowerCase();
        parentPath = parentPath.toLowerCase();
        
        if (fileName.equals("dockerfile") || fileName.endsWith(".dockerfile")) {
            return DockerCicdUsage.DockerCicdFileType.DOCKERFILE;
        }
        
        if (fileName.startsWith("docker-compose") && (fileName.endsWith(".yml") || fileName.endsWith(".yaml"))) {
            return DockerCicdUsage.DockerCicdFileType.DOCKER_COMPOSE;
        }
        
        // Check for Kubernetes files first (most specific)
        if ((fileName.endsWith(".yaml") || fileName.endsWith(".yml")) && 
            parentPath.contains("k8s")) {
            return DockerCicdUsage.DockerCicdFileType.KUBERNETES;
        }
        
        // Check for specific YAML files next
        if (fileName.equals(".bitbucket-pipelines.yml")) {
            return DockerCicdUsage.DockerCicdFileType.BITBUCKET_PIPELINES;
        }
        
        if (fileName.equals("buildspec.yml")) {
            return DockerCicdUsage.DockerCicdFileType.AWS_CODEBUILD;
        }
        
        if (fileName.equals("cloudbuild.yaml")) {
            return DockerCicdUsage.DockerCicdFileType.GOOGLE_CLOUD_BUILD;
        }
        
        if (fileName.equals("azure-pipelines.yml") || fileName.equals("azure-pipelines.yaml")) {
            return DockerCicdUsage.DockerCicdFileType.AZURE_PIPELINES;
        }
        
        if (fileName.startsWith("jenkinsfile")) {
            return DockerCicdUsage.DockerCicdFileType.JENKINSFILE;
        }
        
        if (fileName.endsWith(".yml") || fileName.endsWith(".yaml")) {
            // Could be GitHub Actions, GitLab CI, or other YAML files
            if (fileName.contains("github") || fileName.contains("workflow")) {
                return DockerCicdUsage.DockerCicdFileType.GITHUB_ACTIONS;
            } else if (fileName.contains("gitlab")) {
                return DockerCicdUsage.DockerCicdFileType.GITLAB_CI;
            } else if (fileName.contains("azure")) {
                return DockerCicdUsage.DockerCicdFileType.AZURE_PIPELINES;
            }
            // Default to GitHub Actions for generic YAML files
            return DockerCicdUsage.DockerCicdFileType.GITHUB_ACTIONS;
        }
        
        if (fileName.endsWith(".sh") || fileName.endsWith(".bat") || fileName.endsWith(".ps1")) {
            return DockerCicdUsage.DockerCicdFileType.SHELL_SCRIPT;
        }
        
        // Default fallback
        return DockerCicdUsage.DockerCicdFileType.SHELL_SCRIPT;
    }

    private List<DockerCicdUsage> scanForPatterns(String[] lines, Pattern[] patterns, 
            DockerCicdUsage.DockerCicdReferenceType referenceType, 
            DockerCicdUsage.DockerCicdFileType fileType) {
        
        List<DockerCicdUsage> usages = new ArrayList<>();
        
        for (int lineNum = 0; lineNum < lines.length; lineNum++) {
            String line = lines[lineNum];
            
            for (Pattern pattern : patterns) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    String command = matcher.group();
                    String javaVersion = extractJavaVersion(command);
                    String migrationNote = generateMigrationNote(referenceType, javaVersion, fileType);
                    
                    usages.add(new DockerCicdUsage(
                        referenceType,
                        fileType,
                        javaVersion,
                        command,
                        migrationNote,
                        lineNum + 1
                    ));
                }
            }
        }
        
        return usages;
    }

    private String extractJavaVersion(String command) {
        // Extract Java version from various patterns
        // Pattern 1: FROM openjdk:8, FROM eclipse-temurin:11, etc.
        Pattern fromPattern = Pattern.compile("(?:FROM|image:\\s*)(?:openjdk|eclipse-temurin|amazoncorretto):([0-9]+)");
        Matcher matcher = fromPattern.matcher(command);
        
        if (matcher.find()) {
            String version = matcher.group(1);
            if (isValidJavaVersion(version)) {
                return version;
            }
        }
        
        // Pattern 2: java-version: '17', JAVA_HOME=/opt/java11, etc.
        Pattern versionPattern = Pattern.compile("(?:java-version|JAVA_HOME)[^0-9]*([0-9]+)");
        matcher = versionPattern.matcher(command);
        
        if (matcher.find()) {
            String version = matcher.group(1);
            if (isValidJavaVersion(version)) {
                return version;
            }
        }
        
        // Pattern 3: JDK 17, Java 11, etc.
        Pattern jdkPattern = Pattern.compile("(?:JDK|Java)\\s*([0-9]+)");
        matcher = jdkPattern.matcher(command);
        
        if (matcher.find()) {
            String version = matcher.group(1);
            if (isValidJavaVersion(version)) {
                return version;
            }
        }
        
        return null;
    }
    
    private boolean isValidJavaVersion(String version) {
        return version.equals("8") || version.equals("11") || version.equals("17") || version.equals("21");
    }

    private String generateMigrationNote(DockerCicdUsage.DockerCicdReferenceType referenceType, 
                                       String javaVersion, 
                                       DockerCicdUsage.DockerCicdFileType fileType) {
        
        StringBuilder note = new StringBuilder();
        
        if (javaVersion != null) {
            note.append("Java ").append(javaVersion).append(" detected. ");
            
            if (javaVersion.equals("8")) {
                note.append("Consider upgrading to Java 11 or 17 for better Jakarta EE support. ");
            } else if (javaVersion.equals("11")) {
                note.append("Compatible with Jakarta EE 8+. Consider Java 17 for latest features. ");
            }
        }
        
        switch (referenceType) {
            case COMPILE:
                note.append("Update build configuration to use Jakarta EE dependencies. ");
                break;
            case TEST:
                note.append("Ensure test frameworks support Jakarta EE APIs. ");
                break;
            case STATIC_ANALYSIS:
                note.append("Configure static analysis tools for Jakarta EE compatibility. ");
                break;
            case PACKAGING:
                note.append("Package with Jakarta EE-compatible dependencies. ");
                break;
            case RUNTIME:
                note.append("Use Jakarta EE-compatible application server or runtime. ");
                break;
        }
        
        return note.toString().trim();
    }
}
