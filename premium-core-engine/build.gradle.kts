plugins {
    `java-library`
}

dependencies {
    // Community Core Engine (Apache 2.0) - base functionality
    implementation(project(":community-core-engine"))

    // External dependencies
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("org.ow2.asm:asm:9.6")
    implementation("org.ow2.asm:asm-commons:9.6")
}

// NOTE: This module is PROPRIETARY and not covered by Apache License 2.0

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
