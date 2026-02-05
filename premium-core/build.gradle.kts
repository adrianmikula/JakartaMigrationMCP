plugins {
    `java-library`
}

/*
 * PREMIUM MODULE - PROPRIETARY LICENSE
 * 
 * This module contains premium features that are not part of the open core.
 * Features in this module require a commercial license.
 * 
 * License: Proprietary - All rights reserved
 */

group = "adrianmikula"
version = "1.0.0"

// License type for validation - must be defined as extra property for root project access
extra["licenseType"] = "PROPRIETARY"

dependencies {
    // Free community core dependencies
    implementation(project(":migration-core"))
    
    // Lombok for boilerplate reduction
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")
    
    // SLF4J for logging (used by lombok @Slf4j)
    compileOnly("org.slf4j:slf4j-simple:2.0.11")
    
    // ASM for bytecode analysis (included in community, but used by premium)
    api("org.ow2.asm:asm:9.6")
    api("org.ow2.asm:asm-commons:9.6")
    api("org.ow2.asm:asm-tree:9.6")

    // OpenRewrite for refactoring
    api("org.openrewrite:rewrite-java:8.10.0")
    api("org.openrewrite:rewrite-maven:8.10.0")
    api("org.openrewrite:rewrite-xml:8.10.0")

    // Testing dependencies
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

// Configure Jar task
tasks.named<Jar>("jar") {
    archiveFileName.set("premium-core.jar")
    manifest {
        attributes["Implementation-Title"] = "Jakarta Migration Premium Core"
        attributes["Implementation-Version"] = project.version
        attributes["License"] = "Proprietary - All rights reserved"
        attributes["Author"] = "Jakarta Migration Team"
    }
}

// Configure test task
tasks.named<Test>("test") {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

// Incremental compilation
tasks.named<JavaCompile>("compileJava") {
    options.isIncremental = true
}

tasks.named<JavaCompile>("compileTestJava") {
    options.isIncremental = true
}
