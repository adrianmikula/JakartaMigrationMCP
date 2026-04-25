plugins {
    `java-library`
    id("io.freefair.lombok") version "8.1.0"
}

dependencies {
    // Community Core Engine - local project dependency (Apache 2.0)
    // Contains base domain models and interfaces needed by premium features
    implementation(project(":community-core-engine"))

    // External dependencies
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.2")
    implementation("org.ow2.asm:asm:9.6") // For bytecode analysis
    implementation("com.google.guava:guava:32.1.3-jre") // Common utilities
    
    // Template engine - Thymeleaf for robust template processing
    implementation("org.thymeleaf:thymeleaf:3.1.2.RELEASE")
    
    // HTML-to-PDF generation using Flying Saucer (XhtmlRenderer)
    implementation("org.xhtmlrenderer:flying-saucer-pdf:9.1.22")
    implementation("com.itextpdf:itextpdf:5.5.13.3")
    // JSoup for HTML parsing and manipulation
    implementation("org.jsoup:jsoup:1.17.2")
    
    // JavaScript charting library for professional visualizations - commented out for now
    // implementation("org.webjars:chart.js:4.4.0")

    // HTTP client for Supabase API calls
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Test dependencies
    testImplementation(platform("org.junit:junit-bom:5.10.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("org.slf4j:slf4j-simple:2.0.9")
    testImplementation("org.mockito:mockito-core:5.7.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.7.0")
}

// NOTE: This module is PROPRIETARY and not covered by Apache License 2.0
// Contains premium features: refactoring, runtime verification, etc.

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        showStandardStreams = true
    }
    // Enable parallel test execution
    maxParallelForks = 4
    
    // Automatically set dev environment for all test executions
    systemProperty("jakarta.migration.mode", "dev")
}

// Fast test task - excludes slow tests
tasks.register<Test>("fastTest") {
    group = "verification"
    description = "Run fast unit tests only (excludes integration and slow tests)"
    
    useJUnitPlatform {
        excludeTags("slow")
    }
    
    testLogging {
        showStandardStreams = true
    }
    maxParallelForks = 4
    
    // Automatically set dev environment for all test executions
    systemProperty("jakarta.migration.mode", "dev")
}

// Slow test task - only integration/performance tests
tasks.register<Test>("slowTest") {
    group = "verification"
    description = "Run slow integration and performance tests only"
    
    useJUnitPlatform {
        includeTags("slow")
    }
    
    testLogging {
        showStandardStreams = true
    }
    maxParallelForks = 2  // Fewer forks for network-heavy tests
    
    // Automatically set dev environment for all test executions
    systemProperty("jakarta.migration.mode", "dev")
}

// Ultra-fast compilation test
tasks.register("compileCheck") {
    group = "verification"
    description = "Quick compilation check without running tests"
    
    dependsOn("compileJava", "compileTestJava")
    doLast {
        println("✅ Compilation successful - Ready for fast development!")
    }
}

// Core functionality tests - excludes slow tests
tasks.register<Test>("coreTest") {
    group = "verification"
    description = "Test core functionality (recipes, PDF, validation) - excludes slow tests"
    
    useJUnitPlatform {
        excludeTags("slow")
    }
    
    testLogging {
        showStandardStreams = true
    }
    maxParallelForks = 4
    
    // Automatically set dev environment for all test executions
    systemProperty("jakarta.migration.mode", "dev")
}

// Copy version from root gradle.properties to resources for analytics services
tasks.register<Copy>("copyVersionToResources") {
    group = "build"
    description = "Copy version from root gradle.properties to resources for analytics services"
    
    // Read version from root gradle.properties and create version.properties content
    val versionFile = rootProject.file("gradle.properties")
    val versionContent = versionFile.readLines().find { it.startsWith("version=") } ?: "version=unknown"
    val finalContent = "# Plugin Version - This file is used by UsageService and ErrorReportingService to get plugin version\n# Version is copied from root gradle.properties during build process\n$versionContent"
    
    // Write the content to version.properties
    doLast {
        file("src/main/resources/version.properties").writeText(finalContent)
    }
}

// Ensure version is copied before processing resources and compilation
tasks.processResources {
    dependsOn("copyVersionToResources")
}

tasks.compileJava {
    dependsOn("copyVersionToResources")
}
