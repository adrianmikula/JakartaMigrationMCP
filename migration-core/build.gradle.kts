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
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}
