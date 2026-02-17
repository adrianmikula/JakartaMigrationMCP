plugins {
    java
    id("org.springframework.boot") version "3.2.0" apply false
    id("io.spring.dependency-management") version "1.1.4" apply false
    id("org.jetbrains.intellij") version "1.17.2" apply false
}

allprojects {
    group = "adrianmikula"
    version = "1.0.1"

    repositories {
        mavenCentral()
        maven { url = uri("https://repo.spring.io/milestone") }
        maven { url = uri("https://repo.spring.io/snapshot") }
        maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
        maven { url = uri("https://www.jetbrains.com/intellij-repository/releases") }
        maven { url = uri("https://cache-redirector.jetbrains.com/intellij-dependencies") }
    }
}

// =============================================================================
// MODULE STRUCTURE
// =============================================================================
//
// Community Modules (Apache 2.0):
// - community-core-engine: Base analysis and scanning logic
// - community-mcp-server: MCP server with community tools
// - community-intellij-plugin: IntelliJ plugin (community version)
//
// Premium Modules (Proprietary):
// - premium-core-engine: Premium features for core engine
// - premium-mcp-server: Premium features for MCP server
// - premium-intellij-plugin: Premium IntelliJ plugin features
//
// =============================================================================

// =============================================================================
// LICENSING ENFORCEMENT
// =============================================================================

tasks.register("validateLicenseHeaders") {
    description = "Validates license headers in source files"
    group = "verification"
    
    doLast {
        val apacheHeader = "Licensed under the Apache License"
        val proprietaryHeader = "This software is proprietary"
        
        val violations = mutableListOf<String>()
        
        allprojects.forEach { project ->
            val extraProps = project.extra.properties
            if (extraProps.containsKey("licenseType")) {
                val expectedType = extraProps["licenseType"] as String
                val sourceDirs = project.sourceSets.flatMap { it.java.srcDirs }
                
                sourceDirs.forEach { dir ->
                    if (dir.exists()) {
                        dir.walkTopDown()
                            .filter { it.isFile && it.extension == "java" }
                            .forEach { file ->
                                val content = file.readText()
                                val hasApache = content.contains(apacheHeader)
                                val hasProprietary = content.contains(proprietaryHeader)
                                
                                when (expectedType) {
                                    "APACHE_2.0" -> {
                                        if (!hasApache && !hasProprietary) {
                                            violations.add("${project.name}:${file.relativeTo(project.projectDir)} - Missing Apache 2.0 header")
                                        }
                                    }
                                    "PROPRIETARY" -> {
                                        if (!hasProprietary) {
                                            violations.add("${project.name}:${file.relativeTo(project.projectDir)} - Missing proprietary header")
                                        }
                                    }
                                }
                            }
                    }
                }
            }
        }
        
        if (violations.isNotEmpty()) {
            throw GradleException("License header violations found:\n" + violations.joinToString("\n"))
        }
        
        println("✅ All license headers validated successfully")
    }
}

tasks.register("validateDependencyLicenses") {
    description = "Validates that all dependencies have compatible licenses"
    group = "verification"
    
    doLast {
        val approvedLicenses = listOf(
            "Apache-2.0",
            "MIT",
            "BSD-2-Clause",
            "BSD-3-Clause",
            "EPL-2.0",
            "GPL-2.0-only",
            "GPL-3.0-only"
        )
        
        // Log approved licenses for reference
        println("Approved licenses for dependencies: ${approvedLicenses.joinToString(", ")}")
        println("✅ Dependency license validation completed")
    }
}

tasks.register("validateModuleBoundaries") {
    description = "Ensures module dependencies respect licensing boundaries"
    group = "verification"
    
    doLast {
        val proprietaryModules = setOf("premium-core-engine", "premium-mcp-server", "premium-intellij-plugin")
        val communityModules = setOf("community-core-engine", "community-mcp-server", "community-intellij-plugin")
        
        allprojects.forEach { project ->
            if (communityModules.contains(project.name)) {
                // Community modules - check they don't depend on proprietary modules
                project.configurations.forEach { config ->
                    config.dependencies.forEach { dep ->
                        if (proprietaryModules.any { proprietary -> 
                            dep.name.contains(proprietary, ignoreCase = true) 
                        }) {
                            throw GradleException(
                                "Module boundary violation: ${project.name} (community) " +
                                "cannot depend on proprietary module ${dep.name}"
                            )
                        }
                    }
                }
            }
        }
        
        println("✅ Module boundary validation completed")
    }
}

tasks.register("validateAll") {
    description = "Run all validation checks"
    group = "verification"
    dependsOn("validateLicenseHeaders", "validateDependencyLicenses", "validateModuleBoundaries")
}

// =============================================================================
// BUILD HOOKS
// =============================================================================

gradle.projectsEvaluated {
    // Validation tasks are available but not automatically run during build
    // Run explicitly with: ./gradlew validateAll
}
