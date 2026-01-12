plugins {
    java
    id("org.springframework.boot") version "3.2.0"
    id("io.spring.dependency-management") version "1.1.4"
    id("org.openrewrite.rewrite") version "6.8.0"
    jacoco
    // Code Quality Tools
    id("com.github.spotbugs") version "5.0.14"
    id("pmd")
    id("checkstyle")
    id("org.owasp.dependencycheck") version "9.0.9"
}

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.io.File

group = "adrianmikula"
version = "1.0.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://repo.spring.io/milestone")
    }
    maven {
        url = uri("https://repo.spring.io/snapshot")
    }
    // JitPack for spring-ai-community mcp-annotations if needed
    maven {
        url = uri("https://jitpack.io")
    }
}

extra["springAiVersion"] = "1.1.2"
extra["resilience4jVersion"] = "2.1.0"
extra["jgitVersion"] = "6.8.0.202311291450-r"
extra["mockWebServerVersion"] = "4.12.0"
extra["testcontainersVersion"] = "1.19.3"
extra["awaitilityVersion"] = "4.2.0"
extra["openrewriteVersion"] = "8.10.0"
extra["openrewriteMavenPluginVersion"] = "5.40.0"
extra["rewriteMigrateJavaVersion"] = "2.5.0"
extra["rewriteSpringVersion"] = "5.10.0"

dependencies {
    // DEPEND ON FREE PACKAGE - No code duplication!
    // This project dependency will be replaced with published JAR when premium moves to separate repo
    implementation(project(":"))

    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // Spring AI MCP Server
    implementation("org.springframework.ai:spring-ai-starter-mcp-server-webmvc:${property("springAiVersion")}")
    implementation("org.springframework.ai:spring-ai-mcp-annotations:${property("springAiVersion")}")
    implementation("org.springaicommunity:mcp-annotations:0.8.0")

    // Resilience4j
    implementation("io.github.resilience4j:resilience4j-spring-boot3:${property("resilience4jVersion")}")
    implementation("io.github.resilience4j:resilience4j-circuitbreaker:${property("resilience4jVersion")}")
    implementation("io.github.resilience4j:resilience4j-ratelimiter:${property("resilience4jVersion")}")

    // Git Operations
    implementation("org.eclipse.jgit:org.eclipse.jgit:${property("jgitVersion")}")

    // OpenRewrite for automated refactoring and Jakarta migration (PREMIUM)
    implementation("org.openrewrite:rewrite-java:${property("openrewriteVersion")}")
    implementation("org.openrewrite:rewrite-maven:${property("openrewriteVersion")}")
    implementation("org.openrewrite.recipe:rewrite-migrate-java:${property("rewriteMigrateJavaVersion")}")
    implementation("org.openrewrite.recipe:rewrite-spring:${property("rewriteSpringVersion")}")
    
    // ASM for bytecode analysis (PREMIUM)
    implementation("org.ow2.asm:asm:9.6")
    implementation("org.ow2.asm:asm-commons:9.6")
    
    // SnakeYAML for parsing Jakarta mappings YAML file
    implementation("org.yaml:snakeyaml:2.2")
    
    // japicmp for binary compatibility checking
    implementation("com.github.siom79.japicmp:japicmp:0.18.0")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:junit-jupiter:${property("testcontainersVersion")}")
    testImplementation("org.testcontainers:testcontainers:${property("testcontainersVersion")}")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("com.squareup.okhttp3:mockwebserver:${property("mockWebServerVersion")}")
    testImplementation("org.awaitility:awaitility:${property("awaitilityVersion")}")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
    imports {
        mavenBom("org.testcontainers:testcontainers-bom:${property("testcontainersVersion")}")
        mavenBom("org.springframework.ai:spring-ai-bom:${property("springAiVersion")}")
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

// JaCoCo Configuration
jacoco {
    toolVersion = "0.8.11"
    reportsDirectory.set(layout.buildDirectory.dir("reports/jacoco"))
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("jakarta-migration-mcp-premium-${project.version}.jar")
    manifest {
        attributes(
            "Implementation-Title" to "Jakarta Migration MCP Server (Premium)",
            "Implementation-Version" to project.version,
            "Built-By" to System.getProperty("user.name"),
            "Built-Date" to LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            "Created-By" to "Gradle ${gradle.gradleVersion}"
        )
    }
}

// OpenRewrite Configuration
rewrite {
    activeRecipe("org.openrewrite.java.migrate.UpgradeToJava21")
    activeRecipe("org.openrewrite.java.migrate.javax.AddJakartaNamespace")
    activeRecipe("org.openrewrite.java.spring.boot3.UpgradeSpringBoot_3_2")
}

