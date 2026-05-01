plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    java
}

dependencies {
    implementation(project(":community-core-engine"))
    
    implementation("org.springframework.boot:spring-boot-starter-web")
    // Use working configuration from commit c8972f1
    implementation("org.springframework.ai:spring-ai-starter-mcp-server-webmvc:1.1.2")
    implementation("org.springaicommunity:mcp-annotations:0.8.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    
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

tasks.test {
    useJUnitPlatform()
    testLogging {
        showStandardStreams = true
    }
    // Enable parallel test execution
    maxParallelForks = 4
}

// Fast test task for quick feedback
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
}
