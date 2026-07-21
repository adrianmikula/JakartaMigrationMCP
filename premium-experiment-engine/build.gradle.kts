plugins {
    `java-library`
}

group = "adrianmikula"
version = project.findProperty("version") ?: "1.0.0"

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }
    maven { url = uri("https://repo.spring.io/snapshot") }
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
}

dependencies {
    // JSON persistence
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.3")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.3")

    // Git operations (for apply safety)
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.9.0.202403050737-r")

    // Testcontainers (test-scoped only)
    testImplementation("org.testcontainers:testcontainers:1.19.8")
    testImplementation("org.testcontainers:junit-jupiter:1.19.8")

    // Testing
    testImplementation(platform("org.junit:junit-bom:5.10.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.assertj:assertj-core:3.24.2")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<Test> {
    useJUnitPlatform()
    maxParallelForks = 4
    testLogging {
        events("passed", "skipped", "failed")
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
}

tasks.register<Test>("fastTest") {
    group = "verification"
    description = "Run fast unit tests only (excludes integration and slow tests)"
    useJUnitPlatform {
        excludeTags("slow", "integration")
    }
    maxParallelForks = 4
}
