plugins {
    java
    id("org.springframework.boot") version "3.4.3"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.openrewrite.rewrite") version "7.0.0"
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
    if (rootProject != project) {
        // Multi-project build (run from repo root)
        implementation(project(":"))
    } else {
        // Standalone build (run from this subdirectory) - resolved via composite build in settings.gradle.kts
        implementation("adrianmikula:jakarta-migration-mcp")
    }

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

// JaCoCo Configuration (aligned with root: exclusions + per-class 50% check)
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
    classDirectories.setFrom(
        sourceSets.main.get().output.classesDirs.files.map {
            fileTree(it) {
                exclude(
                    "**/config/**",
                    "**/entity/**",
                    "**/dto/**",
                    "**/*Application.class",
                    "**/*Config.class",
                    "**/projectname/**"
                )
            }
        }
    )
    sourceDirectories.setFrom(sourceSets.main.get().allSource.srcDirs)
}

// Per-class 50% coverage check (same exclusions as root)
tasks.register("jacocoPerClassCoverageCheck") {
    description = "Verify that each class has at least 50% code coverage"
    dependsOn(tasks.named("jacocoTestReport"))

    doLast {
        val xmlReport = tasks.named<org.gradle.testing.jacoco.tasks.JacocoReport>("jacocoTestReport").get().reports.xml.outputLocation.get().asFile

        if (!xmlReport.exists()) {
            throw GradleException("Coverage XML report not found at: ${xmlReport.absolutePath}")
        }

        val xml = javax.xml.parsers.DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false)
        }.newDocumentBuilder().parse(xmlReport)

        val packageNodes = xml.getElementsByTagName("package")
        val classesBelowThreshold = mutableListOf<Pair<String, Double>>()
        val excludedPatterns = listOf(
            "config", "entity", "dto", "Application", "Config", "projectname",
            "Exception", "Controller", "PatternMatcher", "\$" // exceptions, MCP transport controllers, matchers, inner classes
        )

        for (i in 0 until packageNodes.length) {
            val packageNode = packageNodes.item(i) as org.w3c.dom.Element
            val packageName = packageNode.getAttribute("name")

            if (excludedPatterns.any { packageName.contains(it, ignoreCase = true) }) {
                continue
            }

            val classNodes = packageNode.getElementsByTagName("class")
            for (j in 0 until classNodes.length) {
                val classNode = classNodes.item(j) as org.w3c.dom.Element
                val className = classNode.getAttribute("name")
                val fullClassName = if (packageName.isNotEmpty()) "$packageName.$className" else className

                if (excludedPatterns.any { className.contains(it, ignoreCase = true) }) {
                    continue
                }

                val counters = classNode.getElementsByTagName("counter")
                var instructionCounter: org.w3c.dom.Element? = null

                for (k in 0 until counters.length) {
                    val counter = counters.item(k) as org.w3c.dom.Element
                    if (counter.getAttribute("type") == "INSTRUCTION") {
                        instructionCounter = counter
                        break
                    }
                }

                val counterElement = instructionCounter
                if (counterElement != null) {
                    val missed = counterElement.getAttribute("missed").toIntOrNull() ?: 0
                    val covered = counterElement.getAttribute("covered").toIntOrNull() ?: 0
                    val total = missed + covered

                    if (total > 0) {
                        val coverage = (covered.toDouble() / total) * 100
                        if (coverage < 50.0) {
                            classesBelowThreshold.add(Pair(fullClassName, coverage))
                        }
                    }
                }
            }
        }

        if (classesBelowThreshold.isNotEmpty()) {
            println("\n❌ Code Coverage Check Failed!")
            println("The following classes have coverage below 50%:")
            println("=".repeat(80))
            classesBelowThreshold.sortedBy { it.second }.forEach { (className, coverage) ->
                println("  $className: ${String.format("%.2f", coverage)}%")
            }
            println("=".repeat(80))
            throw GradleException(
                "Code coverage check failed: ${classesBelowThreshold.size} class(es) have coverage below 50%"
            )
        } else {
            println("\n✅ Code Coverage Check Passed!")
            println("All classes meet the 50% minimum coverage requirement.")
        }
    }
}

tasks.jacocoTestReport {
    finalizedBy(tasks.named("jacocoPerClassCoverageCheck"))
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

