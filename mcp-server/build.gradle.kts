plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    java
}

dependencies {
    implementation(project(":migration-core"))
    
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    // Use working configuration from commit c8972f1
    implementation("org.springframework.ai:spring-ai-starter-mcp-server-webmvc:1.1.2")
    implementation("org.springaicommunity:mcp-annotations:0.8.0")
    
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")
    
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("jakarta-migration-mcp.jar")
    manifest {
        attributes["Implementation-Title"] = "Jakarta Migration MCP Server"
        attributes["Implementation-Version"] = project.version
        attributes["Built-By"] = System.getProperty("user.name")
        attributes["Main-Class"] = "adrianmikula.jakartamigration.mcp.McpServerApplication"
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

// =============================================================================
// BUILD CACHE COMPATIBILITY
// =============================================================================

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

// Configure BootJar for better caching
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    // Exclude unnecessary files for smaller JAR
    exclude("*.jar")
    exclude("META-INF/INDEX.LIST")
    exclude("META-INF/*.SF")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.RSA")
}

// Configure Jar task for library JAR
tasks.named<Jar>("jar") {
    archiveFileName.set("jakarta-migration-mcp-lib.jar")
    manifest {
        attributes["Implementation-Title"] = "Jakarta Migration MCP Library"
        attributes["Implementation-Version"] = project.version
    }
}
