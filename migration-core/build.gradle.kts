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
    
    // SQLite database for persistence
    implementation("org.xerial:sqlite-jdbc:3.44.1.0")
    
    // Bytecode analysis
    api("org.ow2.asm:asm:9.6")
    api("org.ow2.asm:asm-commons:9.6")
    api("org.ow2.asm:asm-tree:9.6")

    // OpenRewrite for refactoring and scanning
    api("org.openrewrite:rewrite-java:8.10.0")
    api("org.openrewrite:rewrite-maven:8.10.0")
    api("org.openrewrite:rewrite-xml:8.10.0")

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

// =============================================================================
// BUILD CACHE COMPATIBILITY
// =============================================================================

// Configure Jar task for better caching
tasks.named<Jar>("jar") {
    archiveFileName.set("migration-core.jar")
    manifest {
        attributes["Implementation-Title"] = "Jakarta Migration Core"
        attributes["Implementation-Version"] = project.version
        attributes["Built-By"] = System.getProperty("user.name")
    }
}

// Configure test task with caching
tasks.named<Test>("test") {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
    // Enable test caching
    outputs.upToDateWhen { false }
}

// Configure JavaCompile for incremental compilation
tasks.named<JavaCompile>("compileJava") {
    options.isIncremental = true
    options.compilerArgs.addAll(listOf(
        "-parameters",
        "-Xlint:deprecation",
        "-Xlint:unchecked"
    ))
}

tasks.named<JavaCompile>("compileTestJava") {
    options.isIncremental = true
}
