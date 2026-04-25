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

// Custom task to run integration tests
tasks.register("runIntegrationTests") {
    group = "verification"
    description = "Runs integration tests to verify Maven Central lookup functionality"
    
    dependsOn("compileTestJava")
    
    doLast {
        javaexec {
            classpath = sourceSets.test.get().runtimeClasspath
            mainClass = "org.junit.platform.console.ConsoleLauncher"
            args = listOf(
                "--details=verbose",
                "--select-class=adrianmikula.jakartamigration.intellij.service.MavenCentralServiceIntegrationTest"
            )
        }
    }
}

// Custom task to run marketplace validation tests
tasks.register("validateMarketplaceRequirements") {
    group = "verification"
    description = "Validates plugin.xml meets JetBrains Marketplace requirements for paid plugins"
    
    dependsOn("compileTestJava")
    
    doLast {
        javaexec {
            classpath = sourceSets.test.get().runtimeClasspath
            mainClass = "org.junit.platform.console.ConsoleLauncher"
            args = listOf(
                "--details=verbose",
                "--select-class=adrianmikula.jakartamigration.intellij.PluginMarketplaceValidationTest"
            )
        }
    }
}

// Custom task to run build validation tests
tasks.register("validateBuildConfiguration") {
    group = "verification"
    description = "Validates build configuration and dependencies"
    
    dependsOn("compileTestJava")
    
    doLast {
        javaexec {
            classpath = sourceSets.test.get().runtimeClasspath
            mainClass = "org.junit.platform.console.ConsoleLauncher"
            args = listOf(
                "--details=verbose",
                "--select-class=adrianmikula.jakartamigration.intellij.BuildConfigurationValidationTest"
            )
        }
    }
}

// Fix task dependency issue with classpathIndexCleanup
tasks.named("classpathIndexCleanup") {
    mustRunAfter("compileTestJava")
}

// Custom task to run fast tests only
tasks.register<JavaExec>("runFastTests") {
    group = "verification"
    description = "Runs fast subset of tests for quick feedback"
    
    dependsOn("compileTestJava")
    
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass = "org.junit.platform.console.ConsoleLauncher"
    args = listOf(
        "--details=summary",
        "--include-tag=fast"
    )
}

dependencies {
    // Jackson for JSON serialization
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    
    // SLF4J logging implementation
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("org.slf4j:slf4j-simple:2.0.9")
    
    // Community Core Engine - local project dependency (Apache 2.0)
    // Using 'api' to include classes in the final plugin JAR
    api(project(":community-core-engine"))
    
    // Premium Core Engine - local project dependency (Proprietary)
    // Contains premium features: refactoring, runtime verification, etc.
    // Using 'api' to include classes in the final plugin JAR
    api(project(":premium-core-engine"))
    
    // UI Testing dependencies
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")
    testImplementation("org.junit.platform:junit-platform-suite:1.10.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.0")
    testImplementation("org.assertj:assertj-core:3.24.2")
    
    // Kotest for property testing
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
    testImplementation("io.kotest:kotest-property:5.8.0")
    
    testImplementation("org.mockito:mockito-core:5.5.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.5.0")
}

intellij {
    version = "2023.3.4"
    type = "IC"
    plugins = listOf("com.intellij.java")
}

// Exclude IDE packages that should be provided by the IntelliJ platform
tasks.named<org.jetbrains.intellij.tasks.PrepareSandboxTask>("prepareSandbox") {
    exclude { entry ->
        // Exclude org.jetbrains.concurrency package to prevent bundling
        // This package is provided by IntelliJ platform and bundling it causes compatibility issues
        entry.name.contains("org/jetbrains/concurrency/") ||
        entry.name.contains("org/jetbrains/util/")
    }
}

