import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.io.File

plugins {
    id("org.jetbrains.intellij") version "1.17.2"
    `java-library`
    java
    jacoco
}
tasks.withType<JacocoReport> {
    dependsOn("test")
    
    // Include both original and instrumented classes for Jacoco report.
    // The IntelliJ plugin instruments classes for forms and @NotNull, 
    // and tests often use these instrumented classes.
    classDirectories.setFrom(
        files(
            "$buildDir/classes/java/main",
            "$buildDir/instrumented/instrumentCode"
        )
    )
    
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

dependencies {
    // Jackson for JSON serialization
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    
    // Community Core Engine - local project dependency (Apache 2.0)
    // Using 'api' to include classes in the final plugin JAR
    api(project(":community-core-engine"))

    // Premium Core Engine - local project dependency (Proprietary)
    // Contains premium features: refactoring, runtime verification, etc.
    implementation(project(":premium-core-engine"))

    // Premium Core Engine - runtime verification and premium features (Proprietary)
    implementation(project(":premium-core-engine"))

    // UI Testing dependencies
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")
    testImplementation("org.junit.platform:junit-platform-suite:1.10.0")
    testImplementation("org.junit.platform:junit-platform-launcher:1.10.0")
    testImplementation("org.assertj:assertj-core:3.24.2")
    testImplementation("org.mockito:mockito-core:5.5.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.5.0")
}

intellij {
    version = "2023.3.4"
    type = "IC"
    plugins = listOf("com.intellij.java")
}

tasks {
    // Write build timestamp to a properties file for runtime access
    val buildTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    val projectVersion = project.version.toString()
    
    // Create build info file using a simple file task
    val generateBuildInfo = register<DefaultTask>("generateBuildInfo") {
        description = "Generates build info properties file"
        group = "build"
        
        // Use inputs to make it compatible with configuration cache
        inputs.property("buildTimestamp", buildTimestamp)
        inputs.property("projectVersion", projectVersion)
        outputs.file(layout.buildDirectory.file("build-info.properties"))
        
        doLast {
            val buildInfoFile = outputs.files.singleFile
            buildInfoFile.writeText("""
                build.timestamp=$buildTimestamp
                build.version=$projectVersion
                build.java.version=${System.getProperty("java.version", "unknown")}
                build.java.vendor=${System.getProperty("java.vendor", "unknown")}
            """.trimIndent())
        }
    }

    // Disable buildSearchableOptions task to avoid JavaVersion.parse() failure with JDK 25
    buildSearchableOptions {
        onlyIf { false }
    }

    // Configure JUnit Jupiter for testing
    test {
        useJUnitPlatform()
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
        vendor.set(JvmVendorSpec.ADOPTIUM)
    }
    
    // Set source and target compatibility to match IntelliJ 2023.3.4 requirements
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

// Include community-core-engine classes in the plugin JAR
tasks.named<Jar>("jar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    
    // Ensure build-info.properties is generated before jar
    dependsOn("generateBuildInfo")
    
    // Include community-core-engine classes
    val communityCoreEngine = project(":community-core-engine")
    from(communityCoreEngine.sourceSets.main.get().output)
    
    // Also include premium-core-engine classes
    val premiumCoreEngine = project(":premium-core-engine")
    from(premiumCoreEngine.sourceSets.main.get().output)
    
    // Include build info from generateBuildInfo (now in build directory)
    from(layout.buildDirectory.file("build-info.properties"))
}

// Ensure instrumentedJar task depends on generateBuildInfo
tasks.withType<Jar> {
    if (name == "instrumentedJar") {
        dependsOn("generateBuildInfo")
    }
}

// Create a task to generate MCP tool definitions JSON
tasks.register("generateMcpToolsJson") {
    doLast {
        val toolsJson = File(project.buildDir, "mcp-tools.json")
        toolsJson.parentFile.mkdirs()
        toolsJson.writeText("""
            |{
            |  "server": {
            |    "name": "jakarta-migration-mcp",
            |    "version": "${project.version}",
            |    "description": "MCP server for Jakarta EE migration analysis and automation",
            |    "author": "Jakarta Migration Team",
            |    "vendor": "jakarta-migration.com"
            |  },
            |  "tools": [
            |    {
            |      "name": "analyzeJakartaReadiness",
            |      "description": "Analyzes a Java project's readiness for migration from Java EE 8 (javax.*) to Jakarta EE 9+ (jakarta.*)",
            |      "inputSchema": {
            |        "type": "object",
            |        "properties": {
            |          "projectPath": {
            |            "type": "string",
            |            "description": "Absolute path to the project root directory to analyze"
            |          },
            |          "includeTransitiveDependencies": {
            |            "type": "boolean",
            |            "description": "Whether to include transitive dependencies in the analysis"
            |          },
            |          "analysisLevel": {
            |            "type": "string",
            |            "enum": ["basic", "detailed", "comprehensive"]
            |          }
            |        },
            |        "required": ["projectPath"]
            |      }
            |    },
            |    {
            |      "name": "analyzeMigrationImpact",
            |      "description": "Provides detailed analysis of migration impact including affected dependencies, breaking changes, and estimated effort",
            |      "inputSchema": {
            |        "type": "object",
            |        "properties": {
            |          "projectPath": {
            |            "type": "string",
            |            "description": "Absolute path to the project root directory"
            |          },
            |          "scope": {
            |            "type": "string",
            |            "enum": ["dependencies", "code", "configuration", "all"]
            |          }
            |        },
            |        "required": ["projectPath"]
            |      }
            |    },
            |    {
            |      "name": "detectBlockers",
            |      "description": "Identifies migration blockers that prevent successful Jakarta EE migration",
            |      "inputSchema": {
            |        "type": "object",
            |        "properties": {
            |          "projectPath": {
            |            "type": "string",
            |            "description": "Absolute path to the project root directory"
            |          }
            |        },
            |        "required": ["projectPath"]
            |      }
            |    },
            |    {
            |      "name": "recommendVersions",
            |      "description": "Analyzes project dependencies and recommends compatible Jakarta EE versions",
            |      "inputSchema": {
            |        "type": "object",
            |        "properties": {
            |          "projectPath": {
            |            "type": "string",
            |            "description": "Absolute path to the project root directory"
            |          }
            |        },
            |        "required": ["projectPath"]
            |      }
            |    },
            |    {
            |      "name": "applyOpenRewriteRefactoring",
            |      "description": "Applies OpenRewrite refactoring recipes to automatically migrate javax packages to jakarta equivalents",
            |      "inputSchema": {
            |        "type": "object",
            |        "properties": {
            |          "projectPath": {
            |            "type": "string",
            |            "description": "Absolute path to the project root directory"
            |          },
            |          "filePatterns": {
            |            "type": "array",
            |            "description": "Glob patterns for files to refactor"
            |          },
            |          "dryRun": {
            |            "type": "boolean",
            |            "description": "If true, only preview changes without applying them"
            |          }
            |        },
            |        "required": ["projectPath", "filePatterns"]
            |      }
            |    },
            |    {
            |      "name": "scanBinaryDependency",
            |      "description": "Scans a compiled JAR dependency for Jakarta EE compatibility issues",
            |      "inputSchema": {
            |        "type": "object",
            |        "properties": {
            |          "jarPath": {
            |            "type": "string",
            |            "description": "Absolute path to the JAR file to scan"
            |          }
            |        },
            |        "required": ["jarPath"]
            |      }
            |    },
            |    {
            |      "name": "updateDependency",
            |      "description": "Updates a single dependency to a recommended Jakarta-compatible version",
            |      "inputSchema": {
            |        "type": "object",
            |        "properties": {
            |          "projectPath": {
            |            "type": "string",
            |            "description": "Absolute path to the project root directory"
            |          },
            |          "groupId": {
            |            "type": "string",
            |            "description": "Maven group ID of the dependency"
            |          },
            |          "artifactId": {
            |            "type": "string",
            |            "description": "Maven artifact ID of the dependency"
            |          },
            |          "currentVersion": {
            |            "type": "string",
            |            "description": "Current version of the dependency"
            |          },
            |          "recommendedVersion": {
            |            "type": "string",
            |            "description": "Version to upgrade to"
            |          }
            |        },
            |        "required": ["projectPath", "groupId", "artifactId", "currentVersion", "recommendedVersion"]
            |      }
            |    },
            |    {
            |      "name": "generateMigrationPlan",
            |      "description": "Generates a detailed, phased migration plan for Jakarta EE migration",
            |      "inputSchema": {
            |        "type": "object",
            |        "properties": {
            |          "projectPath": {
            |            "type": "string",
            |            "description": "Absolute path to the project root directory"
            |          }
            |        },
            |        "required": ["projectPath"]
            |      }
            |    },
            |    {
            |      "name": "validateMigration",
            |      "description": "Validates that migration was successful by running compile checks and test suites",
            |      "inputSchema": {
            |        "type": "object",
            |        "properties": {
            |          "projectPath": {
            |            "type": "string",
            |            "description": "Absolute path to the project root directory"
            |          }
            |        },
            |        "required": ["projectPath"]
            |      }
            |    }
            |  ]
            |}
        """.trimMargin())
    }
}

/**
 * Build development plugin: clean, rebuild all modules, and run IDE.
 * Force rebuilds community-core-engine and premium-core-engine to ensure fresh code.
 * 
 * Usage: ./gradlew :premium-intellij-plugin:buildDevPlugin
 */
tasks.register("buildDevPlugin") {
    group = "build"
    description = "Clean, rebuild all modules, and run IDE for development"
    
    // Clean all modules to ensure fresh rebuild
    dependsOn(":community-core-engine:clean", ":premium-core-engine:clean", "clean")
    // Build and run
    dependsOn("jar", "runIde")
    
    doLast {
        println("\n=== Distribution Build Complete ===")
        println("Distribution files in build/distributions:")
    }
}
