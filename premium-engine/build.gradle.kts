plugins {
    `java-library`
}

dependencies {
    // Internal dependencies (proprietary)
    implementation(project(":migration-core"))
    implementation(project(":mcp-server"))

    // External dependencies (must be Apache 2.0 compatible)
    implementation("org.slf4j:slf4j-api:2.0.9")
}

// NOTE: This module is PROPRIETARY and not covered by Apache License 2.0
// Only included when building with -PpremiumEnabled=true

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
