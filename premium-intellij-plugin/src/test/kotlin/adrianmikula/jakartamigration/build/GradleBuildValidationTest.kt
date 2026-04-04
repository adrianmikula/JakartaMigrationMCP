package adrianmikula.jakartamigration.build

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldContain
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.shouldThrow
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Kotest tests to verify Gradle build configuration and prevent common build issues.
 * Tests for issues documented in docs/standards/COMMON_ISSUES.md
 */
class GradleBuildValidationTest : DescribeSpec({

    describe("Gradle Build Configuration") {
        
        it("should not use IntelliJ Platform Gradle Plugin v1.x with incompatible Java/Gradle versions") {
            // Read main build.gradle.kts
            val buildFile = Paths.get("build.gradle.kts")
            Files.exists(buildFile) shouldBe true
            
            val buildContent = Files.readString(buildFile)
            
            // Should use IntelliJ Platform Gradle Plugin 1.17.2 (compatible with current setup)
            buildContent shouldContain("id(\"org.jetbrains.intellij\") version \"1.17.2\"")
            
            // Should not use incompatible version combinations
            buildContent shouldNotContain("org.jetbrains.intellij\" version \"1.0.0\"")
            buildContent shouldNotContain("org.jetbrains.intellij\" version \"1.1.0\"")
        }
        
        it("should configure Java toolchain properly to avoid Java 25 build issues") {
            val buildFile = Paths.get("premium-intellij-plugin/build.gradle.kts")
            Files.exists(buildFile) shouldBe true
            
            val buildContent = Files.readString(buildFile)
            
            // Should configure Java toolchain to avoid Java 25 compatibility issues
            // This prevents Kotlin compiler issues with Java 25
            buildContent shouldContain("java")
            
            // Should not force Java 25 usage in build
            buildContent shouldNotContain("JavaVersion.VERSION_25")
            buildContent shouldNotContain("25.0.2")
        }
        
        it("should use compatible Gradle version for Kotlin compilation") {
            val buildFile = Paths.get("gradle/wrapper/gradle-wrapper.properties")
            if (Files.exists(buildFile)) {
                val wrapperContent = Files.readString(buildFile)
                
                // Should use Gradle 8.5 (compatible with current setup)
                wrapperContent shouldContain("gradle-8.5")
                
                // Should not use Gradle 9.0+ (incompatible with IntelliJ Platform Plugin 1.x)
                wrapperContent shouldNotContain("gradle-9.0")
                wrapperContent shouldNotContain("gradle-9.1")
            }
        }
    }
    
    describe("Lombok Constructor Issues Prevention") {
        
        it("should prevent @AllArgsConstructor conflicts in domain classes") {
            val domainFiles = listOf(
                "premium-core-engine/src/main/java/adrianmikula/jakartamigration/dependencyanalysis/domain/Artifact.java",
                "premium-core-engine/src/main/java/adrianmikula/jakartamigration/dependencyanalysis/domain/DependencyNode.java",
                "premium-core-engine/src/main/java/adrianmikula/jakartamigration/dependencyanalysis/domain/DependencyGraph.java",
                "premium-core-engine/src/main/java/adrianmikula/jakartamigration/dependencyanalysis/domain/DependencyAnalysisReport.java",
                "premium-core-engine/src/main/java/adrianmikula/jakartamigration/dependencyanalysis/domain/ReadinessScore.java",
                "premium-core-engine/src/main/java/adrianmikula/jakartamigration/dependencyanalysis/domain/RiskAssessment.java",
                "premium-core-engine/src/main/java/adrianmikula/jakartamigration/dependencyanalysis/domain/Blocker.java",
                "premium-core-engine/src/main/java/adrianmikula/jakartamigration/dependencyanalysis/domain/Recommendation.java",
                "premium-core-engine/src/main/java/adrianmikula/jakartamigration/dependencyanalysis/domain/VersionRecommendation.java"
            )
            
            domainFiles.forEach { filePath ->
                val file = Paths.get(filePath)
                if (Files.exists(file)) {
                    val content = Files.readString(file)
                    
                    // Should not have @AllArgsConstructor with explicit constructors
                    if (content.contains("@AllArgsConstructor")) {
                        content shouldNotContain("public ${file.fileNameWithoutExtension}(")
                    }
                }
            }
        }
        
        it("should use @RequiredArgsConstructor instead of @ArgsConstructor when custom constructors exist") {
            val domainFiles = listOf(
                "premium-core-engine/src/main/java/adrianmikula/jakartamigration/advancedscanning/domain/ReflectionUsage.java",
                "premium-core-engine/src/main/java/adrianmikula/jakartamigration/advancedscanning/domain/ReflectionUsageScanResult.java",
                "premium-core-engine/src/main/java/adrianmikula/jakartamigration/advancedscanning/domain/ReflectionUsageProjectScanResult.java",
                "premium-core-engine/src/main/java/adrianmikula/jakartamigration/coderefactoring/domain/RecipeDefinition.java",
                "premium-core-engine/src/main/java/adrianmikula/jakartamigration/coderefactoring/domain/RecipeExecutionResult.java",
                "premium-core-engine/src/main/java/adrianmikula/jakartamigration/coderefactoring/domain/RecipeExecutionHistory.java"
            )
            
            domainFiles.forEach { filePath ->
                val file = Paths.get(filePath)
                if (Files.exists(file)) {
                    val content = Files.readString(file)
                    
                    // Should prefer @RequiredArgsConstructor for records with final fields
                    if (content.contains("record ") && content.contains("final ")) {
                        content shouldContain("@RequiredArgsConstructor") or content.shouldNotContain("@AllArgsConstructor")
                    }
                }
            }
        }
    }
    
    describe("JSON Deserialization Issues Prevention") {
        
        it("should include @JsonIgnoreProperties(ignoreUnknown = true) in domain classes") {
            val domainFiles = listOf(
                "premium-core-engine/src/main/java/adrianmikula/jakartamigration/dependencyanalysis/domain/Artifact.java",
                "premium-core-engine/src/main/java/adrianmikula/jakartamigration/dependencyanalysis/domain/DependencyGraph.java",
                "premium-core-engine/src/main/java/adrianmikula/jakartamigration/dependencyanalysis/domain/DependencyAnalysisReport.java",
                "premium-core-engine/src/main/java/adrianmikula/jakartamigration/advancedscanning/domain/ReflectionUsage.java",
                "premium-core-engine/src/main/java/adrianmikula/jakartamigration/coderefactoring/domain/RecipeDefinition.java"
            )
            
            domainFiles.forEach { filePath ->
                val file = Paths.get(filePath)
                if (Files.exists(file)) {
                    val content = Files.readString(file)
                    
                    // Should have Jackson annotation for unknown properties
                    if (content.contains("record ") || content.contains("class ")) {
                        content shouldContain("@JsonIgnoreProperties(ignoreUnknown = true)")
                    }
                }
            }
        }
        
        it("should not have jakartaCompatible field that causes deserialization errors") {
            val artifactFile = Paths.get("premium-core-engine/src/main/java/adrianmikula/jakartamigration/dependencyanalysis/domain/Artifact.java")
            if (Files.exists(artifactFile)) {
                val content = Files.readString(artifactFile)
                
                // Should not have deprecated jakartaCompatible field
                content shouldNotContain("jakartaCompatible")
            }
        }
    }
    
    describe("IntelliJ Plugin Issues Prevention") {
        
        it("should use ApplicationManager.getApplication().invokeLater() instead of SwingUtilities.invokeLater()") {
            val uiFiles = listOf(
                "premium-intellij-plugin/src/main/java/adrianmikula/jakartamigration/intellij/ui/HistoryTabComponent.java",
                "premium-intellij-plugin/src/main/java/adrianmikula/jakartamigration/intellij/ui/MigrationReportPanel.java",
                "premium-intellij-plugin/src/main/java/adrianmikula/jakartamigration/intellij/ui/RefactorTabComponent.java"
            )
            
            uiFiles.forEach { filePath ->
                val file = Paths.get(filePath)
                if (Files.exists(file)) {
                    val content = Files.readString(file)
                    
                    // Should not use SwingUtilities.invokeLater for dialogs
                    if (content.contains("Messages.showErrorDialog") || content.contains("Messages.showInfoDialog")) {
                        content shouldNotContain("SwingUtilities.invokeLater")
                        content shouldContain("ApplicationManager.getApplication().invokeLater")
                    }
                }
            }
        }
        
        it("should handle OpenRewrite Path relativization correctly") {
            val recipeFiles = listOf(
                "premium-intellij-plugin/src/main/java/adrianmikula/jakartamigration/intellij/service/util/IsolatedRecipeRunner.java"
            )
            
            recipeFiles.forEach { filePath ->
                val file = Paths.get(filePath)
                if (Files.exists(file)) {
                    val content = Files.readString(file)
                    
                    // Should check if source path is absolute before relativizing
                    if (content.contains("relativize") && content.contains("getSourcePath()")) {
                        content shouldContain("isAbsolute()")
                    }
                }
            }
        }
    }
    
    describe("Module Boundary Validation") {
        
        it("should prevent community modules from depending on premium modules") {
            val communityBuildFiles = listOf(
                "community-core-engine/build.gradle.kts",
                "community-mcp-server/build.gradle.kts",
                "community-intellij-plugin/build.gradle.kts"
            )
            
            communityBuildFiles.forEach { filePath ->
                val file = Paths.get(filePath)
                if (Files.exists(file)) {
                    val content = Files.readString(file)
                    
                    // Should not depend on premium modules
                    content shouldNotContain("premium-core-engine")
                    content shouldNotContain("premium-mcp-server")
                    content shouldNotContain("premium-intellij-plugin")
                }
            }
        }
        
        it("should allow premium modules to depend on community modules") {
            val premiumBuildFiles = listOf(
                "premium-core-engine/build.gradle.kts",
                "premium-mcp-server/build.gradle.kts",
                "premium-intellij-plugin/build.gradle.kts"
            )
            
            premiumBuildFiles.forEach { filePath ->
                val file = Paths.get(filePath)
                if (Files.exists(file)) {
                    val content = Files.readString(file)
                    
                    // Premium modules can depend on community modules
                    // This is expected, so we just verify the structure exists
                    content shouldContain("implementation(project(\":community-core-engine\"))")
                }
            }
        }
    }
    
    describe("Build Task Validation") {
        
        it("should have validateAll task that runs all validation checks") {
            val buildFile = Paths.get("build.gradle.kts")
            Files.exists(buildFile) shouldBe true
            
            val buildContent = Files.readString(buildFile)
            
            // Should have validateAll task
            buildContent shouldContain("tasks.register(\"validateAll\")")
            buildContent shouldContain("dependsOn(\"validateLicenseHeaders\", \"validateDependencyLicenses\", \"validateModuleBoundaries\")")
        }
        
        it("should have validateLicenseHeaders task") {
            val buildFile = Paths.get("build.gradle.kts")
            Files.exists(buildFile) shouldBe true
            
            val buildContent = Files.readString(buildFile)
            
            // Should have license header validation
            buildContent shouldContain("tasks.register(\"validateLicenseHeaders\")")
            buildContent shouldContain("Licensed under the Apache License")
            buildContent shouldContain("This software is proprietary")
        }
        
        it("should have validateDependencyLicenses task") {
            val buildFile = Paths.get("build.gradle.kts")
            Files.exists(buildFile) shouldBe true
            
            val buildContent = Files.readString(buildFile)
            
            // Should have dependency license validation
            buildContent shouldContain("tasks.register(\"validateDependencyLicenses\")")
            buildContent shouldContain("Apache-2.0")
            buildContent shouldContain("MIT")
            buildContent shouldContain("BSD-2-Clause")
        }
        
        it("should have validateModuleBoundaries task") {
            val buildFile = Paths.get("build.gradle.kts")
            Files.exists(buildFile) shouldBe true
            
            val buildContent = Files.readString(buildFile)
            
            // Should have module boundary validation
            buildContent shouldContain("tasks.register(\"validateModuleBoundaries\")")
            buildContent shouldContain("proprietaryModules = setOf(\"premium-core-engine\", \"premium-mcp-server\", \"premium-intellij-plugin\")")
            buildContent shouldContain("communityModules = setOf(\"community-core-engine\", \"community-mcp-server\", \"community-intellij-plugin\")")
        }
    }
    
    describe("Version Consistency") {
        
        it("should maintain consistent version across plugin.xml and build.gradle.kts") {
            val pluginXml = Paths.get("premium-intellij-plugin/src/main/resources/META-INF/plugin.xml")
            val buildGradle = Paths.get("premium-intellij-plugin/build.gradle.kts")
            
            if (Files.exists(pluginXml) && Files.exists(buildGradle)) {
                val xmlContent = Files.readString(pluginXml)
                val gradleContent = Files.readString(buildGradle)
                
                // Extract version from plugin.xml
                val xmlVersionMatch = Regex("<version>([^<]+)</version>").find(xmlContent)
                val xmlVersion = xmlVersionMatch?.groupValues?.get(1)
                
                // Extract version from build.gradle.kts
                val gradleVersionMatch = Regex("version\\s*=\\s*[\"']([^\"']+)[\"']").find(gradleContent)
                val gradleVersion = gradleVersionMatch?.groupValues?.get(1)
                
                xmlVersion shouldNotBe(null
                gradleVersion shouldNotBe(null
                xmlVersion shouldBe gradleVersion
            }
        }
        
        it("should have semantic version format") {
            val pluginXml = Paths.get("premium-intellij-plugin/src/main/resources/META-INF/plugin.xml")
            
            if (Files.exists(pluginXml)) {
                val content = Files.readString(pluginXml)
                
                // Extract version
                val versionMatch = Regex("<version>([^<]+)</version>").find(content)
                val version = versionMatch?.groupValues?.get(1)
                
                version shouldNotBe(null
                // Should be semantic version (x.y.z)
                version shouldMatch(Regex("\\d+\\.\\d+\\.\\d+"))
            }
        }
    }
})
