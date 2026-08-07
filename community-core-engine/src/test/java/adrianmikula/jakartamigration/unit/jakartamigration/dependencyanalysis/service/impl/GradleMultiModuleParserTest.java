package unit.jakartamigration.dependencyanalysis.service.impl;

import adrianmikula.jakartamigration.dependencyanalysis.domain.Artifact;
import adrianmikula.jakartamigration.dependencyanalysis.domain.DependencyGraph;
import adrianmikula.jakartamigration.dependencyanalysis.service.impl.GradleMultiModuleParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GradleMultiModuleParser Tests")
class GradleMultiModuleParserTest {

    private GradleMultiModuleParser parser;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        parser = new GradleMultiModuleParser();
    }

    @Test
    @DisplayName("Should discover modules from settings.gradle.kts")
    void shouldDiscoverModulesFromSettings() throws Exception {
        // Given
        Path settingsFile = tempDir.resolve("settings.gradle.kts");
        Files.writeString(settingsFile, """
                rootProject.name = "my-project"
                
                include("core")
                include("web")
                include("data")
                """);

        // When
        List<String> modules = parser.discoverModules(tempDir);

        // Then
        assertThat(modules).containsExactlyInAnyOrder("core", "web", "data");
    }

    @Test
    @DisplayName("Should discover modules from settings.gradle (Groovy)")
    void shouldDiscoverModulesFromGroovySettings() throws Exception {
        // Given
        Path settingsFile = tempDir.resolve("settings.gradle");
        Files.writeString(settingsFile, """
                rootProject.name = 'my-project'
                
                include 'core'
                include 'web'
                """);

        // When
        List<String> modules = parser.discoverModules(tempDir);

        // Then
        assertThat(modules).containsExactlyInAnyOrder("core", "web");
    }

    @Test
    @DisplayName("Should parse multi-module project with settings.gradle.kts")
    void shouldParseMultiModuleProject() throws Exception {
        // Given
        Path settingsFile = tempDir.resolve("settings.gradle.kts");
        Files.writeString(settingsFile, """
                rootProject.name = "multi-module"
                
                include("core")
                include("web")
                """);

        // Root build file with no direct dependencies
        Files.writeString(tempDir.resolve("build.gradle.kts"), """
                plugins {
                    java
                }
                """);

        // Core module with dependencies
        Path coreDir = tempDir.resolve("core");
        Files.createDirectories(coreDir);
        Files.writeString(coreDir.resolve("build.gradle.kts"), """
                dependencies {
                    implementation("org.slf4j:slf4j-api:2.0.9")
                    implementation("com.google.guava:guava:32.1.3-jre")
                    compileOnly("org.projectlombok:lombok:1.18.30")
                }
                """);

        // Web module with dependencies
        Path webDir = tempDir.resolve("web");
        Files.createDirectories(webDir);
        Files.writeString(webDir.resolve("build.gradle.kts"), """
                dependencies {
                    implementation("org.springframework.boot:spring-boot-starter-web:3.2.0")
                    implementation(project(":core"))
                    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
                }
                """);

        // When
        DependencyGraph graph = parser.parseProject(tempDir);

        // Then
        assertThat(graph.nodeCount()).isGreaterThanOrEqualTo(6); // root + core deps + web deps

        Set<String> artifactIds = graph.getNodes().stream()
                .map(Artifact::artifactId)
                .collect(Collectors.toSet());

        assertThat(artifactIds).contains(
                "slf4j-api",
                "guava",
                "lombok",
                "spring-boot-starter-web",
                "junit-jupiter"
        );
    }

    @Test
    @DisplayName("Should parse versionless dependencies")
    void shouldParseVersionlessDependencies() throws Exception {
        // Given
        String content = """
                dependencies {
                    implementation("org.springframework.boot:spring-boot-starter-web")
                    api("com.google.guava:guava")
                }
                """;

        // When
        List<Artifact> deps = parser.parseDependencies(content);

        // Then
        assertThat(deps).hasSize(2);

        Artifact webStarter = deps.stream()
                .filter(d -> d.artifactId().equals("spring-boot-starter-web"))
                .findFirst().orElseThrow();
        assertThat(webStarter.version()).isEqualTo("unknown");
        assertThat(webStarter.scope()).isEqualTo("compile");

        Artifact guava = deps.stream()
                .filter(d -> d.artifactId().equals("guava"))
                .findFirst().orElseThrow();
        assertThat(guava.version()).isEqualTo("unknown");
        assertThat(guava.scope()).isEqualTo("compile");
    }

    @Test
    @DisplayName("Should map test configurations to test scope")
    void shouldMapTestScope() throws Exception {
        // Given
        String content = """
                dependencies {
                    testImplementation("junit:junit:4.13.1")
                    testRuntimeOnly("org.slf4j:slf4j-simple:2.0.9")
                    testCompileOnly("com.google.code.findbugs:jsr305:3.0.2")
                }
                """;

        // When
        List<Artifact> deps = parser.parseDependencies(content);

        // Then
        assertThat(deps).hasSize(3);
        assertThat(deps).allMatch(d -> d.scope().equals("test"));
    }

    @Test
    @DisplayName("Should skip project() dependencies")
    void shouldSkipProjectDependencies() throws Exception {
        // Given
        String content = """
                dependencies {
                    implementation(project(":core"))
                    implementation("org.slf4j:slf4j-api:2.0.9")
                    api(project(":common"))
                }
                """;

        // When
        List<Artifact> deps = parser.parseDependencies(content);

        // Then
        assertThat(deps).hasSize(1);
        assertThat(deps.get(0).artifactId()).isEqualTo("slf4j-api");
    }

    @Test
    @DisplayName("Should skip platform/BOM imports")
    void shouldSkipPlatformImports() throws Exception {
        // Given
        String content = """
                dependencies {
                    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.2.0"))
                    implementation("org.springframework.boot:spring-boot-starter-web:3.2.0")
                }
                """;

        // When
        List<Artifact> deps = parser.parseDependencies(content);

        // Then
        assertThat(deps).hasSize(1);
        assertThat(deps.get(0).artifactId()).isEqualTo("spring-boot-starter-web");
    }

    @Test
    @DisplayName("Should parse Groovy DSL dependencies")
    void shouldParseGroovyDsl() throws Exception {
        // Given
        String content = """
                dependencies {
                    implementation 'javax.servlet:javax.servlet-api:4.0.1'
                    testImplementation 'junit:junit:4.13.1'
                    runtimeOnly 'com.h2database:h2:2.1.212'
                }
                """;

        // When
        List<Artifact> deps = parser.parseDependencies(content);

        // Then
        assertThat(deps).hasSize(3);

        Set<String> artifactIds = deps.stream().map(Artifact::artifactId).collect(Collectors.toSet());
        assertThat(artifactIds).containsExactlyInAnyOrder("javax.servlet-api", "junit", "h2");
    }

    @Test
    @DisplayName("Should deduplicate artifacts across modules")
    void shouldDeduplicateAcrossModules() throws Exception {
        // Given — both modules depend on the same library
        Path settingsFile = tempDir.resolve("settings.gradle.kts");
        Files.writeString(settingsFile, """
                rootProject.name = "dedup-test"
                include("mod-a")
                include("mod-b")
                """);

        Files.writeString(tempDir.resolve("build.gradle.kts"), "plugins { java }");

        Path modADir = tempDir.resolve("mod-a");
        Files.createDirectories(modADir);
        Files.writeString(modADir.resolve("build.gradle.kts"), """
                dependencies {
                    implementation("org.slf4j:slf4j-api:2.0.9")
                }
                """);

        Path modBDir = tempDir.resolve("mod-b");
        Files.createDirectories(modBDir);
        Files.writeString(modBDir.resolve("build.gradle.kts"), """
                dependencies {
                    implementation("org.slf4j:slf4j-api:2.0.9")
                }
                """);

        // When
        DependencyGraph graph = parser.parseProject(tempDir);

        // Then — slf4j-api should appear only once as a node
        long slf4jCount = graph.getNodes().stream()
                .filter(a -> a.artifactId().equals("slf4j-api"))
                .count();
        assertThat(slf4jCount).isEqualTo(1);

        // But should have edges from both modules
        long slf4jEdges = graph.getEdges().stream()
                .filter(e -> e.to().artifactId().equals("slf4j-api"))
                .count();
        assertThat(slf4jEdges).isEqualTo(2);
    }

    @Test
    @DisplayName("Should fall back to directory scan when no settings file")
    void shouldFallbackToDirectoryScan() throws Exception {
        // Given — no settings file, but subdirs with build files
        Path coreDir = tempDir.resolve("core");
        Files.createDirectories(coreDir);
        Files.writeString(coreDir.resolve("build.gradle.kts"), """
                dependencies {
                    implementation("org.slf4j:slf4j-api:2.0.9")
                }
                """);

        // When
        List<String> modules = parser.discoverModules(tempDir);

        // Then
        assertThat(modules).contains("core");
    }

    @Test
    @DisplayName("Should handle nested module paths from settings")
    void shouldHandleNestedModulePaths() throws Exception {
        // Given
        Path settingsFile = tempDir.resolve("settings.gradle.kts");
        Files.writeString(settingsFile, """
                rootProject.name = "nested"
                include("libs:core")
                include("libs:web")
                """);

        Files.writeString(tempDir.resolve("build.gradle.kts"), "plugins { java }");

        // Create nested module directories
        Path libsCoreDir = tempDir.resolve("libs/core");
        Files.createDirectories(libsCoreDir);
        Files.writeString(libsCoreDir.resolve("build.gradle.kts"), """
                dependencies {
                    implementation("org.slf4j:slf4j-api:2.0.9")
                }
                """);

        Path libsWebDir = tempDir.resolve("libs/web");
        Files.createDirectories(libsWebDir);
        Files.writeString(libsWebDir.resolve("build.gradle.kts"), """
                dependencies {
                    implementation("org.springframework.boot:spring-boot-starter-web:3.2.0")
                }
                """);

        // When
        DependencyGraph graph = parser.parseProject(tempDir);

        // Then
        Set<String> artifactIds = graph.getNodes().stream()
                .map(Artifact::artifactId)
                .collect(Collectors.toSet());

        assertThat(artifactIds).contains("slf4j-api", "spring-boot-starter-web");
    }

    @Test
    @DisplayName("Should map annotationProcessor to provided scope")
    void shouldMapAnnotationProcessorScope() throws Exception {
        // Given
        String content = "dependencies {\n" +
                "    annotationProcessor(\"org.projectlombok:lombok:1.18.30\")\n" +
                "    implementation(\"org.slf4j:slf4j-api:2.0.9\")\n" +
                "}\n";

        // When
        List<Artifact> deps = parser.parseDependencies(content);

        // Then
        assertThat(deps).hasSize(2);
        Artifact lombok = deps.stream()
                .filter(d -> d.artifactId().equals("lombok"))
                .findFirst().orElseThrow();
        assertThat(lombok.scope()).isEqualTo("provided");

        Artifact slf4j = deps.stream()
                .filter(d -> d.artifactId().equals("slf4j-api"))
                .findFirst().orElseThrow();
        assertThat(slf4j.scope()).isEqualTo("compile");
    }

    @Test
    @DisplayName("Should discover many dependencies across many modules (regression: only 2 deps found)")
    void shouldFindDependenciesAcrossManyModules() throws Exception {
        // Given — realistic Spring Boot multi-module project structure
        // Root build.gradle.kts typically only has plugin declarations, NO actual dependencies
        Files.writeString(tempDir.resolve("settings.gradle.kts"), """
                rootProject.name = "enterprise-app"

                include("core-api")
                include("core-impl")
                include("web")
                include("data-access")
                """);

        // Root: only plugins (this is what caused the original bug — root had 0 deps)
        Files.writeString(tempDir.resolve("build.gradle.kts"), """
                plugins {
                    java
                    id("org.springframework.boot") version "3.2.0"
                    id("io.spring.dependency-management") version "1.1.4"
                }
                """);

        // core-api: javax deps
        Files.createDirectories(tempDir.resolve("core-api"));
        Files.writeString(tempDir.resolve("core-api/build.gradle.kts"), """
                dependencies {
                    implementation("javax.servlet:javax.servlet-api:4.0.1")
                    implementation("javax.annotation:javax.annotation-api:1.3.2")
                    implementation("javax.validation:validation-api:2.0.1")
                }
                """);

        // core-impl: jakarta deps
        Files.createDirectories(tempDir.resolve("core-impl"));
        Files.writeString(tempDir.resolve("core-impl/build.gradle.kts"), """
                dependencies {
                    implementation(project(":core-api"))
                    implementation("jakarta.servlet:jakarta.servlet-api:6.0.0")
                    implementation("jakarta.annotation:jakarta.annotation-api:2.1.0")
                    implementation("org.slf4j:slf4j-api:2.0.9")
                }
                """);

        // web: Spring Boot web + Jackson
        Files.createDirectories(tempDir.resolve("web"));
        Files.writeString(tempDir.resolve("web/build.gradle.kts"), """
                dependencies {
                    implementation(project(":core-api"))
                    implementation("org.springframework.boot:spring-boot-starter-web:3.2.0")
                    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.3")
                }
                """);

        // data-access: persistence layer
        Files.createDirectories(tempDir.resolve("data-access"));
        Files.writeString(tempDir.resolve("data-access/build.gradle.kts"), """
                dependencies {
                    implementation(project(":core-api"))
                    implementation("org.springframework.boot:spring-boot-starter-data-jpa:3.2.0")
                    implementation("org.hibernate.orm:hibernate-core:6.4.0.Final")
                    implementation("org.postgresql:postgresql:42.7.1")
                    compileOnly("org.projectlombok:lombok:1.18.30")
                    annotationProcessor("org.projectlombok:lombok:1.18.30")
                }
                """);

        // Debug: check individual file parsing
        String coreImplContent = Files.readString(tempDir.resolve("core-impl/build.gradle.kts"));
        List<Artifact> coreImplDeps = parser.parseDependencies(coreImplContent);
        assertThat(coreImplDeps.stream().map(Artifact::artifactId).collect(Collectors.toList()))
                .as("Direct parse of core-impl build.gradle.kts")
                .contains("jakarta.servlet-api", "jakarta.annotation-api", "slf4j-api");

        // When
        DependencyGraph graph = parser.parseProject(tempDir);

        // Then — should find all deps from all 4 submodules (minus project() deps)
        Set<String> artifactIds = graph.getNodes().stream()
                .map(Artifact::artifactId)
                .collect(Collectors.toSet());

        // Must find javax deps from core-api
        assertThat(artifactIds).contains(
                "javax.servlet-api",
                "javax.annotation-api",
                "validation-api"
        );

        // Must find jakarta deps from core-impl
        assertThat(artifactIds).contains(
                "jakarta.servlet-api",
                "jakarta.annotation-api",
                "slf4j-api"
        );

        // Must find deps from web and data-access
        assertThat(artifactIds).contains(
                "spring-boot-starter-web",
                "spring-boot-starter-data-jpa",
                "jackson-databind",
                "postgresql",
                "hibernate-core",
                "lombok"
        );

        // Root project artifact should also be present
        assertThat(graph.nodeCount()).isGreaterThanOrEqualTo(14);
    }

    @Test
    @DisplayName("Should handle root build.gradle with only plugins block")
    void shouldHandleRootWithOnlyPlugins() throws Exception {
        // Given — root has plugins only, submodules have all deps
        Files.writeString(tempDir.resolve("settings.gradle"), """
                include 'app'
                """);

        Files.writeString(tempDir.resolve("build.gradle"), """
                plugins {
                    id 'java'
                    id 'org.springframework.boot' version '3.2.0'
                }
                """);

        Files.createDirectories(tempDir.resolve("app"));
        Files.writeString(tempDir.resolve("app/build.gradle"), """
                dependencies {
                    implementation 'org.springframework.boot:spring-boot-starter-web:3.2.0'
                    implementation 'org.springframework.boot:spring-boot-starter-data-jpa:3.2.0'
                    implementation 'com.google.guava:guava:32.1.3-jre'
                }
                """);

        // When
        DependencyGraph graph = parser.parseProject(tempDir);

        // Then — should find deps from the app module, not just root
        assertThat(graph.nodeCount()).isGreaterThanOrEqualTo(4); // root + 3 deps

        Set<String> artifactIds = graph.getNodes().stream()
                .map(Artifact::artifactId)
                .collect(Collectors.toSet());
        assertThat(artifactIds).contains("spring-boot-starter-web", "guava");
    }

    @Test
    @DisplayName("Should handle deeply nested module structure")
    void shouldHandleDeeplyNestedModules() throws Exception {
        // Given
        Files.writeString(tempDir.resolve("settings.gradle.kts"), """
                rootProject.name = "deep-project"

                include("platform:common")
                include("platform:shared")
                include("apps:web-app")
                include("apps:batch-app")
                """);

        Files.writeString(tempDir.resolve("build.gradle.kts"), "plugins { java }");

        // platform/common
        Files.createDirectories(tempDir.resolve("platform/common"));
        Files.writeString(tempDir.resolve("platform/common/build.gradle.kts"), """
                dependencies {
                    api("com.google.guava:guava:32.1.3-jre")
                    api("org.slf4j:slf4j-api:2.0.9")
                }
                """);

        // platform/shared
        Files.createDirectories(tempDir.resolve("platform/shared"));
        Files.writeString(tempDir.resolve("platform/shared/build.gradle.kts"), """
                dependencies {
                    api(project(":platform:common"))
                    implementation("javax.servlet:javax.servlet-api:4.0.1")
                }
                """);

        // apps/web-app
        Files.createDirectories(tempDir.resolve("apps/web-app"));
        Files.writeString(tempDir.resolve("apps/web-app/build.gradle.kts"), """
                dependencies {
                    implementation(project(":platform:shared"))
                    implementation("jakarta.servlet:jakarta.servlet-api:6.0.0")
                }
                """);

        // apps/batch-app
        Files.createDirectories(tempDir.resolve("apps/batch-app"));
        Files.writeString(tempDir.resolve("apps/batch-app/build.gradle.kts"), """
                dependencies {
                    implementation(project(":platform:common"))
                    implementation("org.springframework.batch:spring-batch-core:5.1.0")
                }
                """);

        // When
        DependencyGraph graph = parser.parseProject(tempDir);

        // Then
        Set<String> artifactIds = graph.getNodes().stream()
                .map(Artifact::artifactId)
                .collect(Collectors.toSet());

        assertThat(artifactIds).contains(
                "guava",
                "slf4j-api",
                "javax.servlet-api",
                "jakarta.servlet-api",
                "spring-batch-core"
        );
    }
}
