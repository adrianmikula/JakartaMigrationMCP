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
    
    // OpenRewrite for advanced source code scanning
    implementation("org.openrewrite:rewrite-java:8.10.0")
    implementation("org.openrewrite:rewrite-xml:8.10.0")
    
    // Lombok for logging
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")
}

// NOTE: This module is PROPRIETARY and not covered by Apache License 2.0

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}
