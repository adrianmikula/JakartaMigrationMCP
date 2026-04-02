package adrianmikula.jakartamigration.licensing

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldContain
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.shouldThrow
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Kotest tests to enforce licensing model compliance.
 * Prevents premium modules from being accidentally included in community modules
 * and ensures proper module boundary enforcement.
 */
class LicensingModelEnforcementTest : DescribeSpec({

    describe("Licensing Model Enforcement") {
        
        it("should prevent community modules from depending on premium modules") {
            val communityModules = mapOf(
                "community-core-engine" to Paths.get("community-core-engine/build.gradle.kts"),
                "community-mcp-server" to Paths.get("community-mcp-server/build.gradle.kts"),
                "community-intellij-plugin" to Paths.get("community-intellij-plugin/build.gradle.kts")
            )
            
            val premiumModules = setOf(
                "premium-core-engine",
                "premium-mcp-server", 
                "premium-intellij-plugin"
            )
            
            communityModules.forEach { (moduleName, buildFile) ->
                if (Files.exists(buildFile)) {
                    val buildContent = Files.readString(buildFile)
                    
                    // Check for any dependency on premium modules
                    premiumModules.forEach { premiumModule ->
                        buildContent shouldNotContain("implementation(project(\":$premiumModule\"))") {
                            "Community module '$moduleName' should not depend on premium module '$premiumModule'"
                        }
                        buildContent shouldNotContain("api(project(\":$premiumModule\"))") {
                            "Community module '$moduleName' should not have API dependency on premium module '$premiumModule'"
                        }
                        buildContent shouldNotContain("compileOnly(project(\":$premiumModule\"))") {
                            "Community module '$moduleName' should not have compile-only dependency on premium module '$premiumModule'"
                        }
                        buildContent shouldNotContain("runtimeOnly(project(\":$premiumModule\"))") {
                            "Community module '$moduleName' should not have runtime-only dependency on premium module '$premiumModule'"
                        }
                        buildContent shouldNotContain("implementation(project(\":$premiumModule\"))") {
                            "Community module '$moduleName' should not have implementation dependency on premium module '$premiumModule'"
                        }
                        
                        // Also check for direct module references
                        buildContent shouldNotContain(":$premiumModule") {
                            "Community module '$moduleName' should not reference premium module '$premiumModule'"
                        }
                    }
                }
            }
        }
        
        it("should allow premium modules to depend on community modules") {
            val premiumModules = mapOf(
                "premium-core-engine" to Paths.get("premium-core-engine/build.gradle.kts"),
                "premium-mcp-server" to Paths.get("premium-mcp-server/build.gradle.kts"),
                "premium-intellij-plugin" to Paths.get("premium-intellij-plugin/build.gradle.kts")
            )
            
            val communityModules = setOf(
                "community-core-engine",
                "community-mcp-server",
                "community-intellij-plugin"
            )
            
            premiumModules.forEach { (moduleName, buildFile) ->
                if (Files.exists(buildFile)) {
                    val buildContent = Files.readString(buildFile)
                    
                    // Premium modules should be able to depend on community modules
                    communityModules.forEach { communityModule ->
                        buildContent shouldContain("implementation(project(\":$communityModule\"))") {
                            "Premium module '$moduleName' should be able to depend on community module '$communityModule'"
                        }
                    }
                }
            }
        }
        
        it("should prevent transitive dependencies on premium modules in community modules") {
            val communityBuildFile = Paths.get("community-core-engine/build.gradle.kts")
            
            if (Files.exists(communityBuildFile)) {
                val buildContent = Files.readString(communityBuildFile)
                
                // Check for any reference to premium modules in dependencies block
                buildContent shouldNotContain("premium-core-engine") {
                    "Community module should not reference premium-core-engine"
                }
                buildContent shouldNotContain("premium-mcp-server") {
                    "Community module should not reference premium-mcp-server"
                }
                buildContent shouldNotContain("premium-intellij-plugin") {
                    "Community module should not reference premium-intellij-plugin"
                }
            }
        }
        
        it("should enforce proper dependency configuration syntax") {
            val communityBuildFile = Paths.get("community-core-engine/build.gradle.kts")
            
            if (Files.exists(communityBuildFile)) {
                val buildContent = Files.readString(communityBuildFile)
                
                // Should use proper project dependency syntax
                buildContent shouldContain("implementation(project(\":community-core-engine\"))") {
                    "Should use proper project dependency syntax"
                }
                
                // Should not use relative paths or invalid syntax
                buildContent shouldNotContain("implementation '../premium-core-engine'") {
                    "Should not use relative paths for dependencies"
                }
                buildContent shouldNotContain("compile files('../premium-core-engine')") {
                    "Should not use compile files with relative paths"
                }
            }
        }
    }
    
    describe("Module Boundary Detection") {
        
        it("should detect module boundary violations in build files") {
            val buildFiles = listOf(
                "community-core-engine/build.gradle.kts",
                "community-mcp-server/build.gradle.kts", 
                "community-intellij-plugin/build.gradle.kts",
                "premium-core-engine/build.gradle.kts",
                "premium-mcp-server/build.gradle.kts",
                "premium-intellij-plugin/build.gradle.kts"
            )
            
            buildFiles.forEach { buildFilePath ->
                val buildFile = Paths.get(buildFilePath)
                if (Files.exists(buildFile)) {
                    val buildContent = Files.readString(buildFile)
                    val moduleName = buildFilePath.split("/").last().replace("-build.gradle.kts", "")
                    
                    when {
                        moduleName.startsWith("community") -> {
                            // Community modules should not depend on premium
                            buildContent shouldNotContain("premium-core-engine")
                            buildContent shouldNotContain("premium-mcp-server")
                            buildContent shouldNotContain("premium-intellij-plugin")
                        }
                        
                        moduleName.startsWith("premium") -> {
                            // Premium modules can depend on community
                            buildContent shouldContain("community-core-engine") {
                                "Premium module should depend on community-core-engine"
                            }
                        }
                    }
                }
            }
        }
        
        it("should validate module dependency directionality") {
            // Premium -> Community dependencies are allowed
            // Community -> Premium dependencies are forbidden
            // Premium -> Premium dependencies are allowed (but should be minimal)
            
            val premiumBuildFile = Paths.get("premium-intellij-plugin/build.gradle.kts")
            if (Files.exists(premiumBuildFile)) {
                val buildContent = Files.readString(premiumBuildFile)
                
                // Premium module should depend on community-core-engine
                buildContent shouldContain("implementation(project(\":community-core-engine\"))") {
                    "Premium module should depend on community-core-engine"
                }
                
                // Premium module should not depend on other premium modules (circular dependency)
                buildContent shouldNotContain("implementation(project(\":premium-mcp-server\"))") {
                    "Premium module should not depend on other premium modules"
                }
            }
        }
    }
    
    describe("Gradle Configuration Validation") {
        
        it("should have proper module structure in root build.gradle.kts") {
            val rootBuildFile = Paths.get("build.gradle.kts")
            Files.exists(rootBuildFile) shouldBe true
            
            val buildContent = Files.readString(rootBuildFile)
            
            // Should include all modules
            buildContent shouldContain("community-core-engine")
            buildContent shouldContain("community-mcp-server")
            buildContent shouldContain("community-intellij-plugin")
            buildContent shouldContain("premium-core-engine")
            buildContent shouldContain("premium-mcp-server")
            buildContent shouldContain("premium-intellij-plugin")
        }
        
        it("should have module boundary validation task") {
            val rootBuildFile = Paths.get("build.gradle.kts")
            Files.exists(rootBuildFile) shouldBe true
            
            val buildContent = Files.readString(rootBuildFile)
            
            // Should have validateModuleBoundaries task
            buildContent shouldContain("tasks.register(\"validateModuleBoundaries\")") {
                "Should have module boundary validation task"
            }
            
            // Should define proper module sets
            buildContent shouldContain("proprietaryModules = setOf(\"premium-core-engine\", \"premium-mcp-server\", \"premium-intellij-plugin\")") {
                "Should define proprietary modules set"
            }
            buildContent shouldContain("communityModules = setOf(\"community-core-engine\", \"community-mcp-server\", \"community-intellij-plugin\")") {
                "Should define community modules set"
            }
        }
        
        it("should prevent accidental premium module inclusion in community publications") {
            val communityBuildFile = Paths.get("community-mcp-server/build.gradle.kts")
            
            if (Files.exists(communityBuildFile)) {
                val buildContent = Files.readString(communityBuildFile)
                
                // Community modules should not publish premium content
                buildContent shouldNotContain("from premium-core-engine") {
                    "Community module should not publish premium module content"
                }
                buildContent shouldNotContain("from premium-intellij-plugin") {
                    "Community module should not publish premium plugin content"
                }
                
                // Should only include community dependencies
                buildContent shouldContain("from community-core-engine") {
                    "Community module should include community dependencies"
                }
            }
        }
    }
    
    describe("License Header Compliance") {
        
        it("should enforce Apache 2.0 headers in community modules") {
            val communityModules = listOf(
                "community-core-engine/src/main/java",
                "community-mcp-server/src/main/java",
                "community-intellij-plugin/src/main/java"
            )
            
            communityModules.forEach { modulePath ->
                val moduleDir = Paths.get(modulePath)
                if (Files.exists(moduleDir)) {
                    moduleDir.toFile().walkTopDown()
                        .filter { it.isFile && it.extension == "java" }
                        .forEach { file ->
                            val content = Files.readString(file.toPath())
                            
                            // Should have Apache 2.0 header
                            content shouldContain("Licensed under the Apache License") {
                                "Community module file should have Apache 2.0 header: ${file.fileName}"
                            }
                            
                            // Should not have proprietary header
                            content shouldNotContain("This software is proprietary") {
                                "Community module file should not have proprietary header: ${file.fileName}"
                            }
                        }
                }
            }
        }
        
        it("should enforce proprietary headers in premium modules") {
            val premiumModules = listOf(
                "premium-core-engine/src/main/java",
                "premium-mcp-server/src/main/java",
                "premium-intellij-plugin/src/main/java"
            )
            
            premiumModules.forEach { modulePath ->
                val moduleDir = Paths.get(modulePath)
                if (Files.exists(moduleDir)) {
                    moduleDir.toFile().walkTopDown()
                        .filter { it.isFile && it.extension == "java" }
                        .forEach { file ->
                            val content = Files.readString(file.toPath())
                            
                            // Should have proprietary header
                            content shouldContain("This software is proprietary") {
                                "Premium module file should have proprietary header: ${file.fileName}"
                            }
                            
                            // Should not have Apache header (unless it's a shared utility)
                            val isSharedUtility = file.toString().contains("util/") || file.toString().contains("common/")
                            if (!isSharedUtility) {
                                content shouldNotContain("Licensed under the Apache License") {
                                    "Premium module file should not have Apache header: ${file.fileName}"
                                }
                            }
                        }
                }
            }
        }
    }
})
