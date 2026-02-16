plugins {
    `java-library`
    id("io.freefair.lombok") version "8.1.0"
}

dependencies {
    // Community Core Engine - local project dependency (Apache 2.0)
    // Contains base domain models and interfaces needed by premium features
    implementation(project(":community-core-engine"))

    // External dependencies (must be Apache 2.0 compatible)
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("org.ow2.asm:asm:9.6") // For bytecode analysis
    implementation("com.google.guava:guava:32.1.3-jre") // Common utilities
}

// NOTE: This module is PROPRIETARY and not covered by Apache License 2.0
// Contains premium features: refactoring, runtime verification, etc.

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