tasks {
    patchPluginXml {
        sinceBuild.set(providers.gradleProperty("intellij.sinceBuild").orElse("233"))
        untilBuild.set(providers.gradleProperty("intellij.untilBuild").orElse(""))
    }
    
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
    
    // Disable problematic tasks that cause connectivity issues
    tasks {
        named("initializeIntelliJPlugin") {
            enabled = false
        }
    }

    // Configure JUnit Jupiter for testing
    test {
        useJUnitPlatform()

        // Exclude UI tests that require full IntelliJ Platform environment
        exclude("**/ui/UI*Tests.class")
        exclude("**/ui/UI*TestSuite.class")
        exclude("**/ui/ComprehensiveJakartaLookupTest.class")
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
    from(communityCoreEngine.sourceSets.main.get().output) {
        // Exclude IDE-provided packages that should not be bundled
        exclude("org/jetbrains/concurrency/**")
        exclude("org/jetbrains/util/**")
    }
    
    // Also include premium-core-engine classes
    val premiumCoreEngine = project(":premium-core-engine")
    from(premiumCoreEngine.sourceSets.main.get().output) {
        // Exclude IDE-provided packages that should not be bundled
        exclude("org/jetbrains/concurrency/**")
        exclude("org/jetbrains/util/**")
    }
    
    // Exclude IDE-provided packages from the main source set as well
    from(sourceSets.main.get().output) {
        exclude("org/jetbrains/concurrency/**")
        exclude("org/jetbrains/util/**")
    }
    
    // Include build info from generateBuildInfo (now in build directory)
    from(layout.buildDirectory.file("build-info.properties"))
}

// Ensure instrumentedJar task depends on generateBuildInfo and excludes IDE packages
tasks.withType<Jar> {
    if (name == "instrumentedJar") {
        dependsOn("generateBuildInfo")
        
        // Exclude IDE-provided packages from instrumented JAR
        exclude("org/jetbrains/concurrency/**")
        exclude("org/jetbrains/util/**")
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
 * Build development plugin: clean, rebuild all modules, and run IDE for development
 * 
 * Usage: ./gradlew :premium-intellij-plugin:buildDevPlugin
 */
tasks.register("buildDevPlugin") {
    group = "build"
    description = "Clean, rebuild all modules, and run IDE for development (dev mode - skips licensing)"
    
    // Clean all modules to ensure fresh rebuild
    dependsOn(":community-core-engine:clean", ":premium-core-engine:clean", "clean")
    
    // Set development environment
    doLast {        
        project.ext.set("environment", "dev")
        println("\n=== Building in DEV MODE (skipping all licensing checks) ===")
        
        // Build and run
        dependsOn(tasks.named<Jar>("jar").get(), tasks.named("runIdeDev").get())
        
        println("\n=== Development Build Complete ===")
        println("Plugin built with development configuration (no licensing checks)")
    }
}

/**
 * Disable product descriptor for development (prevents license dialog)
 * 
 * Usage: ./gradlew :premium-intellij-plugin:disableProductDescriptor --no-configuration-cache
 */
tasks.register<DefaultTask>("disableProductDescriptor") {
    group = "build"
    description = "Disable product descriptor to prevent license dialog during development"
    
    doLast {
        val pluginXml = file("src/main/resources/META-INF/plugin.xml")
        if (!pluginXml.exists()) {
            println(" plugin.xml not found at ${pluginXml.absolutePath}")
            return@doLast
        }
        
        val content = pluginXml.readText()
        
        if (content.contains("<!-- <product-descriptor")) {
            println(" Product descriptor is already disabled for development")
        } else {
            // Comment out the product descriptor
            val updatedContent = content.replace(
                "<product-descriptor code=\"PJAKARTAMIGRATI\" release-date=\"20250326\" release-version=\"108\"/>",
                "<!-- <product-descriptor code=\"PJAKARTAMIGRATI\" release-date=\"20250326\" release-version=\"108\"/> -->"
            )
            
            pluginXml.writeText(updatedContent)
            println(" Product descriptor disabled - no license dialog during development")
        }
    }
}

/**
 * Enable product descriptor for production
 * 
 * Usage: ./gradlew :premium-intellij-plugin:enableProductDescriptor --no-configuration-cache
 */
tasks.register<DefaultTask>("enableProductDescriptor") {
    group = "build"
    description = "Enable product descriptor for production builds"
    
    doLast {
        val pluginXml = file("src/main/resources/META-INF/plugin.xml")
        if (!pluginXml.exists()) {
            println(" plugin.xml not found at ${pluginXml.absolutePath}")
            return@doLast
        }
        
        val content = pluginXml.readText()
        
        if (content.contains("<product-descriptor code=\"PJAKARTAMIGRATI\"") && !content.contains("<!-- <product-descriptor")) {
            println(" Product descriptor is already enabled for production")
        } else {
            // Uncomment the product descriptor
            val updatedContent = content.replace(
                "<!-- <product-descriptor code=\"PJAKARTAMIGRATI\" release-date=\"20250326\" release-version=\"108\"/> -->",
                "<product-descriptor code=\"PJAKARTAMIGRATI\" release-date=\"20250326\" release-version=\"108\"/>"
            )
            
            pluginXml.writeText(updatedContent)
            println(" Product descriptor enabled - ready for production")
        }
    }
}

/**
 * Run IDE in development mode (skips licensing, enables dev tab with premium simulation)
 * 
 * Usage: ./gradlew :premium-intellij-plugin:runIdeDev
 */
tasks.register<org.jetbrains.intellij.tasks.RunIdeTask>("runIdeDev") {
    group = "build"
    description = "Run IDE in development mode (dev - skips all licensing checks, enables dev tab)"
    
    // Set development environment system properties
    systemProperty("jakarta.migration.mode", "dev")
    // Note: Premium simulation is controlled via the Dev tab UI, not set by default
    // Users can enable it via the Dev tab checkbox during development
    
    doFirst {
        project.ext.set("environment", "dev")
        println("\n=== Running IDE in DEV MODE (skipping all licensing checks) ===")
        println("Dev tab will be available with premium simulation settings")
    }
}

/**
 * Run IDE in demo marketplace mode
 * 
 * Usage: ./gradlew :premium-intellij-plugin:runIdeDemo
 */
tasks.register<org.jetbrains.intellij.tasks.RunIdeTask>("runIdeDemo") {
    group = "build"
    description = "Run IDE in demo marketplace mode (demo - uses JetBrains Demo Marketplace)"
    
    // Set demo environment system properties
    systemProperty("jakarta.migration.mode", "demo")
    
    doFirst {
        project.ext.set("environment", "demo")
        println("\n=== Running IDE in DEMO MODE (JetBrains Demo Marketplace) ===")
        println("NOTE: Make sure product descriptor is enabled in plugin.xml")
        println("Run: .\\fix-license-dialog.bat enable if needed")
    }
}

/**
 * Run IDE in development mode with premium simulation enabled by default
 * 
 * Usage: ./gradlew :premium-intellij-plugin:runIdeDevPremium
 */
tasks.register<org.jetbrains.intellij.tasks.RunIdeTask>("runIdeDevPremium") {
    group = "build"
    description = "Run IDE in development mode with premium simulation enabled"
    
    // Set development environment and premium simulation system properties
    systemProperty("jakarta.migration.mode", "dev")
    systemProperty("jakarta.migration.dev.simulate_premium", "true")
    
    doFirst {
        project.ext.set("environment", "dev")
        println("\n=== Running IDE in DEV MODE with PREMIUM SIMULATION ===")
        println("Dev tab will be available with premium simulation ENABLED by default")
    }
}

/**
 * Run IDE in production marketplace mode
 * 
 * Usage: ./gradlew :premium-intellij-plugin:runIdeProd
 */
tasks.register<org.jetbrains.intellij.tasks.RunIdeTask>("runIdeProd") {
    group = "build"
    description = "Run IDE in production marketplace mode (production - uses JetBrains Production Marketplace)"
    
    // Enable product descriptor for production marketplace
    dependsOn("enableProductDescriptor")
    
    // Set production environment system properties
    systemProperty("jakarta.migration.mode", "production")
    
    doFirst {
        project.ext.set("environment", "production")
        println("\n=== Running IDE in PRODUCTION MODE (JetBrains Production Marketplace) ===")
    }
}

/**
 * Build production plugin: clean, rebuild all modules, and run IDE for production marketplace (default)
 * 
 * Usage: ./gradlew :premium-intellij-plugin:buildProductionPlugin
 */
tasks.register("buildProductionPlugin") {
    group = "build"
    description = "Clean, rebuild all modules, and run IDE for production marketplace (default)"
    
    // Clean all modules to ensure fresh rebuild
    dependsOn(":community-core-engine:clean", ":premium-core-engine:clean", "clean")
    
    // Validate marketplace requirements before building
    dependsOn("validateMarketplaceRequirements")
    
    // Set production environment
    doLast {
        project.ext.set("environment", "production")
        println("\n=== Building in PRODUCTION MODE (JetBrains Production Marketplace) ===")
        
        // Build and run
        dependsOn("jar", "runIde")
        
        println("\n=== Production Build Complete ===")
        println("Plugin built with production marketplace configuration")
    }
}
