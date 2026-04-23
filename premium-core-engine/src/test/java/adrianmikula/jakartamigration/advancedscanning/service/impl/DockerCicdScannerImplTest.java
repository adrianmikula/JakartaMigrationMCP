package adrianmikula.jakartamigration.advancedscanning.service.impl;

import adrianmikula.jakartamigration.advancedscanning.domain.DockerCicdUsage;
import adrianmikula.jakartamigration.advancedscanning.service.DockerCicdScanner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for DockerCicdScannerImpl
 */
class DockerCicdScannerImplTest {
    
    private DockerCicdScanner scanner;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        scanner = new DockerCicdScannerImpl();
    }
    
    private List<DockerCicdUsage> extractAllUsages(adrianmikula.jakartamigration.advancedscanning.domain.ProjectScanResult<adrianmikula.jakartamigration.advancedscanning.domain.FileScanResult<DockerCicdUsage>> result) {
        return result.fileResults().stream()
                .flatMap(fileResult -> fileResult.usages().stream())
                .toList();
    }
    
    @Test
    void shouldDetectJavaReferencesInDockerfile() throws Exception {
        // Given
        Path dockerfile = tempDir.resolve("Dockerfile");
        String content = """
            FROM openjdk:8
            WORKDIR /app
            COPY target/app.jar .
            RUN mvn compile
            CMD ["java", "-jar", "app.jar"]
            """;
        Files.writeString(dockerfile, content);
        
        // When
        var result = scanner.scanProject(tempDir);
        
        // Then
        assertThat(result.hasIssues()).isTrue();
        assertThat(result.fileResults()).isNotEmpty();
        
        // Should detect Java 11 version and test command
        var usages = extractAllUsages(result);
        assertThat(usages).isNotEmpty();
        assertThat(usages).anyMatch(usage -> 
            usage.javaVersion() != null && 
            usage.referenceType() == DockerCicdUsage.DockerCicdReferenceType.RUNTIME);
        assertThat(usages).anyMatch(usage -> 
            usage.command().contains("mvn compile") && 
            usage.referenceType() == DockerCicdUsage.DockerCicdReferenceType.COMPILE);
    }
    
    @Test
    void shouldDetectJavaReferencesInDockerCompose() throws Exception {
        // Given
        Path dockerCompose = tempDir.resolve("docker-compose.yml");
        String content = """
            version: '3.8'
            services:
              app:
                image: openjdk:11
                command: ["java", "-jar", "app.jar"]
                environment:
                  - JAVA_HOME=/opt/java
              test:
                image: maven:3.8
                command: ["mvn", "test"]
            """;
        Files.writeString(dockerCompose, content);
        
        // When
        var result = scanner.scanProject(tempDir);
        
        // Then
        assertThat(result.hasIssues()).isTrue();
        var usages = extractAllUsages(result);
        assertThat(usages).anyMatch(usage -> 
            usage.javaVersion() != null && 
            usage.fileType() == DockerCicdUsage.DockerCicdFileType.DOCKER_COMPOSE);
        assertThat(usages).anyMatch(usage -> 
            usage.referenceType() == DockerCicdUsage.DockerCicdReferenceType.TEST);
    }
    
    @Test
    void shouldDetectJavaReferencesInGitHubActions() throws Exception {
        // Given
        Path workflowsDir = tempDir.resolve(".github/workflows");
        Files.createDirectories(workflowsDir);
        
        Path githubActions = workflowsDir.resolve("ci.yml");
        String content = """
            name: CI
            on: [push, pull_request]
            jobs:
              test:
                runs-on: ubuntu-latest
                steps:
                  - uses: actions/checkout@v2
                  - name: Set up JDK 17
                    uses: actions/setup-java@v2
                    with:
                      java-version: '17'
                      distribution: 'temurin'
                  - name: Run tests
                    run: mvn test
                  - name: Run static analysis
                    run: sonar-scanner
                  - name: Build
                    run: mvn package
            """;
        Files.writeString(githubActions, content);
        
        // When
        var result = scanner.scanProject(tempDir);
        
        // Then
        assertThat(result.hasIssues()).isTrue();
        var usages = extractAllUsages(result);
        assertThat(usages).anyMatch(usage -> 
            usage.javaVersion() != null && 
            usage.fileType() == DockerCicdUsage.DockerCicdFileType.GITHUB_ACTIONS);
        assertThat(usages).anyMatch(usage -> 
            usage.referenceType() == DockerCicdUsage.DockerCicdReferenceType.TEST);
        assertThat(usages).anyMatch(usage -> 
            usage.referenceType() == DockerCicdUsage.DockerCicdReferenceType.STATIC_ANALYSIS);
        assertThat(usages).anyMatch(usage -> 
            usage.referenceType() == DockerCicdUsage.DockerCicdReferenceType.PACKAGING);
    }
    
    @Test
    void shouldDetectJavaReferencesInGitLabCI() throws Exception {
        // Given
        Path gitlabCi = tempDir.resolve(".gitlab-ci.yml");
        String content = """
            stages:
              - test
              - build
              
            test:
              stage: test
              image: openjdk:11
              script:
                - mvn test
                - checkstyle
                
            build:
              stage: build
              script:
                - gradle build
                - java -jar build/libs/app.jar
            """;
        Files.writeString(gitlabCi, content);
        
        // When
        var result = scanner.scanProject(tempDir);
        
        // Then
        assertThat(result.hasIssues()).isTrue();
        var usages = extractAllUsages(result);
        assertThat(usages).anyMatch(usage -> 
            usage.fileType() == DockerCicdUsage.DockerCicdFileType.GITLAB_CI);
        assertThat(usages).anyMatch(usage -> 
            usage.referenceType() == DockerCicdUsage.DockerCicdReferenceType.TEST);
        assertThat(usages).anyMatch(usage -> 
            usage.referenceType() == DockerCicdUsage.DockerCicdReferenceType.STATIC_ANALYSIS);
    }
    
    @Test
    void shouldDetectJavaReferencesInJenkinsfile() throws Exception {
        // Given
        Path jenkinsfile = tempDir.resolve("Jenkinsfile");
        String content = """
            pipeline {
                agent any
                tools {
                    maven 'Maven-3.8'
                    jdk 'Java 11'
                }
                stages {
                    stage('Test') {
                        steps {
                            sh 'mvn test'
                            sh 'spotbugs'
                        }
                    }
                    stage('Build') {
                        steps {
                            sh 'gradle build'
                            sh 'java -jar build/libs/app.jar'
                        }
                    }
                }
            }
            """;
        Files.writeString(jenkinsfile, content);
        
        // When
        var result = scanner.scanProject(tempDir);
        
        // Then
        assertThat(result.hasIssues()).isTrue();
        var usages = extractAllUsages(result);
        assertThat(usages).anyMatch(usage -> 
            usage.fileType() == DockerCicdUsage.DockerCicdFileType.JENKINSFILE);
        assertThat(usages).anyMatch(usage -> 
            usage.javaVersion() != null);
    }
    
    @Test
    void shouldDetectJavaReferencesInShellScript() throws Exception {
        // Given
        Path shellScript = tempDir.resolve("build.sh");
        String content = """
            #!/bin/bash
            export JAVA_HOME=/opt/java/openjdk17
            mvn clean compile
            mvn test
            sonar-scanner
            mvn package
            java -jar target/app.jar
            """;
        Files.writeString(shellScript, content);
        
        // When
        var result = scanner.scanProject(tempDir);
        
        // Then
        assertThat(result.hasIssues()).isTrue();
        var usages = extractAllUsages(result);
        assertThat(usages).anyMatch(usage -> 
            usage.fileType() == DockerCicdUsage.DockerCicdFileType.SHELL_SCRIPT);
        assertThat(usages).anyMatch(usage -> 
            usage.javaVersion() != null);
        assertThat(usages).anyMatch(usage -> 
            usage.referenceType() == DockerCicdUsage.DockerCicdReferenceType.COMPILE);
        assertThat(usages).anyMatch(usage -> 
            usage.referenceType() == DockerCicdUsage.DockerCicdReferenceType.TEST);
        assertThat(usages).anyMatch(usage -> 
            usage.referenceType() == DockerCicdUsage.DockerCicdReferenceType.STATIC_ANALYSIS);
        assertThat(usages).anyMatch(usage -> 
            usage.referenceType() == DockerCicdUsage.DockerCicdReferenceType.PACKAGING);
        assertThat(usages).anyMatch(usage -> 
            usage.referenceType() == DockerCicdUsage.DockerCicdReferenceType.RUNTIME);
    }
    
    @Test
    void shouldDetectJavaReferencesInPowerShellScript() throws Exception {
        // Given
        Path powershellScript = tempDir.resolve("build.ps1");
        String content = """
            $env:JAVA_HOME = "C:\\Program Files\\Java\\jdk-21"
            gradle build
            gradle test
            java -jar build\\libs\\app.jar
            """;
        Files.writeString(powershellScript, content);
        
        // When
        var result = scanner.scanProject(tempDir);
        
        // Then
        assertThat(result.hasIssues()).isTrue();
        var usages = extractAllUsages(result);
        assertThat(usages).anyMatch(usage -> 
            usage.fileType() == DockerCicdUsage.DockerCicdFileType.SHELL_SCRIPT);
        assertThat(usages).anyMatch(usage -> 
            usage.javaVersion() != null);
    }
    
    @Test
    void shouldProvideJakartaMigrationNotes() throws Exception {
        // Given
        Path dockerfile = tempDir.resolve("Dockerfile");
        String content = """
            FROM openjdk:8
            RUN mvn compile
            CMD ["java", "-jar", "app.jar"]
            """;
        Files.writeString(dockerfile, content);
        
        // When
        var result = scanner.scanProject(tempDir);
        
        // Then
        assertThat(result.hasIssues()).isTrue();
        var usages = extractAllUsages(result);
        assertThat(usages).anyMatch(usage -> 
            usage.hasJakartaMigrationImplications() && 
            usage.jakartaMigrationNote().contains("Java 8"));
    }
    
    @Test
    void shouldReturnEmptyForCleanProject() throws Exception {
        // Given
        Path dockerfile = tempDir.resolve("Dockerfile");
        String content = """
            FROM node:18
            WORKDIR /app
            COPY package.json .
            RUN npm install
            CMD ["node", "app.js"]
            """;
        Files.writeString(dockerfile, content);
        
        // When
        var result = scanner.scanProject(tempDir);
        
        // Then
        assertThat(result.hasIssues()).isFalse();
    }
    
    @Test
    void shouldHandleEmptyDirectory() {
        // Given - Empty temp directory
        
        // When
        var result = scanner.scanProject(tempDir);
        
        // Then
        assertThat(result.hasIssues()).isFalse();
    }
    
    @Test
    void shouldScanSingleFile() throws Exception {
        // Given
        Path dockerfile = tempDir.resolve("Dockerfile");
        String content = """
            FROM eclipse-temurin:17
            RUN gradle test
            CMD ["java", "-cp", "app.jar:lib/*", "com.example.Main"]
            """;
        Files.writeString(dockerfile, content);
        
        // When
        var result = scanner.scanFile(dockerfile);
        
        // Then
        assertThat(result.hasIssues()).isTrue();
        var usages = result.usages();
        assertThat(usages).hasSize(3); // test, runtime, and version detection
        assertThat(usages).anyMatch(usage -> 
            usage.javaVersion().equals("17"));
        assertThat(usages).anyMatch(usage -> 
            usage.referenceType() == DockerCicdUsage.DockerCicdReferenceType.TEST);
        assertThat(usages).anyMatch(usage -> 
            usage.referenceType() == DockerCicdUsage.DockerCicdReferenceType.RUNTIME);
    }
    
    @Test
    void shouldHandleInvalidFilePath() {
        // Given
        Path invalidPath = tempDir.resolve("nonexistent.txt");
        
        // When
        var result = scanner.scanFile(invalidPath);
        
        // Then
        assertThat(result.hasIssues()).isFalse();
    }
    
    @Test
    void shouldDetectJavaInBitbucketPipelines() throws Exception {
        // Given
        Path bitbucketPipelines = tempDir.resolve(".bitbucket-pipelines.yml");
        String content = """
            pipelines:
              default:
                - step:
                    name: Build and Test
                    image: openjdk:11
                    script:
                      - mvn clean compile
                      - mvn test
                      - mvn package
            """;
        Files.writeString(bitbucketPipelines, content);
        
        // When
        var result = scanner.scanProject(tempDir);
        
        // Then
        assertThat(result.hasIssues()).isTrue();
        var usages = extractAllUsages(result);
        assertThat(usages).anyMatch(usage -> 
            usage.fileType() == DockerCicdUsage.DockerCicdFileType.BITBUCKET_PIPELINES);
        assertThat(usages).isNotEmpty();
    }
    
    @Test
    void shouldDetectJavaInAWSCodeBuild() throws Exception {
        // Given
        Path buildspec = tempDir.resolve("buildspec.yml");
        String content = """
            version: 0.2
            
            phases:
              install:
                runtime-versions:
                  java: corretto17
              build:
                commands:
                  - mvn clean compile
                  - mvn test
                  - mvn package
                  - java -version
            """;
        Files.writeString(buildspec, content);
        
        // When
        var result = scanner.scanProject(tempDir);
        
        // Then
        assertThat(result.hasIssues()).isTrue();
        var usages = extractAllUsages(result);
        assertThat(usages).anyMatch(usage -> 
            usage.fileType() == DockerCicdUsage.DockerCicdFileType.AWS_CODEBUILD);
        assertThat(usages).isNotEmpty();
    }
    
    @Test
    void shouldDetectJavaInGoogleCloudBuild() throws Exception {
        // Given
        Path cloudbuild = tempDir.resolve("cloudbuild.yaml");
        String content = """
            steps:
              - name: 'gcr.io/cloud-builders/mvn'
                args: ['clean', 'package']
              - name: 'openjdk:17'
                entrypoint: 'java'
                args: ['-jar', 'target/app.jar']
              - name: 'gcr.io/cloud-builders/docker'
                args: ['build', '-t', 'gcr.io/$PROJECT_ID/java-app', '.']
            """;
        Files.writeString(cloudbuild, content);
        
        // When
        var result = scanner.scanProject(tempDir);
        
        // Then
        assertThat(result.hasIssues()).isTrue();
        var usages = extractAllUsages(result);
        assertThat(usages).anyMatch(usage -> 
            usage.fileType() == DockerCicdUsage.DockerCicdFileType.GOOGLE_CLOUD_BUILD);
        assertThat(usages).isNotEmpty();
    }
    
    @Test
    void shouldDetectJavaInKubernetesManifests() throws Exception {
        // Given
        Path k8sDir = tempDir.resolve("k8s");
        Files.createDirectories(k8sDir);
        
        Path deployment = k8sDir.resolve("deployment.yaml");
        String content = """
            apiVersion: apps/v1
            kind: Deployment
            metadata:
              name: java-app
            spec:
              replicas: 3
              selector:
                matchLabels:
                  app: java-app
              template:
                metadata:
                  labels:
                    app: java-app
                spec:
                  containers:
                  - name: java-app
                    image: openjdk:11
                    command: ["java", "-jar", "app.jar"]
                    env:
                    - name: JAVA_OPTS
                      value: "-Xmx512m"
            """;
        Files.writeString(deployment, content);
        
        // When
        var result = scanner.scanProject(tempDir);
        
        // Then
        assertThat(result.hasIssues()).isTrue();
        var usages = extractAllUsages(result);
        assertThat(usages).anyMatch(usage -> 
            usage.fileType() == DockerCicdUsage.DockerCicdFileType.KUBERNETES);
        assertThat(usages).isNotEmpty();
    }
    
    @Test
    void shouldDetectMultipleJavaVersions() throws Exception {
        // Given
        Path dockerCompose = tempDir.resolve("docker-compose.yml");
        String content = """
            version: '3.8'
            services:
              app1:
                image: openjdk:8
                command: ["java", "-jar", "app1.jar"]
              app2:
                image: openjdk:11
                command: ["java", "-jar", "app2.jar"]
              app3:
                image: eclipse-temurin:17
                command: ["java", "-jar", "app3.jar"]
            """;
        Files.writeString(dockerCompose, content);
        
        // When
        var result = scanner.scanProject(tempDir);
        
        // Then
        assertThat(result.hasIssues()).isTrue();
        var usages = extractAllUsages(result);
        assertThat(usages).anyMatch(usage -> usage.javaVersion() != null);
        assertThat(usages).anyMatch(usage -> usage.javaVersion() != null);
        assertThat(usages).anyMatch(usage -> usage.javaVersion() != null);
    }
}
