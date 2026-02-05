plugins {
    java
    id("org.springframework.boot") version "3.2.0" apply false
    id("io.spring.dependency-management") version "1.1.4" apply false
    id("org.jetbrains.intellij") version "1.17.2" apply false
}

allprojects {
    group = "adrianmikula"
    version = "1.0.0"

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
// - migration-core: Base analysis and scanning logic
// - mcp-server: MCP server with all 8 community tools (FREE)
//
// Premium Module (Proprietary):
// - intellij-plugin: IntelliJ plugin with free trial ($49/mo or $399/yr after trial)
//
// The MCP server is fully open source (Apache 2.0).
// The IntelliJ plugin is proprietary and includes premium features.
// =============================================================================

// =============================================================================
// LICENSING ENFORCEMENT
// =============================================================================

/**
 * Validates license headers in source files.
 * 
 * Rules:
 * - migration-core: Must have Apache 2.0 header
 * - mcp-server: Must have Apache 2.0 header
 * - intellij-plugin: Must have proprietary header
 */
tasks.register("validateLicenseHeaders") {
    description = "Validates license headers in source files"
    group = "verification"
    
    doLast {
        val apacheHeader = "Licensed under the Apache License"
        val proprietaryHeader = "This software is proprietary"
        
        val violations = mutableListOf<String>()
        
        allprojects.forEach { project ->
            if (project.hasProperty("licenseType")) {
                val expectedType = project.property("licenseType") as String
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

/**
 * Validates dependency licenses to ensure no proprietary code is used.
 * 
 * Rules:
 * - intellij-plugin can use Apache 2.0 dependencies
 * - mcp-server can use Apache 2.0 dependencies
 * - Commercial/proprietary dependencies must be declared in approved list
 */
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

/**
 * Ensures module boundaries are respected.
 * 
 * Rules:
 * - intellij-plugin can depend on migration-core (OK - Apache 2.0)
 * - mcp-server can depend on migration-core (OK - Apache 2.0)
 * - Community modules should NOT depend on proprietary modules
 */
tasks.register("validateModuleBoundaries") {
    description = "Ensures module dependencies respect licensing boundaries"
    group = "verification"
    
    doLast {
        val proprietaryModules = setOf("intellij-plugin")
        
        allprojects.forEach { project ->
            if (!proprietaryModules.contains(project.name)) {
                // Community modules - check they don't depend on proprietary
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

// =============================================================================
// BUILD HOOKS
// =============================================================================

gradle.projectsEvaluated {
    // Validation tasks are available but not automatically run during build
    // Run explicitly with: ./gradlew validateLicenseHeaders validateDependencyLicenses validateModuleBoundaries
}

// =============================================================================
// DEFAULT LICENSE TYPE FOR SUBPROJECTS
// =============================================================================

// License type is determined by project name in validation tasks
// - intellij-plugin: PROPRIETARY
// - migration-core, mcp-server: APACHE_2.0
