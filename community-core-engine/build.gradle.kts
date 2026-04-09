plugins {
    `java-library`
}

dependencies {
    implementation("org.slf4j:slf4j-api:2.0.9")
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")
    
    // YAML processing
    implementation("org.yaml:snakeyaml:2.2")
    
    // JSON processing
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.3")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.3")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.15.3")
    implementation("com.fasterxml.jackson.module:jackson-module-parameter-names:2.15.3")
    
    // SQLite database for persistence
    implementation("org.xerial:sqlite-jdbc:3.44.1.0")
    
    // Bytecode analysis
    api("org.ow2.asm:asm:9.6")
    api("org.ow2.asm:asm-commons:9.6")
    api("org.ow2.asm:asm-tree:9.6")

    // OpenRewrite for refactoring and scanning
    api("org.openrewrite:rewrite-core:8.10.0")
    api("org.openrewrite:rewrite-java:8.10.0")
    api("org.openrewrite:rewrite-maven:8.10.0")
    api("org.openrewrite:rewrite-xml:8.10.0")
    api("org.openrewrite.recipe:rewrite-migrate-java:2.5.0")
    runtimeOnly("org.openrewrite:rewrite-java-17:8.10.0")

    testImplementation(platform("org.junit:junit-bom:5.10.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.8.0")
    testImplementation("org.assertj:assertj-core:3.24.2")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.test {
    useJUnitPlatform {
        excludeTags("slow")
    }
    testLogging {
        showStandardStreams = true
    }
    // Enable parallel test execution
    maxParallelForks = 4
}

// Fast test task for quick agent feedback
tasks.register("fastTest") {
    group = "verification"
    description = "Run fast unit tests only (excludes integration and slow tests)"
    
    doLast {
        exec {
            workingDir = projectDir
            commandLine = listOf(
                "./gradlew", "test", "--tests", "*fast*",
                "--parallel", "--max-worker-count=4",
                "--configuration-cache", "--build-cache",
                "--no-daemon"
            )
        }
    }
}

// =============================================================================
// LICENSE ENFORCEMENT - Community modules must not depend on premium modules
// =============================================================================

val premiumModules = setOf(
    ":premium-core-engine",
    ":premium-mcp-server",
    ":premium-intellij-plugin"
)

afterEvaluate {
    configurations.forEach { configuration ->
        configuration.dependencies.forEach { dependency ->
            if (premiumModules.any { dependency.name.contains(it.removePrefix(":")) }) {
                throw GradleException(
                    "License Violation: ${project.name} (community) cannot depend on " +
                    "premium module '${dependency.name}'. " +
                    "Community modules must only use other community modules."
                )
            }
        }
    }
}
