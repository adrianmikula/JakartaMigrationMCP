plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    java
}

dependencies {
    implementation(project(":migration-core"))
    
    implementation("org.springframework.boot:spring-boot-starter")
    // Use official Spring AI coordinates as found in the latest documentation and milestone repos
    implementation("org.springframework.ai:spring-ai-starter-mcp-server:1.0.0-M5")
    
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")
    
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveFileName.set("jakarta-migration-mcp.jar")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}
