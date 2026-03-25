plugins {
    `java-library`
}

dependencies {
    // Internal dependencies (proprietary)
    implementation(project(":community-core-engine"))
    implementation(project(":premium-core-engine"))
    implementation(project(":community-mcp-server"))

    // External dependencies (must be Apache 2.0 compatible)
    implementation("org.slf4j:slf4j-api:2.0.9")
    
    // Test dependencies
    testImplementation(platform("org.junit:junit-bom:5.10.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("org.mockito:mockito-core:5.7.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.7.0")
}

// NOTE: This module is PROPRIETARY and not covered by Apache License 2.0
// Only included when building with -PpremiumEnabled=true

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        showStandardStreams = true
    }
    // Enable parallel test execution
    maxParallelForks = 4
}
