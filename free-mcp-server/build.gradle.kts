plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    java
}

dependencies {
    implementation(project(":free-core-engine"))
    
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
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}
